#include "speedbridge.hpp"
#include "../menu/menu.hpp"
#include "../sdk/minecraft.hpp"
#include <windows.h>
#include <chrono>

// Speed Bridge: pulsa sneak (Shift) enquanto o feature estiver ativo e in-game.
//
// Método primário: escreve diretamente no keyDownBuffer do LWJGL via JNI.
// LWJGL 2 usa Raw Input com RIDEV_NOLEGACY, o que suprime WM_KEYDOWN e faz
// SendInput ser ignorado pelo Minecraft. A escrita direta no buffer bypassa isso.
//
// Fallback: SendInput (funciona se LWJGL não usar Raw Input, ex: versões antigas).

namespace SpeedBridge {

static const int SNEAK_HOLD_MS    = 50;   // ms com Shift pressionado
static const int SNEAK_RELEASE_MS = 150;  // ms com Shift solto (ciclo total ~200ms)

static bool g_shiftHeld = false;
static std::chrono::steady_clock::time_point g_lastToggle;
static bool s_timerInit = false;

static bool IsEnabled() {
    for (auto& tab : Menu::tabs)
        for (auto& ft : tab.features)
            if (ft.name == "Speed Bridge" && ft.enabled) return true;
    return false;
}

static void SetShift(bool pressed) {
    // Tenta JNI direto no keyDownBuffer do LWJGL (método confiável)
    if (SDK::Minecraft::SetSneakKeyState(pressed)) return;

    // Fallback: SendInput com scan code explícito
    INPUT inp = {};
    inp.type       = INPUT_KEYBOARD;
    inp.ki.wVk     = VK_LSHIFT;
    inp.ki.wScan   = 0x2A; // DIK_LSHIFT
    inp.ki.dwFlags = pressed ? KEYEVENTF_SCANCODE
                             : (KEYEVENTF_SCANCODE | KEYEVENTF_KEYUP);
    SendInput(1, &inp, sizeof(INPUT));
}

void Update() {
    if (!s_timerInit) {
        g_lastToggle = std::chrono::steady_clock::now();
        s_timerInit  = true;
    }

    if (!IsEnabled()) {
        if (g_shiftHeld) { SetShift(false); g_shiftHeld = false; }
        return;
    }

    // Só pulsa quando in-game (cursor oculto = inventário/chat fechados)
    CURSORINFO ci = {};
    ci.cbSize = sizeof(ci);
    GetCursorInfo(&ci);
    bool inGame = !(ci.flags & CURSOR_SHOWING);

    if (!inGame) {
        if (g_shiftHeld) { SetShift(false); g_shiftHeld = false; }
        g_lastToggle = std::chrono::steady_clock::now();
        return;
    }

    auto now     = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - g_lastToggle).count();

    if (g_shiftHeld) {
        if (elapsed >= SNEAK_HOLD_MS) {
            SetShift(false);
            g_shiftHeld  = false;
            g_lastToggle = now;
        }
    } else {
        if (elapsed >= SNEAK_RELEASE_MS) {
            SetShift(true);
            g_shiftHeld  = true;
            g_lastToggle = now;
        }
    }
}

void Release() {
    if (g_shiftHeld) { SetShift(false); g_shiftHeld = false; }
}

} // namespace SpeedBridge
