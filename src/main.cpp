#include <windows.h>
#include <iostream>
#include <thread>

#include "utils/scanner.hpp"
#include "hooks/hook_manager.hpp"
#include "renderer/renderer.hpp"
#include "menu/menu.hpp"
#include "sdk/minecraft.hpp"

DWORD WINAPI MainThread(LPVOID lpReserved) {
    AllocConsole();
    FILE* f;
    freopen_s(&f, "CONOUT$", "w", stdout);

    std::cout << "[+] Tentavia Internal Framework carregado!" << std::endl;

    // Inicializa tabs e features do menu
    Menu::Init();

    // Instala hook em wglSwapBuffers (o renderer chama Menu::Render a cada frame)
    Renderer::Init();

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
