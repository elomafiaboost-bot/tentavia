# Build script for Tentavia Internal Framework
$vcvarsPath = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat"

if (-not (Test-Path $vcvarsPath)) {
    Write-Host "[-] vcvars64.bat no encontrado em $vcvarsPath" -ForegroundColor Red
    exit
}

Write-Host "[+] Compilando Tentavia Internal DLL..." -ForegroundColor Cyan

# Cria o diretrio de build se no existir
if (-not (Test-Path "build")) { New-Item -ItemType Directory -Path "build" }

# Executa o vcvars e o compilador no mesmo processo CMD para manter o ambiente
$compileCmd = "cl.exe src/main.cpp src/renderer/renderer.cpp src/sdk/minecraft.cpp /LD /Ox /EHsc /std:c++17 /I src /Febuild/tentavia.dll /Fobuild/ user32.lib psapi.lib"
cmd /c "call `"$vcvarsPath`" && $compileCmd"

if ($LASTEXITCODE -eq 0) {
    Write-Host "[+] Build conclu do com sucesso! Arquivo: build/tentavia.dll" -ForegroundColor Green
} else {
    Write-Host "[-] Erro na compilao." -ForegroundColor Red
}
