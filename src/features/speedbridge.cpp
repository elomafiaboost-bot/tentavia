#include "speedbridge.hpp"
#include "../menu/menu.hpp"
#include <windows.h>
#include <chrono>

// Speed Bridge: pulsa Shift rapidamente enquanto o jogador anda para trás.
// Segurar Shift continuamente causaria sneak speed (lento).
// Pulsando (~50ms hold / ~150ms release) evita cair sem perder velocidade.

namespace SpeedBridge {

static const int SNEAK_HOLD_MS    = 50;   // duração do Shift pressionado
static const int SNEAK_RELEASE_MS = 150;  // pausa entre pulsos

static bool g_shiftHeld = false;
static std::chrono::steady_clock::time_point g_lastToggle;

static bool s_timerInit = false;

static bool IsEnabled() {
    for (auto& tab : Menu::tabs)
        for (auto& ft : tab.features)
            if (ft.name == "Speed Bridge" && ft.enabled) return true;
    return false;
}

static void PressShift() {
    INPUT inp = {};
    inp.type     = INPUT_KEYBOARD;
    inp.ki.wVk   = VK_LSHIFT;
    inp.ki.dwFlags = 0;
    SendInput(1, &inp, sizeof(INPUT));
}

static void ReleaseShift() {
    INPUT inp = {};
    inp.type       = INPUT_KEYBOARD;
    inp.ki.wVk     = VK_LSHIFT;
    inp.ki.dwFlags = KEYEVENTF_KEYUP;
    SendInput(1, &inp, sizeof(INPUT));
}

void Update() {
    if (!s_timerInit) {
        g_lastToggle = std::chrono::steady_clock::now();
        s_timerInit  = true;
    }

    if (!IsEnabled()) {
        if (g_shiftHeld) { ReleaseShift(); g_shiftHeld = false; }
        return;
    }

    // Só age in-game (cursor oculto = não está em inventário/chat)
    CURSORINFO ci = {};
    ci.cbSize = sizeof(ci);
    GetCursorInfo(&ci);
    bool inGame = !(ci.flags & CURSOR_SHOWING);

    // Pulsa apenas enquanto o jogador estiver andando para trás (bridging)
    bool movingBack = (GetAsyncKeyState('S') & 0x8000) != 0;

    if (!inGame || !movingBack) {
        if (g_shiftHeld) { ReleaseShift(); g_shiftHeld = false; }
        g_lastToggle = std::chrono::steady_clock::now(); // reseta timer ao parar
        return;
    }

    auto now     = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - g_lastToggle).count();

    if (g_shiftHeld) {
        // Solta após SNEAK_HOLD_MS
        if (elapsed >= SNEAK_HOLD_MS) {
            ReleaseShift();
            g_shiftHeld  = false;
            g_lastToggle = now;
        }
    } else {
        // Pressiona após SNEAK_RELEASE_MS
        if (elapsed >= SNEAK_RELEASE_MS) {
            PressShift();
            g_shiftHeld  = true;
            g_lastToggle = now;
        }
    }
}

void Release() {
    if (g_shiftHeld) { ReleaseShift(); g_shiftHeld = false; }
}

} // namespace SpeedBridge
