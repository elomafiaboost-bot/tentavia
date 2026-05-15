#pragma once
#include <windows.h>
#include <string>
#include <vector>

namespace Menu {

// ── Tipos públicos ─────────────────────────────────────────────────────────

struct Feature {
    std::string name;
    bool        enabled  = false;
    // Campos de slider — ignorados quando hasValue == false
    bool        hasValue = false;
    float       value    = 0.0f;
    float       vMin     = 0.0f;
    float       vMax     = 1.0f;
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

// Renderiza texto em coordenadas de tela 2D (y-down).
// Requer que a fonte já tenha sido inicializada (a partir do 2º frame).
// r/g/b em [0,1]. Centralizado horizontalmente se centerX=true.
void DrawText2D(int sw, int sh, float x, float y,
                float r, float g, float b,
                const char* txt, bool centerX = false);

// Retorna true se a fonte bitmap já foi inicializada.
bool FontReady();

// Retorna o valor float de uma feature slider (0.0 se não encontrada).
float GetValue(const char* name);

} // namespace Menu
