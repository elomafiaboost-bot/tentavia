# Build tentavia.jar sem Maven — usa javac + jar direto
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$agentSrc = "$root\agent\src\main\java"
$buildDir = "$root\build"
$tmpDir   = "$root\.build_tmp"
$outJar   = "$buildDir\tentavia.jar"

# --- Localizar JDK ---
$jdkCandidates = @(
    "C:\Users\mknal\Downloads\jdk21\jdk-21.0.4+7",
    "C:\Users\mknal\curseforge\minecraft\Install\runtime\java-runtime-gamma\windows-x64\java-runtime-gamma",
    $env:JAVA_HOME
)
$jdkHome = $null
foreach ($c in $jdkCandidates) {
    if ($c -and (Test-Path "$c\bin\javac.exe")) { $jdkHome = $c; break }
}
if (!$jdkHome) { Write-Error "JDK nao encontrado."; exit 1 }
$javac = "$jdkHome\bin\javac.exe"
$jarTool = "$jdkHome\bin\jar.exe"
Write-Host "[*] JDK: $jdkHome"

# --- Classpath de compilacao ---
# mc_stubs.jar = stubs MC 1.8.9 gerados com SRG names — compile-time only
$mcStubsJar  = "C:\Users\mknal\Downloads\mc_stubs.jar"
$gsonJar     = "C:\Users\mknal\.m2\repository\com\google\code\gson\gson\2.2.4\gson-2.2.4.jar"
$guavaJar    = "C:\Users\mknal\.m2\repository\com\google\guava\guava\17.0\guava-17.0.jar"
$commonsIoJar = "C:\Users\mknal\.m2\repository\commons-io\commons-io\2.6\commons-io-2.6.jar"
$lwjglJar    = "C:\Users\mknal\AppData\Roaming\.minecraft\libraries\org\lwjgl\lwjgl\lwjgl\2.9.4-nightly-20150209\lwjgl-2.9.4-nightly-20150209.jar"

if (!(Test-Path $mcStubsJar)) {
    Write-Error "mc_stubs.jar nao encontrado em: $mcStubsJar"
    exit 1
}

# ASM 5.2 — baixar do Maven Central se nao existir
$asmDir = "$root\.asm_cache"
$asmJar = "$asmDir\asm-5.2.jar"
$asmComJar = "$asmDir\asm-commons-5.2.jar"
if (!(Test-Path $asmDir)) { New-Item -ItemType Directory -Path $asmDir | Out-Null }
if (!(Test-Path $asmJar)) {
    Write-Host "[*] Baixando ASM 5.2..."
    Invoke-WebRequest "https://repo1.maven.org/maven2/org/ow2/asm/asm/5.2/asm-5.2.jar" -OutFile $asmJar
}
if (!(Test-Path $asmComJar)) {
    Invoke-WebRequest "https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/5.2/asm-commons-5.2.jar" -OutFile $asmComJar
}

$cp = "$mcStubsJar;$asmJar;$asmComJar"
if (Test-Path $gsonJar)      { $cp += ";$gsonJar" }
if (Test-Path $guavaJar)     { $cp += ";$guavaJar" }
if (Test-Path $commonsIoJar) { $cp += ";$commonsIoJar" }
if (Test-Path $lwjglJar)     { $cp += ";$lwjglJar" }

# --- Compilar ---
if (Test-Path $tmpDir) { Remove-Item $tmpDir -Recurse -Force }
New-Item -ItemType Directory -Path "$tmpDir\classes" | Out-Null

$sources = Get-ChildItem $agentSrc -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
Write-Host "[*] Compilando $($sources.Count) arquivos..."

$listFile = "$tmpDir\sources.txt"
[System.IO.File]::WriteAllLines($listFile, $sources, [System.Text.Encoding]::ASCII)

& $javac -source 8 -target 8 -encoding UTF-8 -cp $cp -d "$tmpDir\classes" "@$listFile"
if ($LASTEXITCODE -ne 0) { Write-Error "Compilacao falhou"; exit 1 }
Write-Host "[+] Compilacao OK"

# --- Extrair ASM para bundle ---
Push-Location "$tmpDir\classes"
& $jarTool xf $asmJar | Out-Null
& $jarTool xf $asmComJar | Out-Null
# Remover META-INF do ASM para nao conflitar
if (Test-Path "META-INF") { Remove-Item "META-INF" -Recurse -Force }
Pop-Location

# --- Criar MANIFEST ---
$manifest = "$tmpDir\MANIFEST.MF"
@"
Manifest-Version: 1.0
Agent-Class: cc.unknown.TentaviaAgent
Premain-Class: cc.unknown.TentaviaAgent
Can-Redefine-Classes: true
Can-Retransform-Classes: true

"@ | Out-File $manifest -Encoding ascii

# --- Empacotar JAR ---
if (!(Test-Path $buildDir)) { New-Item -ItemType Directory $buildDir | Out-Null }
Push-Location "$tmpDir\classes"
& $jarTool cfm $outJar $manifest .
if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Error "JAR falhou"; exit 1 }
Pop-Location

$sizeKb = [Math]::Round((Get-Item $outJar).Length / 1KB)
Write-Host "[+] tentavia.jar -> $outJar ($sizeKb KB)"
Remove-Item $tmpDir -Recurse -Force
Write-Host "[+] Build concluido!"
