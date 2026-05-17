#pragma once

namespace SpeedBridge {
    // Chama a cada frame no hook de SwapBuffers.
    // Auto-segura Shift enquanto Speed Bridge ativo + RMB pressionado + in-game.
    void Update();

    // Solta Shift se estiver sendo segurado (chamar ao desativar a DLL).
    void Release();
}
