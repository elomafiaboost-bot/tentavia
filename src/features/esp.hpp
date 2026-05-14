#pragma once

namespace ESP {
    // Chama a cada frame dentro do hook de SwapBuffers (projeção 2D já configurada).
    void Render(int screenW, int screenH);
}
