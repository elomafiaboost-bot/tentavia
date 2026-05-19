# ============================================================
# Tentavia Premain Launcher
# Reads the vanilla MC 1.8.9 profile JSON, builds the full
# classpath + JVM args, appends -javaagent:tentavia.jar,
# and launches javaw.exe directly (no Forge required).
#
# Usage:
#   .\launch.ps1
#   .\launch.ps1 -McVersion 1.8.9 -Username "Player" -McDir "C:\Users\you\AppData\Roaming\.minecraft"
#   .\launch.ps1 -AgentJar "C:\path\to\tentavia.jar" -Xmx 4G
#
# Notes:
#   - Runs in offline (cracked) mode when no valid auth token is supplied.
#   - Obfuscated 1.8.9 classes (bib, bjp, etc.) are handled by the
#     VanillaCoreTransformer; SRG-mapped environments (Forge profile)
#     are also supported with the same agent jar.
# ============================================================
param(
    [string]$McVersion = "1.8.9",
    [string]$Username  = "Player",
    [string]$McDir     = "$env:APPDATA\.minecraft",
    [string]$AgentJar  = (Join-Path (Split-Path $PSScriptRoot) "build\tentavia.jar"),
    [string]$Xmx       = "2G",
    [string]$Xms       = "512M"
)

$ErrorActionPreference = "Continue"

# ── Paths ──────────────────────────────────────────────────────────────────────
$profileJson = "$McDir\versions\$McVersion\$McVersion.json"
$versionJar  = "$McDir\versions\$McVersion\$McVersion.jar"
$nativesDir  = "$McDir\versions\$McVersion\natives"
$libDir      = "$McDir\libraries"
$assetsDir   = "$McDir\assets"

if (-not (Test-Path $profileJson)) {
    Write-Host "[-] MC $McVersion nao encontrado: $profileJson" -ForegroundColor Red
    Write-Host "    Abra o launcher oficial ao menos uma vez para baixar a versao." -ForegroundColor Yellow
    exit 1
}
if (-not (Test-Path $versionJar)) {
    Write-Host "[-] $versionJar nao existe. Baixe o MC $McVersion pelo launcher oficial primeiro." -ForegroundColor Red
    exit 1
}
$agentResolved = Resolve-Path $AgentJar -ErrorAction SilentlyContinue
if (-not $agentResolved) {
    Write-Host "[-] tentavia.jar nao encontrado em: $AgentJar" -ForegroundColor Red
    Write-Host "    Execute build_agent.ps1 primeiro." -ForegroundColor Yellow
    exit 1
}

# ── Parse profile JSON ─────────────────────────────────────────────────────────
$profile = Get-Content $profileJson -Raw | ConvertFrom-Json

