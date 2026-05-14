# Build script for Tentavia Injector
$vcvarsPath = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat"

if (-not (Test-Path $vcvarsPath)) {
    Write-Host "[-] vcvars64.bat no encontrado em $vcvarsPath" -ForegroundColor Red
    exit
}

Write-Host "[+] Compilando Tentavia Injector..." -ForegroundColor Cyan

# Cria o diretrio de build se no existir
if (-not (Test-Path "build")) { New-Item -ItemType Directory -Path "build" }

# Compila o injector
$compileCmd = "cl.exe injector/main.cpp /EHsc /Ox /Febuild/injector.exe /Fobuild/ user32.lib"
cmd /c "call `"$vcvarsPath`" && $compileCmd"

if ($LASTEXITCODE -eq 0) {
    Write-Host "[+] Injector compilado com sucesso! Arquivo: build/injector.exe" -ForegroundColor Green

    # Copia o tentavia.ini para build/ para que o injector.exe o encontre ao lado de si
    $iniSrc = Join-Path (Split-Path $PSScriptRoot) "tentavia.ini"
    if (Test-Path $iniSrc) {
        Copy-Item -Path $iniSrc -Destination "build/tentavia.ini" -Force
        Write-Host "[+] tentavia.ini copiado para build/" -ForegroundColor Green
    } else {
        Write-Host "[!] tentavia.ini nao encontrado — injector usara os valores padrao." -ForegroundColor Yellow
    }
} else {
    Write-Host "[-] Erro na compilacao do Injector." -ForegroundColor Red
}
