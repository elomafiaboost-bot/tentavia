#include <windows.h>
#include <iostream>
#include <tlhelp32.h>

// Funo para achar o PID do processo pelo nome
DWORD GetProcessIdByName(const wchar_t* processName) {
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

int main() {
    const wchar_t* targetProcess = L"javaw.exe"; // Minecraft Java geralmente roda nesse processo
    char dllPath[MAX_PATH];
    
    // Pega o caminho absoluto da nossa DLL (assume que est na mesma pasta ou na build/)
    GetFullPathNameA("build/tentavia.dll", MAX_PATH, dllPath, nullptr);

    std::cout << "[+] Procurando por " << "javaw.exe" << "..." << std::endl;
    DWORD pid = GetProcessIdByName(targetProcess);

    if (!pid) {
        std::cerr << "[-] Processo no encontrado! Abra o Minecraft primeiro." << std::endl;
        return 1;
    }

    std::cout << "[+] Processo encontrado! PID: " << pid << std::endl;

    // Abre o processo com permisses totais
    HANDLE hProcess = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pid);
    if (!hProcess) {
        std::cerr << "[-] Falha ao abrir o processo. Tente rodar como Administrador." << std::endl;
        return 1;
    }

    // Aloca memria no processo alvo para o caminho da DLL
    void* pRemoteBuf = VirtualAllocEx(hProcess, nullptr, strlen(dllPath) + 1, MEM_COMMIT, PAGE_READWRITE);
    if (!pRemoteBuf) {
        std::cerr << "[-] Falha ao alocar memria no processo alvo." << std::endl;
        CloseHandle(hProcess);
        return 1;
    }

    // Escreve o caminho da DLL na memria alocada
    if (!WriteProcessMemory(hProcess, pRemoteBuf, dllPath, strlen(dllPath) + 1, nullptr)) {
        std::cerr << "[-] Falha ao escrever na memria do processo alvo." << std::endl;
        VirtualFreeEx(hProcess, pRemoteBuf, 0, MEM_RELEASE);
        CloseHandle(hProcess);
        return 1;
    }

    // Pega o endereo da LoadLibraryA no kernel32.dll ( que  igual em todos os processos)
    void* pLoadLibrary = (void*)GetProcAddress(GetModuleHandleA("kernel32.dll"), "LoadLibraryA");

    // Cria uma thread remota que executa LoadLibraryA(pRemoteBuf)
    HANDLE hThread = CreateRemoteThread(hProcess, nullptr, 0, (LPTHREAD_START_ROUTINE)pLoadLibrary, pRemoteBuf, 0, nullptr);

    if (hThread) {
        std::cout << "[+] DLL Injetada com sucesso!" << std::endl;
        WaitForSingleObject(hThread, INFINITE);
        CloseHandle(hThread);
    } else {
        std::cerr << "[-] Falha ao criar thread remota." << std::endl;
    }

    // Cleanup
    VirtualFreeEx(hProcess, pRemoteBuf, 0, MEM_RELEASE);
    CloseHandle(hProcess);

    std::cout << "[+] Processo de injeo finalizado." << std::endl;
    return 0;
}
