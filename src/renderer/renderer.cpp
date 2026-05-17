#include "renderer.hpp"
#include "../menu/menu.hpp"
#include "../features/esp.hpp"
#include "../features/aimbot.hpp"
#include "../features/speedbridge.hpp"
#include "../sdk/gl_capture.hpp"
#include "../sdk/minecraft.hpp"
#include <iostream>
#include <thread>
#include <chrono>
#include <cmath>
#include <cstring>
#include <GL/gl.h>
#include <psapi.h>

// Detours (Microsoft Research) — compila junto via build_script.ps1
#include "Detours.h"

#pragma comment(lib, "opengl32.lib")
#pragma comment(lib, "psapi.lib")

// ── GLCapture: defini��es (populado pelos hooks, lido por ESP::Render) ────────
namespace GLCapture {
    std::vector<Entity> players;
    std::vector<Entity> chests;
    std::vector<Entity> largeChests;

    void Clear() {
        players.clear();
        chests.clear();
        largeChests.clear();
    }
}

// ── Hooks de captura de matrizes ──────────────────────────────────────────────

static void (WINAPI* o_glScalef)(GLfloat, GLfloat, GLfloat)       = nullptr;
static void (WINAPI* o_glTranslatef)(GLfloat, GLfloat, GLfloat)   = nullptr;

// Aplica offset em espa�o de modelo na coluna de transla��o da modelview,
// igual ao savePosition() do projeto de refer�ncia.
static void BakeOffset(float* mv, float ox, float oy, float oz) {
    float m3[4];
    for (int i = 0; i < 4; i++)
        m3[i] = mv[i]*ox + mv[i+4]*oy + mv[i+8]*oz + mv[i+12];
    memcpy(mv + 12, m3, sizeof(m3));
}

static void CaptureEntity(std::vector<GLCapture::Entity>& dst,
                           float ox, float oy, float oz) {
    GLCapture::Entity e;
    glGetFloatv(GL_PROJECTION_MATRIX, e.pr);
    glGetFloatv(GL_MODELVIEW_MATRIX,  e.mv);
    BakeOffset(e.mv, ox, oy, oz);
    dst.push_back(e);
}

// glScalef(0.9375, 0.9375, 0.9375) → Minecraft escala todos os modelos de jogador
void WINAPI hk_glScalef(GLfloat x, GLfloat y, GLfloat z) {
    o_glScalef(x, y, z);
    if (fabsf(x - 0.9375f) < 0.001f &&
        fabsf(y - 0.9375f) < 0.001f &&
        fabsf(z - 0.9375f) < 0.001f)
    {
        CaptureEntity(GLCapture::players, 0.0f, -1.0f, 0.0f);
    }
}

// glTranslatef detecta ba�s (chest) e ba�s duplos (large chest)
void WINAPI hk_glTranslatef(GLfloat x, GLfloat y, GLfloat z) {
    o_glTranslatef(x, y, z);
    // Ba� simples: glTranslatef(0.5, 0.4375, 0.9375)
    if (fabsf(x - 0.5f)    < 0.001f &&
        fabsf(y - 0.4375f) < 0.001f &&
        fabsf(z - 0.9375f) < 0.001f)
    {
        CaptureEntity(GLCapture::chests, 0.0f, 0.0625f, -0.4375f);
    }
    // Ba� duplo: glTranslatef(1.0, 0.4375, 0.9375)
    else if (fabsf(x - 1.0f)    < 0.001f &&
             fabsf(y - 0.4375f) < 0.001f &&
             fabsf(z - 0.9375f) < 0.001f)
    {
        CaptureEntity(GLCapture::largeChests, 0.0f, 0.0625f, -0.4375f);
    }
}

// ── SwapBuffers hook (IAT) ────────────────────────────────────────────────────

namespace Renderer {

    twglSwapBuffers owglSwapBuffers = nullptr;
    static bool     s_hooked        = false;
    static bool     s_glHooked      = false;

    static void InstallGLCaptureHooks(); // forward declaration

