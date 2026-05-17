# Build script for Tentavia DLL (Java agent loader)
# The DLL only contains the JAR loader; all game logic is in tentavia.jar

$vcvarsPath = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat"

if (-not (Test-Path $vcvarsPath)) {
    Write-Host "[-] vcvars64.bat nao encontrado em $vcvarsPath" -ForegroundColor Red
    exit
}

Write-Host "[+] Compilando Tentavia DLL (loader)..." -ForegroundColor Cyan

if (-not (Test-Path "build")) { New-Item -ItemType Directory -Path "build" }

$clExe = "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Tools\MSVC\14.51.36223\bin\Hostx64\x64\cl.exe"

# The DLL is now a thin JAR loader - no Detours, no OpenGL, no ImGui needed
$jdkInclude = "C:\Users\mknal\Downloads\jdk21\jdk-21.0.4+7\include"

$compileCmd = "`"$clExe`" " +
    "src/main.cpp " +
    "/LD /Ox /EHsc /std:c++17 " +
    "/I src " +
    "/I `"$jdkInclude`" " +
    "/I `"$jdkInclude\win32`" " +
    "/Febuild/tentavia.dll /Fobuild/ " +
    "user32.lib kernel32.lib"

cmd /c "call `"$vcvarsPath`" >nul 2>&1 && $compileCmd"

if ($LASTEXITCODE -eq 0) {
    Write-Host "[+] tentavia.dll compilado com sucesso!" -ForegroundColor Green
    Write-Host "[!] Lembre de tambem buildar o agente Java: .\build_agent.ps1" -ForegroundColor Yellow
} else {
    Write-Host "[-] Erro na compilacao da DLL." -ForegroundColor Red
}
