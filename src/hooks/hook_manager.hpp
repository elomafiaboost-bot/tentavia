#pragma once
#include <windows.h>

namespace Hooks {
    struct HookData {
        void* target;
        void* detour;
        uint8_t originalBytes[14];
        bool active;
    };

    class HookManager {
    public:
        static bool CreateHook(void* target, void* detour, void** original) {
            DWORD oldProtect;
            VirtualProtect(target, 14, PAGE_EXECUTE_READWRITE, &oldProtect);

            // Salva os bytes originais
            uint8_t* targetBytes = (uint8_t*)target;
            // (Aqui precisaria de um trampolim real para chamar o original depois, 
            // mas por simplicidade vamos apenas fazer o detour)

            // Jump absoluto x64 (14 bytes):
            // FF 25 00 00 00 00 [64-bit address]
            uint8_t jmp_code[] = { 0xFF, 0x25, 0x00, 0x00, 0x00, 0x00 };
            memcpy(target, jmp_code, 6);
            memcpy((uint8_t*)target + 6, &detour, 8);

            VirtualProtect(target, 14, oldProtect, &oldProtect);
            return true;
        }
    };
}