    BOOL WINAPI hkwglSwapBuffers(HDC hdc) {
        static bool s_glReady = false;

        // Instala hooks GL no thread de renderização (mais seguro com Detours)
        if (!s_glReady) {
            InstallGLCaptureHooks();
            s_glReady = true;
        }

        HGLRC ctx = wglGetCurrentContext();
        if (!ctx) return owglSwapBuffers(hdc);

        GLint vp[4] = {};
        glGetIntegerv(GL_VIEWPORT, vp);
        int sw = vp[2], sh = vp[3];

        if (sw <= 0 || sh <= 0) return owglSwapBuffers(hdc);

        // Salva estado completo do jogo
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, sw, sh, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Desativa estados que bloqueiam o desenho 2D
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_LIGHTING);
        glDisable(GL_FOG);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_CULL_FACE);
        glDisable(GL_COLOR_LOGIC_OP);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, 0);

        ESP::Render(sw, sh);
        Aimbot::Update(sw, sh);
        SpeedBridge::Update();

        // AntiKB: zera motionX/Z do jogador local via JNI a cada frame
        {
            bool antiKB = false;
            for (auto& tab : Menu::tabs)
                for (auto& ft : tab.features)
                    if (ft.name == "AntiKB" && ft.enabled) antiKB = true;
            if (antiKB) SDK::Minecraft::ApplyAntiKB();
        }

        Menu::Render(hdc, sw, sh);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();

        // XRay + Fullbright: aplicados após restaurar o estado para persistir no próximo frame
        {
            bool xray = false, fullbright = false;
            for (auto& tab : Menu::tabs)
                for (auto& ft : tab.features) {
                    if (ft.name == "XRay"       && ft.enabled) xray       = true;
                    if (ft.name == "Fullbright" && ft.enabled) fullbright = true;
                }

            glDepthFunc(xray ? GL_ALWAYS : GL_LEQUAL);

            if (fullbright) {
                GLfloat full[] = { 1.0f, 1.0f, 1.0f, 1.0f };
                glLightModelfv(GL_LIGHT_MODEL_AMBIENT, full);
            } else {
                GLfloat def[] = { 0.0f, 0.0f, 0.0f, 1.0f };
                glLightModelfv(GL_LIGHT_MODEL_AMBIENT, def);
            }
        }

        // Limpa dados capturados — ser�o preenchidos novamente no pr�ximo frame
        GLCapture::Clear();

        return owglSwapBuffers(hdc);
    }

    // ── IAT hook helper ───────────────────────────────────────────────────────

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

    static HMODULE FindLwjglModule() {
        const char* knownNames[] = {"lwjgl64.dll", "lwjgl.dll", nullptr};
        for (int i = 0; knownNames[i]; i++) {
            HMODULE h = GetModuleHandleA(knownNames[i]);
            if (h) return h;
        }

        HMODULE mods[1024];
        DWORD   needed = 0;
        HANDLE  hProc  = GetCurrentProcess();
        if (!EnumProcessModules(hProc, mods, sizeof(mods), &needed)) return nullptr;

        DWORD count = needed / sizeof(HMODULE);
        for (DWORD i = 0; i < count; i++) {
            char name[MAX_PATH] = {};
            GetModuleBaseNameA(hProc, mods[i], name, sizeof(name));
            if (_strnicmp(name, "lwjgl", 5) == 0) {
                std::cout << "[+] M�dulo LWJGL: " << name << std::endl;
                return mods[i];
            }
        }
        return nullptr;
    }

    static bool TryInstallHook() {
        if (s_hooked) return true;

        HMODULE hLwjgl = FindLwjglModule();
        if (!hLwjgl) return false;

        // SwapBuffers via IAT do LWJGL
        bool ok = HookIAT(hLwjgl, "GDI32.dll", "SwapBuffers",
                          reinterpret_cast<void*>(hkwglSwapBuffers),
                          reinterpret_cast<void**>(&owglSwapBuffers));

        if (!ok)
            ok = HookIAT(hLwjgl, "opengl32.dll", "wglSwapBuffers",
                         reinterpret_cast<void*>(hkwglSwapBuffers),
                         reinterpret_cast<void**>(&owglSwapBuffers));

        if (ok) {
            s_hooked = true;
            std::cout << "[+] IAT hook (SwapBuffers) instalado." << std::endl;
        } else {
            std::cout << "[-] SwapBuffers n�o encontrado na IAT do LWJGL." << std::endl;
        }
        return ok;
    }

    // ── Detours hooks para captura de entidades ───────────────────────────────

    static void InstallGLCaptureHooks() {
        if (s_glHooked) return;

        HMODULE hGL = GetModuleHandleA("opengl32.dll");
        if (!hGL) {
            std::cout << "[-] opengl32.dll n�o encontrado." << std::endl;
            return;
        }

        o_glScalef    = reinterpret_cast<void(WINAPI*)(GLfloat,GLfloat,GLfloat)>(
                            GetProcAddress(hGL, "glScalef"));
        o_glTranslatef = reinterpret_cast<void(WINAPI*)(GLfloat,GLfloat,GLfloat)>(
                            GetProcAddress(hGL, "glTranslatef"));

        if (!o_glScalef || !o_glTranslatef) {
            std::cout << "[-] glScalef/glTranslatef n�o encontrados." << std::endl;
            return;
        }

        DetourRestoreAfterWith();
        DetourTransactionBegin();
        DetourUpdateThread(GetCurrentThread());
        DetourAttach(reinterpret_cast<PVOID*>(&o_glScalef),    hk_glScalef);
        DetourAttach(reinterpret_cast<PVOID*>(&o_glTranslatef), hk_glTranslatef);
        LONG err = DetourTransactionCommit();

        if (err == NO_ERROR) {
            s_glHooked = true;
            std::cout << "[+] Detours hooks (glScalef/glTranslatef) instalados." << std::endl;
        } else {
            std::cout << "[-] Detours falhou: " << err << std::endl;
        }
    }

    void Init() {
        std::cout << "[+] Inicializando Renderer..." << std::endl;

        // Os hooks GL (glScalef/glTranslatef) são instalados no primeiro frame
        // do hkwglSwapBuffers, para rodar no thread de renderização do OpenGL.

        // Hook de SwapBuffers: aguarda LWJGL carregar
        if (TryInstallHook()) return;

        std::cout << "[~] LWJGL n�o encontrado ainda. Aguardando..." << std::endl;
        std::thread([]() {
            for (int i = 0; i < 300; i++) {
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
                if (TryInstallHook()) return;
            }
            std::cout << "[-] Timeout: LWJGL n�o foi encontrado em 30s." << std::endl;
        }).detach();
    }
}