# ── Library resolution ─────────────────────────────────────────────────────────
# Each library can have: rules (OS filter), natives (skip from classpath), downloads.artifact
function Resolve-Library {
    param($lib)

    # OS rules check (only care about windows)
    if ($lib.rules) {
        $allowed = $false
        foreach ($rule in $lib.rules) {
            if ($rule.action -eq "allow") {
                if (-not $rule.os -or $rule.os.name -eq "windows") { $allowed = $true }
            } elseif ($rule.action -eq "disallow") {
                if ($rule.os -and $rule.os.name -eq "windows") { $allowed = $false }
            }
        }
        if (-not $allowed) { return $null }
    }

    # Skip native-only libraries (they go in natives dir, not classpath)
    if ($lib.natives) { return $null }

    # downloads.artifact.path (preferred — exact path from JSON)
    if ($lib.downloads -and $lib.downloads.artifact -and $lib.downloads.artifact.path) {
        $path = Join-Path $libDir ($lib.downloads.artifact.path -replace "/", "\")
        if (Test-Path $path) { return $path }
    }

    # Fallback: derive path from maven coordinates (group:artifact:version)
    $parts = $lib.name -split ":"
    if ($parts.Count -lt 3) { return $null }
    $groupPath = $parts[0] -replace "\.", "\"
    $artifact  = $parts[1]
    $version   = $parts[2]
    $path = "$libDir\$groupPath\$artifact\$version\$artifact-$version.jar"
    if (Test-Path $path) { return $path }

    Write-Host "    [!] Biblioteca nao encontrada localmente: $($lib.name)" -ForegroundColor DarkYellow
    return $null
}

$cpParts = @($versionJar)
foreach ($lib in $profile.libraries) {
    $p = Resolve-Library $lib
    if ($p) { $cpParts += $p }
}
$classpath = $cpParts -join ";"

# ── Find Java ──────────────────────────────────────────────────────────────────
# MC 1.8.9 requires Java 8. Try Minecraft's own bundled runtime first.
$javaCandidates = @(
    "$McDir\runtime\java-runtime-alpha\windows-x64\java-runtime-alpha\bin\javaw.exe",
    "$env:APPDATA\CurseForge\minecraft\Install\runtime\java-runtime-alpha\windows-x64\java-runtime-alpha\bin\javaw.exe",
    "C:\Users\$env:USERNAME\curseforge\minecraft\Install\runtime\java-runtime-gamma\windows-x64\java-runtime-gamma\bin\javaw.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-8.0.392.8-hotspot\bin\javaw.exe",
    "C:\Program Files\Java\jre1.8.0_401\bin\javaw.exe",
    "C:\Program Files\Java\jdk1.8.0_401\bin\javaw.exe"
)
if ($env:JAVA_HOME) { $javaCandidates += "$env:JAVA_HOME\bin\javaw.exe" }
$fromPath = (Get-Command "javaw.exe" -ErrorAction SilentlyContinue)
if ($fromPath) { $javaCandidates += $fromPath.Source }

$javaExe = $null
foreach ($c in $javaCandidates) {
    if ($c -and (Test-Path $c)) { $javaExe = $c; break }
}
if (-not $javaExe) {
    Write-Host "[-] Java nao encontrado. Instale Java 8 ou defina JAVA_HOME." -ForegroundColor Red
    exit 1
}

# ── Build JVM arguments ────────────────────────────────────────────────────────
$assetIndex = if ($profile.assets) { $profile.assets } else { $McVersion }

$jvmArgs = @(
    "-Xmx$Xmx",
    "-Xms$Xms",
    "-XX:+UseG1GC",
    "-XX:+ParallelRefProcEnabled",
    "-XX:MaxGCPauseMillis=200",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:G1NewSizePercent=20",
    "-XX:G1ReservePercent=20",
    "-XX:G1HeapRegionSize=32M",
    "-Djava.library.path=$nativesDir",
    "-Dminecraft.launcher.brand=tentavia",
    "-Dminecraft.launcher.version=4.0",
    "-javaagent:`"$agentResolved`""
)

# ── Build game arguments ───────────────────────────────────────────────────────
$minecraftArgs = $profile.minecraftArguments
if (-not $minecraftArgs -and $profile.arguments -and $profile.arguments.game) {
    # 1.13+ format — not expected for 1.8.9 but handle gracefully
    $minecraftArgs = ($profile.arguments.game | Where-Object { $_ -is [string] }) -join " "
}

# Build a deterministic offline UUID from the username (last 12 hex chars padded)
$userHex  = ($Username.ToLower() -replace '[^a-f0-9]', '') + "000000000000"
$fakeUuid = "00000000-0000-0000-0000-" + $userHex.Substring(0, 12)
$gameArgs  = ($minecraftArgs -split " ") | Where-Object { $_ -ne "" }
$replacements = @{
    '${auth_player_name}'  = $Username
    '${version_name}'      = $McVersion
    '${game_directory}'    = $McDir
    '${assets_root}'       = $assetsDir
    '${assets_index_name}' = $assetIndex
    '${auth_uuid}'         = $fakeUuid
    '${auth_access_token}' = "0"
    '${user_type}'         = "legacy"
    '${version_type}'      = if ($profile.type) { $profile.type } else { "release" }
    '${user_properties}'   = "{}"
}
$resolvedGameArgs = $gameArgs | ForEach-Object {
    $arg = $_
    foreach ($key in $replacements.Keys) {
        $arg = $arg -replace [regex]::Escape($key), $replacements[$key]
    }
    $arg
}

$mainClass = $profile.mainClass

# ── Launch ─────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "  *** TENTAVIA CLIENT ***" -ForegroundColor Cyan
Write-Host ""
Write-Host "[*] MC Version : $McVersion"                          -ForegroundColor White
Write-Host "[*] Username   : $Username"                           -ForegroundColor White
Write-Host "[*] Java       : $javaExe"                            -ForegroundColor White
Write-Host "[*] Agent      : $agentResolved"                      -ForegroundColor White
Write-Host "[*] Libraries  : $($cpParts.Count) JARs no classpath" -ForegroundColor White
Write-Host ""
Write-Host "[*] Lancando Minecraft $McVersion com tentavia..." -ForegroundColor Green

$allArgs = ($jvmArgs + @("-cp", $classpath, $mainClass) + $resolvedGameArgs) | Where-Object { $_ -ne $null -and $_ -ne "" }

$proc = Start-Process -FilePath $javaExe -ArgumentList $allArgs -PassThru -WindowStyle Normal
Write-Host "[+] PID: $($proc.Id) - MC iniciado." -ForegroundColor Green
