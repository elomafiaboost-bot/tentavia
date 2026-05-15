#pragma once

namespace Aimbot {
    // Chama a cada frame dentro do hook de SwapBuffers.
    // Verifica se "Aimbot" está ativo no menu antes de agir.
    void Update(int screenW, int screenH);
}
