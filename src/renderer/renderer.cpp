#include "renderer.hpp"
#include <iostream>

namespace Renderer {
    twglSwapBuffers owglSwapBuffers = nullptr;

    BOOL WINAPI hkwglSwapBuffers(HDC hdc) {
        // Aqui a gente renderiza o menu
        return owglSwapBuffers(hdc);
    }

    void Init() {
        std::cout << "[+] Inicializando Renderer Hook..." << std::endl;
        
        HMODULE hOpenGL = GetModuleHandleA("opengl32.dll");
        if (hOpenGL) {
            void* pSwapBuffers = (void*)GetProcAddress(hOpenGL, "wglSwapBuffers");
            if (pSwapBuffers) {
                std::cout << "[+] wglSwapBuffers encontrado em: " << pSwapBuffers << std::endl;
            }
        }
    }
}
