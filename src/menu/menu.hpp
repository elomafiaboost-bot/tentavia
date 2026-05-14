#pragma once
#include <windows.h>
#include <string>
#include <vector>

namespace Menu {

// ── Tipos públicos ─────────────────────────────────────────────────────────

struct Feature {
    std::string name;
    bool        enabled = false;
};

struct Tab {
    std::string          name;
    std::vector<Feature> features;
};

// ── Interface pública ──────────────────────────────────────────────────────

extern bool              visible;
extern float             x, y;          // posição do menu (arrastável)
extern std::vector<Tab>  tabs;
extern int               activeTab;

// Inicializa as tabs e features padrão. Chamar uma vez ao injetar.
void Init();

// Chama a cada frame dentro do hook de wglSwapBuffers.
// hdc     = DC passado ao hook (usado para inicializar fonte bitmap)
// screenW / screenH = dimensões atuais do viewport
void Render(HDC hdc, int screenW, int screenH);

} // namespace Menu
