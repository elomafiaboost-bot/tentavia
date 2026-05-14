#include "renderer.hpp"
#include "../hooks/hook_manager.hpp"
#include "../menu/menu.hpp"
#include <iostream>
#include <GL/gl.h>

#pragma comment(lib, "opengl32.lib")

namespace Renderer {

    twglSwapBuffers owglSwapBuffers = nullptr;

    BOOL WINAPI hkwglSwapBuffers(HDC hdc) {
        // Obtém dimensões atuais do viewport
        GLint vp[4];
        glGetIntegerv(GL_VIEWPORT, vp);
        int sw = vp[2], sh = vp[3];
        if (sw <= 0 || sh <= 0) return owglSwapBuffers(hdc);

        // Configura projeção 2D y-down para os draws do menu
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, sw, sh, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Renderiza o menu (inclui input e toggle via INSERT)
        Menu::Render(hdc, sw, sh);

        // Restaura estado de matrizes
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();

        return owglSwapBuffers(hdc);
    }

    void Init() {
        std::cout << "[+] Inicializando Renderer Hook..." << std::endl;

        HMODULE hOpenGL = GetModuleHandleA("opengl32.dll");
        if (!hOpenGL) {
            std::cout << "[-] opengl32.dll nao encontrado no processo." << std::endl;
            return;
        }

        void* pSwap = (void*)GetProcAddress(hOpenGL, "wglSwapBuffers");
        if (!pSwap) {
            std::cout << "[-] wglSwapBuffers nao encontrado." << std::endl;
            return;
        }

        std::cout << "[+] wglSwapBuffers em: " << pSwap << std::endl;

        bool ok = Hooks::HookManager::CreateHook(
            pSwap,
            reinterpret_cast<void*>(hkwglSwapBuffers),
            reinterpret_cast<void**>(&owglSwapBuffers)
        );

        if (ok)
            std::cout << "[+] Hook instalado. Pressione INSERT para abrir o menu." << std::endl;
        else
            std::cout << "[-] Falha ao instalar hook de wglSwapBuffers." << std::endl;
    }
}
