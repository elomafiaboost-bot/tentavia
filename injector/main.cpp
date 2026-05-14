#include <windows.h>
#include <shellapi.h>
#include <shlwapi.h>
#include <iostream>
#include <tlhelp32.h>
#include <string>

#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "shlwapi.lib")

// ─── Elevação ──────────────────────────────────────────────────────────────

static bool IsElevated() {
    BOOL elevated = FALSE;
    HANDLE hToken = nullptr;
    if (OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &hToken)) {
        TOKEN_ELEVATION elevation;
        DWORD cbSize = sizeof(elevation);
        if (GetTokenInformation(hToken, TokenElevation, &elevation, sizeof(elevation), &cbSize))
            elevated = elevation.TokenIsElevated;
        CloseHandle(hToken);
    }
    return elevated != FALSE;
}

// Relança este executável com o verbo "runas" (prompt UAC) e encerra o processo atual.
// Retorna false se o usuário cancelou ou houve falha.
static bool RelaunchAsAdmin() {
    char szPath[MAX_PATH];
    if (!GetModuleFileNameA(nullptr, szPath, MAX_PATH))
        return false;

    SHELLEXECUTEINFOA sei = {};
    sei.cbSize = sizeof(sei);
    sei.lpVerb = "runas";
    sei.lpFile = szPath;
    sei.nShow  = SW_SHOWNORMAL;
    sei.fMask  = SEE_MASK_NOASYNC;

    return ShellExecuteExA(&sei) != FALSE;
}

// ─── Configuração ─────────────────────────────────────────────────────────

// Retorna o diretório do executável atual (com barra final), ex: "C:\tools\build\"
static std::string GetExeDir() {
    char buf[MAX_PATH];
    GetModuleFileNameA(nullptr, buf, MAX_PATH);
    std::string path(buf);
    auto pos = path.find_last_of("\\/");
    return (pos != std::string::npos) ? path.substr(0, pos + 1) : "";
}

struct Config {
    std::string processName;
    std::string dllPath;    // caminho absoluto resolvido
};

static Config LoadConfig() {
    std::string exeDir = GetExeDir();
    std::string iniPath = exeDir + "tentavia.ini";

    char procBuf[256]  = {};
    char dllBuf[MAX_PATH] = {};

    // Lê com defaults que mantêm comportamento original se o .ini não existir
    GetPrivateProfileStringA("Injector", "ProcessName", "javaw.exe",
        procBuf, sizeof(procBuf), iniPath.c_str());

    GetPrivateProfileStringA("Injector", "DLLPath", "tentavia.dll",
        dllBuf, sizeof(dllBuf), iniPath.c_str());

    Config cfg;
    cfg.processName = procBuf;

    // Resolve DLLPath: se for relativo, ancora no diretório do executável
    if (PathIsRelativeA(dllBuf)) {
        char resolved[MAX_PATH] = {};
        GetFullPathNameA((exeDir + dllBuf).c_str(), MAX_PATH, resolved, nullptr);
        cfg.dllPath = resolved;
    } else {
        cfg.dllPath = dllBuf;
    }

    return cfg;
}

// ─── Processo ─────────────────────────────────────────────────────────────

static DWORD GetProcessIdByName(const wchar_t* processName) {
    DWORD pid = 0;
    HANDLE hSnapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hSnapshot != INVALID_HANDLE_VALUE) {
        PROCESSENTRY32W pe;
        pe.dwSize = sizeof(pe);
        if (Process32FirstW(hSnapshot, &pe)) {
            do {
                if (wcscmp(pe.szExeFile, processName) == 0) {
                    pid = pe.th32ProcessID;
                    break;
                }
            } while (Process32NextW(hSnapshot, &pe));
        }
        CloseHandle(hSnapshot);
    }
    return pid;
}

// ─── Main ──────────────────────────────────────────────────────────────────

int main() {
    // Verifica e solicita elevação antes de qualquer operação privilegiada
    if (!IsElevated()) {
        std::cout << "[!] Permissoes de Administrador necessarias." << std::endl;
        std::cout << "[!] Solicitando elevacao via UAC..." << std::endl;

        if (!RelaunchAsAdmin()) {
            DWORD err = GetLastError();
            if (err == ERROR_CANCELLED)
                std::cerr << "[-] Elevacao cancelada pelo usuario." << std::endl;
            else
                std::cerr << "[-] Falha ao solicitar elevacao (erro " << err << ")." << std::endl;
            system("pause");
            return 1;
        }
        // A instância elevada foi iniciada; esta encerra.
        return 0;
    }

    std::cout << "[+] Rodando com privilegios de Administrador." << std::endl;

    Config cfg = LoadConfig();
    std::cout << "[+] ProcessName : " << cfg.processName << std::endl;
    std::cout << "[+] DLLPath     : " << cfg.dllPath     << std::endl;

    // Converte processName para wchar_t para usar na API Unicode
    std::wstring wProcessName(cfg.processName.begin(), cfg.processName.end());
    const char*  dllPath = cfg.dllPath.c_str();

    std::cout << "[+] Procurando por " << cfg.processName << "..." << std::endl;
    DWORD pid = GetProcessIdByName(wProcessName.c_str());
    if (!pid) {
        std::cerr << "[-] Processo nao encontrado! Abra o Minecraft primeiro." << std::endl;
        system("pause");
        return 1;
    }
    std::cout << "[+] Processo encontrado! PID: " << pid << std::endl;

    HANDLE hProcess = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pid);
    if (!hProcess) {
        std::cerr << "[-] Falha ao abrir o processo (erro " << GetLastError() << ")." << std::endl;
        system("pause");
        return 1;
    }

    void* pRemoteBuf = VirtualAllocEx(hProcess, nullptr, strlen(dllPath) + 1, MEM_COMMIT, PAGE_READWRITE);
    if (!pRemoteBuf) {
        std::cerr << "[-] Falha ao alocar memoria no processo alvo." << std::endl;
        CloseHandle(hProcess);
        system("pause");
        return 1;
    }

    if (!WriteProcessMemory(hProcess, pRemoteBuf, dllPath, strlen(dllPath) + 1, nullptr)) {
        std::cerr << "[-] Falha ao escrever na memoria do processo alvo." << std::endl;
        VirtualFreeEx(hProcess, pRemoteBuf, 0, MEM_RELEASE);
        CloseHandle(hProcess);
        system("pause");
        return 1;
    }

    void* pLoadLibrary = (void*)GetProcAddress(GetModuleHandleA("kernel32.dll"), "LoadLibraryA");
    HANDLE hThread = CreateRemoteThread(
        hProcess, nullptr, 0,
        (LPTHREAD_START_ROUTINE)pLoadLibrary,
        pRemoteBuf, 0, nullptr
    );

    if (hThread) {
        std::cout << "[+] DLL injetada com sucesso!" << std::endl;
        WaitForSingleObject(hThread, INFINITE);
        CloseHandle(hThread);
    } else {
        std::cerr << "[-] Falha ao criar thread remota (erro " << GetLastError() << ")." << std::endl;
    }

    VirtualFreeEx(hProcess, pRemoteBuf, 0, MEM_RELEASE);
    CloseHandle(hProcess);

    std::cout << "[+] Processo de injecao finalizado." << std::endl;
    system("pause");
    return 0;
}
