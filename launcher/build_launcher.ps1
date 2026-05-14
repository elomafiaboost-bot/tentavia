# Build script — Tentavia Launcher (GUI Win32)
$vcvarsPath = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat"

if (-not (Test-Path $vcvarsPath)) {
    Write-Host "[-] vcvars64.bat nao encontrado em $vcvarsPath" -ForegroundColor Red
    exit 1
}

Write-Host "[+] Compilando Tentavia Launcher..." -ForegroundColor Cyan

$buildDir = Join-Path (Split-Path $PSScriptRoot) "build"
if (-not (Test-Path $buildDir)) { New-Item -ItemType Directory -Path $buildDir | Out-Null }

# Compila sem console (SUBSYSTEM:WINDOWS via #pragma no fonte)
$compileCmd = "cl.exe `"$PSScriptRoot\main.cpp`" /EHsc /Ox /std:c++17 /Fe`"$buildDir\launcher.exe`" /Fo`"$buildDir\\`" user32.lib shell32.lib shlwapi.lib comdlg32.lib gdi32.lib"

cmd /c "call `"$vcvarsPath`" && $compileCmd"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[-] Erro na compilacao do Launcher." -ForegroundColor Red
    exit 1
}

# Embute o manifest UAC no executavel usando mt.exe
Write-Host "[+] Embutindo manifest UAC..." -ForegroundColor Cyan
$manifestPath = "$PSScriptRoot\launcher.manifest"
$embedCmd = "mt.exe -nologo -manifest `"$manifestPath`" -outputresource:`"$buildDir\launcher.exe`";1"
cmd /c "call `"$vcvarsPath`" && $embedCmd"

if ($LASTEXITCODE -eq 0) {
    Write-Host "[+] Launcher pronto: build\launcher.exe" -ForegroundColor Green
    Write-Host "[i] Coloque tentavia.dll na mesma pasta (build\) antes de usar." -ForegroundColor Yellow
} else {
    Write-Host "[!] Manifest nao embutido (launcher funciona, mas sem UAC automatico)." -ForegroundColor Yellow
}
