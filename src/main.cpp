#include <windows.h>
#include <iostream>
#include <thread>

// Nossos mdulos
#include "utils/scanner.hpp"
#include "hooks/hook_manager.hpp"
#include "renderer/renderer.hpp"
#include "sdk/minecraft.hpp"
#include "sdk/minecraft.hpp"

DWORD WINAPI MainThread(LPVOID lpReserved) {
    AllocConsole();
    FILE* f;
    freopen_s(&f, "CONOUT$", "w", stdout);

    std::cout << "[+] Tentavia Internal Framework carregado!" << std::endl;

    // Inicializa o renderer (hooks de OpenGL/DirectX)
    Renderer::Init();

    // Exemplo de como usar o scanner pra achar algo no processo
    uintptr_t someFunc = Utils::PatternScan("opengl32.dll", "48 89 5C 24 ? ? ? ?");
    if (someFunc) {
        std::cout << "[+] Funcao encontrada em: " << std::hex << someFunc << std::endl;
        
        // Exemplo de como criar um hook (detour)
        void* original = nullptr;
        Hooks::HookManager::CreateHook((void*)someFunc, (void*)Renderer::hkwglSwapBuffers, &original);
    }

    // Comeando a dissecao
    SDK::Minecraft::PrintLocalPlayerName();

    std::cout << "[+] Pressione [END] para ejetar a DLL." << std::endl;

    while (!(GetAsyncKeyState(VK_END) & 0x8000)) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    std::cout << "[-] Cleanup finalizado. Saindo..." << std::endl;
    
    fclose(f);
    FreeConsole();
    FreeLibraryAndExitThread((HMODULE)lpReserved, 0);
    return 0;
}

BOOL WINAPI DllMain(HMODULE hModule, DWORD dwReason, LPVOID lpReserved) {
    switch (dwReason) {
    case DLL_PROCESS_ATTACH:
        DisableThreadLibraryCalls(hModule);
        HANDLE hThread = CreateThread(nullptr, 0, MainThread, hModule, 0, nullptr);
        if (hThread) CloseHandle(hThread);
        break;
    }
    return TRUE;
}
