#pragma once
#include <windows.h>
#include <cstring>

namespace Hooks {

    static constexpr size_t HOOK_SIZE = 14; // FF 25 00 00 00 00 + 8 bytes de endereço

    struct HookData {
        void*   target;
        void*   trampoline;
        uint8_t originalBytes[HOOK_SIZE];
        bool    active;
    };

    namespace detail {
        // Escreve um JMP absoluto x64 de 14 bytes em dst apontando para addr.
        // Bytecode: FF 25 00 00 00 00 [addr 8 bytes]
        // Equivale a: JMP QWORD PTR [RIP+0] / .quad addr
        inline void WriteAbsJump(uint8_t* dst, void* addr) {
            dst[0] = 0xFF; dst[1] = 0x25;
            dst[2] = dst[3] = dst[4] = dst[5] = 0x00;
            memcpy(dst + 6, &addr, 8);
        }
    }

    class HookManager {
    public:
        // Instala hook em target redirecionando para detour.
        // Seta *original para um trampoline executável que roda os bytes originais
        // e depois pula de volta para target+HOOK_SIZE, permitindo chamar a função
        // original sem recursão infinita.
        static bool CreateHook(void* target, void* detour, void** original) {
            if (!target || !detour) return false;

            // Aloca memória executável próxima ao processo para o trampoline:
            // [14 bytes originais] + [14 bytes de jump de volta para target+HOOK_SIZE]
            uint8_t* trampoline = static_cast<uint8_t*>(
                VirtualAlloc(nullptr, HOOK_SIZE * 2, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE)
            );
            if (!trampoline) return false;

            DWORD oldProtect;
            VirtualProtect(target, HOOK_SIZE, PAGE_EXECUTE_READWRITE, &oldProtect);

            // Copia instruções originais para o trampoline
            memcpy(trampoline, target, HOOK_SIZE);

            // Adiciona jump absoluto do trampoline de volta para target+HOOK_SIZE
            void* resumeAddr = static_cast<uint8_t*>(target) + HOOK_SIZE;
            detail::WriteAbsJump(trampoline + HOOK_SIZE, resumeAddr);

            // Escreve o jump de detour na função alvo
            detail::WriteAbsJump(static_cast<uint8_t*>(target), detour);

            VirtualProtect(target, HOOK_SIZE, oldProtect, &oldProtect);

            // Garante que o trampoline está visível no pipeline de instruções
            FlushInstructionCache(GetCurrentProcess(), trampoline, HOOK_SIZE * 2);

            if (original) *original = trampoline;
            return true;
        }

        // Remove o hook restaurando os bytes originais e libera o trampoline.
        static bool RemoveHook(void* target, void* trampoline) {
            if (!target || !trampoline) return false;

            DWORD oldProtect;
            VirtualProtect(target, HOOK_SIZE, PAGE_EXECUTE_READWRITE, &oldProtect);
            memcpy(target, trampoline, HOOK_SIZE);
            VirtualProtect(target, HOOK_SIZE, oldProtect, &oldProtect);

            FlushInstructionCache(GetCurrentProcess(), target, HOOK_SIZE);
            VirtualFree(trampoline, 0, MEM_RELEASE);
            return true;
        }
    };
}
