# Build tentavia.jar (the Haru-based Java agent)
# Requires Maven (mvn) on PATH or uses the CurseForge bundled Java

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$agentDir = Join-Path $root "agent"
$buildDir = Join-Path $root "build"

# Find Maven
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (!$mvn) {
    # Try common locations
    $candidates = @(
        "C:\Program Files\Apache\maven\bin\mvn.cmd",
        "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd",
        "$env:USERPROFILE\.m2\wrapper\dists\*\*\apache-maven-*\bin\mvn.cmd"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { $mvn = $c; break }
    }
}

if (!$mvn) {
    Write-Error "Maven nao encontrado. Instale via 'winget install Apache.Maven' ou adicione ao PATH."
    exit 1
}

# Find Java 8 or 11 for building (the agent must target Java 8)
$javaHome = $env:JAVA_HOME
if (!$javaHome) {
    $candidates = @(
        "C:\Users\mknal\curseforge\minecraft\Install\java\Jre_21",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk*"
    )
    foreach ($c in $candidates) {
        $found = Resolve-Path $c -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { $javaHome = $found.Path; break }
    }
}

Write-Host "[*] Building tentavia.jar..."
Push-Location $agentDir
try {
    if ($javaHome) {
        $env:JAVA_HOME = $javaHome
    }
    & $mvn package -q -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
} finally {
    Pop-Location
}

$jarSrc = Join-Path $agentDir "target\tentavia.jar"
if (!(Test-Path $jarSrc)) {
    $jarSrc = Join-Path $buildDir "tentavia.jar"
}

if (Test-Path $jarSrc) {
    # Copy to build dir alongside the DLL
    Copy-Item $jarSrc (Join-Path $buildDir "tentavia.jar") -Force
    Write-Host "[+] tentavia.jar -> build\tentavia.jar"
} else {
    Write-Error "tentavia.jar nao encontrado apos build"
}

Write-Host "[+] Build concluido!"
