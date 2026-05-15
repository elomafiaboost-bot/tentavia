#include "aimbot.hpp"
#include "../sdk/gl_capture.hpp"
#include "../menu/menu.hpp"
#include <windows.h>
#include <cmath>

// Configurações do aimbot (mesmas do projeto de referência)
static constexpr float AIM_FOV_PX  = 300.0f; // raio de detecção em pixels
static constexpr float AIM_SPEED   = 0.6f;   // suavização (0.1 lento → 2.0 rápido)
static constexpr float AIM_HEIGHT  = 0.5f;   // 0.0=pés, 1.0=cabeça, 0.5=centro

namespace Aimbot {

static bool IsEnabled(const char* name) {
    for (auto& tab : Menu::tabs)
        for (auto& ft : tab.features)
            if (ft.name == name && ft.enabled) return true;
    return false;
}

// Projeta ponto 3D (em espaço de modelo) para coordenadas de tela 2D.
// Idêntico ao MatrixProject do esp.cpp — usa matrizes capturadas pelo hook GL.
static bool Project(const float* mv, const float* pr,
                    float mx, float my, float mz,
                    int sw, int sh, float& sx, float& sy)
{
    float vx = mv[0]*mx + mv[4]*my + mv[ 8]*mz + mv[12];
    float vy = mv[1]*mx + mv[5]*my + mv[ 9]*mz + mv[13];
    float vz = mv[2]*mx + mv[6]*my + mv[10]*mz + mv[14];
    float vw = mv[3]*mx + mv[7]*my + mv[11]*mz + mv[15];

    float cx = pr[0]*vx + pr[4]*vy + pr[ 8]*vz + pr[12]*vw;
    float cy = pr[1]*vx + pr[5]*vy + pr[ 9]*vz + pr[13]*vw;
    float cw = pr[3]*vx + pr[7]*vy + pr[11]*vz + pr[15]*vw;

    if (cw <= 0.00001f) return false;

    sx = ((cx / cw) + 1.0f) * 0.5f * (float)sw;
    sy = (1.0f - (cy / cw + 1.0f) * 0.5f) * (float)sh;
    return true;
}

void Update(int sw, int sh) {
    if (!IsEnabled("Aimbot")) return;
    if (GLCapture::players.empty()) return;

    // Só atua quando a janela do jogo está em foco
    HWND fg = GetForegroundWindow();
    if (!fg) return;

    // Só atua quando o cursor está oculto (jogador está in-game, não no inventário)
    CURSORINFO ci = {};
    ci.cbSize = sizeof(ci);
    GetCursorInfo(&ci);
    if (ci.flags & CURSOR_SHOWING) return;

    float centerX = (float)sw * 0.5f;
    float centerY = (float)sh * 0.5f;

    // Interpola entre pés (-1.0 = base do hitbox) e cabeça (+1.0 = topo)
    float targetY = -1.0f + AIM_HEIGHT * 2.0f;

    float bestDist = AIM_FOV_PX * AIM_FOV_PX;
    float bestSX = 0, bestSY = 0;
    bool  found  = false;

    // Itera jogadores capturados — índice 0 é o jogador local, pula
    for (size_t i = 1; i < GLCapture::players.size(); i++) {
        auto& ent = GLCapture::players[i];
        float sx, sy;
        if (!Project(ent.mv, ent.pr, 0.f, targetY, 0.f, sw, sh, sx, sy)) continue;

        float dx = sx - centerX;
        float dy = sy - centerY;
        float dist = dx*dx + dy*dy;

        if (dist < bestDist) {
            bestDist = dist;
            bestSX = sx;
            bestSY = sy;
            found  = true;
        }
    }

    if (!found) return;

    int moveX = (int)lroundf((bestSX - centerX) * AIM_SPEED);
    int moveY = (int)lroundf((bestSY - centerY) * AIM_SPEED);

    // Cap de movimento por frame para evitar saltos bruscos
    if (moveX >  50) moveX =  50;
    if (moveX < -50) moveX = -50;
    if (moveY >  50) moveY =  50;
    if (moveY < -50) moveY = -50;

    if (moveX == 0 && moveY == 0) return;

    INPUT inp = {};
    inp.type       = INPUT_MOUSE;
    inp.mi.dwFlags = MOUSEEVENTF_MOVE;
    inp.mi.dx      = (LONG)moveX;
    inp.mi.dy      = (LONG)moveY;
    SendInput(1, &inp, sizeof(INPUT));
}

} // namespace Aimbot
