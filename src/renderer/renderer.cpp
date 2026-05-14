#include "renderer.hpp"
#include "../hooks/hook_manager.hpp"
#include <iostream>
#include <GL/gl.h>

#pragma comment(lib, "opengl32.lib")

namespace Renderer {

    twglSwapBuffers owglSwapBuffers = nullptr;

    // Estado do menu
    static bool g_menuOpen = false;

    static void DrawPanel(float x, float y, float w, float h, float r, float g, float b, float a) {
        glBegin(GL_QUADS);
            glColor4f(r, g, b, a);
            glVertex2f(x,     y);
            glVertex2f(x + w, y);
            glVertex2f(x + w, y + h);
            glVertex2f(x,     y + h);
        glEnd();
    }

    static void DrawBorder(float x, float y, float w, float h, float r, float g, float b, float a) {
        glBegin(GL_LINE_LOOP);
            glColor4f(r, g, b, a);
            glVertex2f(x,     y);
            glVertex2f(x + w, y);
            glVertex2f(x + w, y + h);
            glVertex2f(x,     y + h);
        glEnd();
    }

    static void DrawOverlay() {
        GLint viewport[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        float sw = (float)viewport[2];
        float sh = (float)viewport[3];

        // Salva todo estado OpenGL
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, sw, sh, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (g_menuOpen) {
            float mx = 20.0f, my = 20.0f, mw = 220.0f, mh = 180.0f;

            // Fundo do menu
            DrawPanel(mx, my, mw, mh, 0.05f, 0.05f, 0.08f, 0.82f);

            // Barra de título
            DrawPanel(mx, my, mw, 24.0f, 0.13f, 0.38f, 0.78f, 0.95f);

            // Borda externa
            glLineWidth(1.5f);
            DrawBorder(mx, my, mw, mh, 0.18f, 0.52f, 1.0f, 0.9f);

            // Separador sob o título
            glBegin(GL_LINES);
                glColor4f(0.18f, 0.52f, 1.0f, 0.5f);
                glVertex2f(mx,        my + 24.0f);
                glVertex2f(mx + mw,   my + 24.0f);
            glEnd();

            // Itens do menu (barras de texto placeholder)
            float itemY = my + 34.0f;
            float itemColors[3][3] = {
                {0.2f, 0.8f, 0.4f},   // verde — ativo
                {0.8f, 0.8f, 0.8f},   // branco
                {0.8f, 0.8f, 0.8f},
            };
            for (int i = 0; i < 3; i++) {
                DrawPanel(mx + 10.0f, itemY, mw - 20.0f, 14.0f,
                    itemColors[i][0], itemColors[i][1], itemColors[i][2], 0.15f);
                DrawBorder(mx + 10.0f, itemY, mw - 20.0f, 14.0f,
                    itemColors[i][0], itemColors[i][1], itemColors[i][2], 0.4f);
                itemY += 22.0f;
            }
        } else {
            // Indicador minimizado no canto — só um dot azul pulsante via alpha estático
            DrawPanel(sw - 18.0f, 8.0f, 10.0f, 10.0f, 0.18f, 0.52f, 1.0f, 0.75f);
            DrawBorder(sw - 18.0f, 8.0f, 10.0f, 10.0f, 0.4f, 0.7f, 1.0f, 0.9f);
        }

        // Restaura estado
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();
    }

    BOOL WINAPI hkwglSwapBuffers(HDC hdc) {
        // Toggle do menu com INSERT
        if (GetAsyncKeyState(VK_INSERT) & 1)
            g_menuOpen = !g_menuOpen;

        DrawOverlay();
        return owglSwapBuffers(hdc);
    }

    void Init() {
        std::cout << "[+] Inicializando Renderer Hook..." << std::endl;

        HMODULE hOpenGL = GetModuleHandleA("opengl32.dll");
        if (!hOpenGL) {
            std::cout << "[-] opengl32.dll nao encontrado no processo." << std::endl;
            return;
        }

        void* pSwapBuffers = (void*)GetProcAddress(hOpenGL, "wglSwapBuffers");
        if (!pSwapBuffers) {
            std::cout << "[-] wglSwapBuffers nao encontrado em opengl32.dll." << std::endl;
            return;
        }

        std::cout << "[+] wglSwapBuffers encontrado em: " << pSwapBuffers << std::endl;

        bool ok = Hooks::HookManager::CreateHook(
            pSwapBuffers,
            reinterpret_cast<void*>(hkwglSwapBuffers),
            reinterpret_cast<void**>(&owglSwapBuffers)
        );

        if (ok)
            std::cout << "[+] Hook de wglSwapBuffers instalado com sucesso." << std::endl;
        else
            std::cout << "[-] Falha ao instalar hook de wglSwapBuffers." << std::endl;
    }
}
