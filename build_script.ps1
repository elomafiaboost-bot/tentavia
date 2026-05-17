# Build script for Tentavia Internal Framework
$vcvarsPath = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat"

if (-not (Test-Path $vcvarsPath)) {
    Write-Host "[-] vcvars64.bat nao encontrado em $vcvarsPath" -ForegroundColor Red
    exit
}

Write-Host "[+] Compilando Tentavia Internal DLL..." -ForegroundColor Cyan

if (-not (Test-Path "build")) { New-Item -ItemType Directory -Path "build" }

# Detours (Microsoft Research) - usa o .lib pre-compilado + header
$detourInc = "C:\Users\mknal\OneDrive\Desktop\Internal MC_custom\Detours\Include"
$detourLib = "C:\Users\mknal\OneDrive\Desktop\Internal MC_custom\Detours\Library\Detours64.lib"

if (-not (Test-Path $detourLib)) {
    Write-Host "[-] Detours64.lib nao encontrado em: $detourLib" -ForegroundColor Red
    Write-Host "    Ajuste o caminho \$detourLib no build_script.ps1" -ForegroundColor Yellow
    exit
}

$clExe = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Tools\MSVC\14.51.36223\bin\Hostx64\x64\cl.exe"

$compileCmd = "`"$clExe`" " +
    "src/main.cpp " +
    "src/renderer/renderer.cpp " +
    "src/menu/menu.cpp " +
    "src/sdk/minecraft.cpp " +
    "src/features/esp.cpp " +
    "src/features/aimbot.cpp " +
    "src/features/speedbridge.cpp " +
    "/LD /Ox /EHsc /std:c++17 " +
    "/I src " +
    "/I `"$detourInc`" " +
    "/Febuild/tentavia.dll /Fobuild/ " +
    "user32.lib psapi.lib opengl32.lib gdi32.lib `"$detourLib`""

cmd /c "call `"$vcvarsPath`" >nul 2>&1 && $compileCmd"

if ($LASTEXITCODE -eq 0) {
    Write-Host "[+] Build concluido! Arquivo: build/tentavia.dll" -ForegroundColor Green
} else {
    Write-Host "[-] Erro na compilacao." -ForegroundColor Red
}
