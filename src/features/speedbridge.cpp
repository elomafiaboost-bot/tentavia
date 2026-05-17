#include "speedbridge.hpp"
#include "../menu/menu.hpp"
#include <windows.h>

namespace SpeedBridge {

static bool g_shiftHeld = false;

static bool IsEnabled() {
    for (auto& tab : Menu::tabs)
        for (auto& ft : tab.features)
            if (ft.name == "Speed Bridge" && ft.enabled) return true;
    return false;
}

static void PressShift() {
    INPUT inp = {};
    inp.type     = INPUT_KEYBOARD;
    inp.ki.wVk   = VK_SHIFT;
    inp.ki.dwFlags = 0;
    SendInput(1, &inp, sizeof(INPUT));
}

static void ReleaseShift() {
    INPUT inp = {};
    inp.type       = INPUT_KEYBOARD;
    inp.ki.wVk     = VK_SHIFT;
    inp.ki.dwFlags = KEYEVENTF_KEYUP;
    SendInput(1, &inp, sizeof(INPUT));
}

void Update() {
    bool enabled = IsEnabled();

    if (!enabled) {
        if (g_shiftHeld) { ReleaseShift(); g_shiftHeld = false; }
        return;
    }

    // Só atua quando in-game (cursor oculto = não está em inventário/menu)
    CURSORINFO ci = {};
    ci.cbSize = sizeof(ci);
    GetCursorInfo(&ci);
    bool inGame = !(ci.flags & CURSOR_SHOWING);

    // Ativa sneak enquanto RMB estiver pressionado (colocando bloco)
    bool rmbHeld = (GetAsyncKeyState(VK_RBUTTON) & 0x8000) != 0;
    bool shouldSneak = inGame && rmbHeld;

    if (shouldSneak && !g_shiftHeld) {
        PressShift();
        g_shiftHeld = true;
    } else if (!shouldSneak && g_shiftHeld) {
        ReleaseShift();
        g_shiftHeld = false;
    }
}

void Release() {
    if (g_shiftHeld) { ReleaseShift(); g_shiftHeld = false; }
}

} // namespace SpeedBridge
