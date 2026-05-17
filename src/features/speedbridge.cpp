#include "speedbridge.hpp"
#include "../menu/menu.hpp"
#include "../sdk/minecraft.hpp"
#include <windows.h>
#include <chrono>
#include <iostream>

namespace SpeedBridge {

static const int SNEAK_HOLD_MS    = 50;
static const int SNEAK_RELEASE_MS = 150;

static bool g_shiftHeld = false;
static std::chrono::steady_clock::time_point g_lastToggle;
static bool s_timerInit = false;

static bool IsEnabled() {
    for (auto& tab : Menu::tabs)
        for (auto& ft : tab.features)
            if (ft.name == "Speed Bridge" && ft.enabled) return true;
    return false;
}

// Verifica se a janela do nosso processo está em foco.
// Mais confiável que GetCursorInfo para detectar "in-game" no Minecraft.
static bool OurWindowFocused() {
    HWND fg = GetForegroundWindow();
    if (!fg) return false;
    DWORD pid = 0;
    GetWindowThreadProcessId(fg, &pid);
    return pid == GetCurrentProcessId();
}

static void SetShift(bool pressed) {
    // Tenta JNI direto no keyDownBuffer do LWJGL (bypassa Raw Input)
    if (SDK::Minecraft::SetSneakKeyState(pressed)) return;

    // Fallback: SendInput com scan code
    INPUT inp = {};
    inp.type       = INPUT_KEYBOARD;
    inp.ki.wVk     = VK_LSHIFT;
    inp.ki.wScan   = 0x2A;
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

    if (!OurWindowFocused()) {
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
            std::cout << "[SB] sneak ON" << std::endl;
        }
    }
}

void Release() {
    if (g_shiftHeld) { SetShift(false); g_shiftHeld = false; }
}

} // namespace SpeedBridge
