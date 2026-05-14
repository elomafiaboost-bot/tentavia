#include "renderer.hpp"
#include "../menu/menu.hpp"
#include <iostream>
#include <GL/gl.h>

#pragma comment(lib, "opengl32.lib")

namespace Renderer {

    twglSwapBuffers owglSwapBuffers = nullptr;

    BOOL WINAPI hkwglSwapBuffers(HDC hdc) {
        // Guard: contexto pode não estar current durante Display.create()/initContext()
        if (!wglGetCurrentContext()) return owglSwapBuffers(hdc);

        GLint vp[4];
        glGetIntegerv(GL_VIEWPORT, vp);
        int sw = vp[2], sh = vp[3];
        if (sw <= 0 || sh <= 0) return owglSwapBuffers(hdc);

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, sw, sh, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        Menu::Render(hdc, sw, sh);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();

        return owglSwapBuffers(hdc);
    }

    // Patcha a entrada de wglSwapBuffers na tabela de importações (IAT) de hMod.
    // Evita trampolines com cópia de instruções — sem risco de RIP-relative relocation.
    static bool HookIAT(HMODULE hMod, const char* dllName, const char* funcName,
                        void* detour, void** original) {
        auto* base = reinterpret_cast<uint8_t*>(hMod);
        auto* dos  = reinterpret_cast<IMAGE_DOS_HEADER*>(base);
        if (dos->e_magic != IMAGE_DOS_SIGNATURE) return false;

        auto* nt  = reinterpret_cast<IMAGE_NT_HEADERS*>(base + dos->e_lfanew);
        auto& dir = nt->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT];
        if (!dir.VirtualAddress) return false;

        auto* desc = reinterpret_cast<IMAGE_IMPORT_DESCRIPTOR*>(base + dir.VirtualAddress);
        for (; desc->Name; desc++) {
            if (_stricmp(reinterpret_cast<const char*>(base + desc->Name), dllName) != 0)
                continue;

            auto* thunk = reinterpret_cast<IMAGE_THUNK_DATA*>(base + desc->FirstThunk);
            auto* orig  = reinterpret_cast<IMAGE_THUNK_DATA*>(base + desc->OriginalFirstThunk);
            for (; thunk->u1.Function; thunk++, orig++) {
                if (IMAGE_SNAP_BY_ORDINAL(orig->u1.Ordinal)) continue;
                auto* ibn = reinterpret_cast<IMAGE_IMPORT_BY_NAME*>(
                    base + orig->u1.AddressOfData);
                if (strcmp(ibn->Name, funcName) != 0) continue;

                void** slot = reinterpret_cast<void**>(&thunk->u1.Function);
                if (original) *original = *slot;

                DWORD old;
                VirtualProtect(slot, sizeof(void*), PAGE_READWRITE, &old);
                *slot = detour;
                VirtualProtect(slot, sizeof(void*), old, &old);
                return true;
            }
        }
        return false;
    }

    void Init() {
        std::cout << "[+] Inicializando Renderer Hook (IAT)..." << std::endl;

        HMODULE hLwjgl = GetModuleHandleA("lwjgl64.dll");
        if (!hLwjgl) {
            std::cout << "[-] lwjgl64.dll nao encontrado no processo." << std::endl;
            return;
        }

        // lwjgl64.dll chama SwapBuffers de GDI32 (não wglSwapBuffers de opengl32)
        bool ok = HookIAT(hLwjgl, "GDI32.dll", "SwapBuffers",
                          reinterpret_cast<void*>(hkwglSwapBuffers),
                          reinterpret_cast<void**>(&owglSwapBuffers));

        if (ok)
            std::cout << "[+] IAT hook instalado (GDI32!SwapBuffers). Pressione INSERT para abrir o menu." << std::endl;
        else
            std::cout << "[-] Falha ao instalar IAT hook de SwapBuffers." << std::endl;
    }
}
