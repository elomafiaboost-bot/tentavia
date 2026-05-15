#include "renderer.hpp"
#include "../menu/menu.hpp"
#include "../features/esp.hpp"
#include <iostream>
#include <thread>
#include <chrono>
#include <GL/gl.h>
#include <psapi.h>

#pragma comment(lib, "opengl32.lib")
#pragma comment(lib, "psapi.lib")

namespace Renderer {

    twglSwapBuffers owglSwapBuffers = nullptr;
    static bool     s_hooked        = false;

    BOOL WINAPI hkwglSwapBuffers(HDC hdc) {
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

        ESP::Render(sw, sh);
        Menu::Render(hdc, sw, sh);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();

        return owglSwapBuffers(hdc);
    }

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

    // Tenta encontrar qualquer módulo LWJGL carregado no processo.
    // Suporta: lwjgl64.dll, lwjgl.dll, lwjgl64-<versão>.dll, etc.
    static HMODULE FindLwjglModule() {
        // Tentativa rápida por nomes conhecidos
        const char* knownNames[] = {"lwjgl64.dll", "lwjgl.dll", nullptr};
        for (int i = 0; knownNames[i]; i++) {
            HMODULE h = GetModuleHandleA(knownNames[i]);
            if (h) return h;
        }

        // Enumera todos os módulos procurando qualquer um com "lwjgl" no nome
        HMODULE mods[1024];
        DWORD   needed = 0;
        HANDLE  hProc  = GetCurrentProcess();
        if (!EnumProcessModules(hProc, mods, sizeof(mods), &needed)) return nullptr;

        DWORD count = needed / sizeof(HMODULE);
        for (DWORD i = 0; i < count; i++) {
            char name[MAX_PATH] = {};
            GetModuleBaseNameA(hProc, mods[i], name, sizeof(name));
            if (_strnicmp(name, "lwjgl", 5) == 0) {
                std::cout << "[+] Modulo LWJGL encontrado: " << name << std::endl;
                return mods[i];
            }
        }
        return nullptr;
    }

    // Tenta instalar o hook. Retorna true se bem-sucedido.
    static bool TryInstallHook() {
        if (s_hooked) return true;

        HMODULE hLwjgl = FindLwjglModule();
        if (!hLwjgl) return false;

        // Tenta GDI32!SwapBuffers (LWJGL 2 / vanilla 1.8)
        bool ok = HookIAT(hLwjgl, "GDI32.dll", "SwapBuffers",
                          reinterpret_cast<void*>(hkwglSwapBuffers),
                          reinterpret_cast<void**>(&owglSwapBuffers));

        if (!ok) {
            // Fallback: tenta opengl32!wglSwapBuffers (algumas builds de LWJGL)
            ok = HookIAT(hLwjgl, "opengl32.dll", "wglSwapBuffers",
                         reinterpret_cast<void*>(hkwglSwapBuffers),
                         reinterpret_cast<void**>(&owglSwapBuffers));
        }

        if (ok) {
            s_hooked = true;
            std::cout << "[+] IAT hook instalado. Pressione INSERT para abrir o menu." << std::endl;
        } else {
            std::cout << "[-] Modulo LWJGL encontrado mas SwapBuffers nao esta na IAT." << std::endl;
        }
        return ok;
    }

    void Init() {
        std::cout << "[+] Inicializando Renderer Hook..." << std::endl;

        // Tenta instalar imediatamente
        if (TryInstallHook()) return;

        // LWJGL ainda nao carregou — retenta em background por ate 30s
        std::cout << "[~] LWJGL nao encontrado ainda. Aguardando carregamento..." << std::endl;
        std::thread([]() {
            for (int i = 0; i < 300; i++) {
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
                if (TryInstallHook()) return;
            }
            std::cout << "[-] Timeout: LWJGL nao foi encontrado em 30s." << std::endl;
        }).detach();
    }
}
