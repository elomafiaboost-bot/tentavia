#include "menu.hpp"
#include <GL/gl.h>
#include <cstring>

#pragma comment(lib, "opengl32.lib")

namespace Menu {

// ── Estado ─────────────────────────────────────────────────────────────────
bool              visible   = false;
float             x         = 40.0f;
float             y         = 50.0f;
std::vector<Tab>  tabs;
int               activeTab = 0;

// Fonte OpenGL bitmap (wglUseFontBitmaps — uma lista de display lists por char)
static GLuint g_fontBase   = 0;
static bool   g_fontReady  = false;
static int    g_fontH      = 14; // altura em pixels do glyph

// Input
static bool  g_prevIns  = false; // estado anterior do INSERT
static bool  g_prevLBtn = false;
static bool  g_clicked  = false;
static float g_mx = 0, g_my = 0; // posição do mouse em coords de cliente

// Drag
static bool  g_dragging = false;
static float g_dragOX, g_dragOY;

// ── Dimensões do menu ──────────────────────────────────────────────────────
static constexpr float MW    = 270.0f; // largura total
static constexpr float HDR_H = 30.0f;  // altura do cabeçalho
static constexpr float TAB_H = 25.0f;  // altura da barra de tabs
static constexpr float ITM_H = 27.0f;  // altura por feature
static constexpr float FOT_H = 17.0f;  // altura do rodapé

static float MenuH() {
    int n = (tabs.empty() || activeTab >= (int)tabs.size())
            ? 0 : (int)tabs[activeTab].features.size();
    return HDR_H + TAB_H + ITM_H * (n ? n : 1) + FOT_H;
}

// ── Helpers de input ───────────────────────────────────────────────────────
static bool InBox(float px, float py, float bx, float by, float bw, float bh) {
    return px >= bx && px <= bx + bw && py >= by && py <= by + bh;
}

static void UpdateInput(HDC hdc) {
    // Toggle INSERT (borda de subida)
    bool ins = (GetAsyncKeyState(VK_INSERT) & 0x8000) != 0;
    if (ins && !g_prevIns) visible = !visible;
    g_prevIns = ins;

    if (!visible) return;

    // Posição do mouse em coordenadas de cliente
    POINT pt; GetCursorPos(&pt);
    HWND hw = WindowFromDC(hdc);
    if (hw) ScreenToClient(hw, &pt);
    g_mx = (float)pt.x;
    g_my = (float)pt.y;

    bool lbtn = (GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
    g_clicked  = lbtn && !g_prevLBtn;
    g_prevLBtn = lbtn;

    float mh = MenuH();

    // ── Drag pelo cabeçalho ────────────────────────────────────────────
    if (InBox(g_mx, g_my, x, y, MW, HDR_H) && g_clicked) {
        g_dragging = true;
        g_dragOX   = g_mx - x;
        g_dragOY   = g_my - y;
    }
    if (!lbtn) g_dragging = false;
    if (g_dragging) { x = g_mx - g_dragOX; y = g_my - g_dragOY; }

    // ── Click em tab ───────────────────────────────────────────────────
    if (!tabs.empty()) {
        float tw = MW / (float)tabs.size();
        for (int i = 0; i < (int)tabs.size(); i++) {
            if (InBox(g_mx, g_my, x + i * tw, y + HDR_H, tw, TAB_H) && g_clicked)
                activeTab = i;
        }
    }

    // ── Click em feature ───────────────────────────────────────────────
    if (activeTab < (int)tabs.size()) {
        auto& feats = tabs[activeTab].features;
        for (int i = 0; i < (int)feats.size(); i++) {
            float iy = y + HDR_H + TAB_H + i * ITM_H;
            if (InBox(g_mx, g_my, x, iy, MW, ITM_H) && g_clicked)
                feats[i].enabled = !feats[i].enabled;
        }
    }
}

// ── Helpers de desenho ──────────────────────────────────────────────────────
static void C(float r, float g, float b, float a = 1.f) { glColor4f(r, g, b, a); }

static void FillRect(float rx, float ry, float rw, float rh) {
    glBegin(GL_QUADS);
        glVertex2f(rx,      ry);
        glVertex2f(rx + rw, ry);
        glVertex2f(rx + rw, ry + rh);
        glVertex2f(rx,      ry + rh);
    glEnd();
}

static void StrokeRect(float rx, float ry, float rw, float rh) {
    glBegin(GL_LINE_LOOP);
        glVertex2f(rx,      ry);
        glVertex2f(rx + rw, ry);
        glVertex2f(rx + rw, ry + rh);
        glVertex2f(rx,      ry + rh);
    glEnd();
}

static void HLine(float x1, float y1, float x2) {
    glBegin(GL_LINES); glVertex2f(x1, y1); glVertex2f(x2, y1); glEnd();
}

// ── Fonte bitmap via wglUseFontBitmaps ─────────────────────────────────────
static void EnsureFont(HDC hdc) {
    if (g_fontReady) return;
    g_fontBase = glGenLists(128);
    HFONT hf = CreateFontA(g_fontH, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, "Segoe UI");
    HFONT old = (HFONT)SelectObject(hdc, hf);
    wglUseFontBitmapsA(hdc, 0, 128, g_fontBase);
    SelectObject(hdc, old);
    DeleteObject(hf);
    g_fontReady = true;
}

// Renderiza texto em coordenadas de tela (y = topo do caractere).
// Usa projeção y-up temporária para que wglUseFontBitmaps funcione corretamente
// com nossa ortho y-down.
static void DrawStr(float sx, float sy, const char* txt, int screenH) {
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();
    // y-up: gl_y = sh - screen_y; raster baseline abaixo do topo do glyph
    glOrtho(0, 10000, 0, screenH, -1, 1);
    glMatrixMode(GL_MODELVIEW);

    // converte screen-y (0=topo) → gl-y (0=base), offset pelo descensor
    glRasterPos2f(sx, (float)screenH - sy - (float)g_fontH + 2.0f);
    glListBase(g_fontBase);
    glCallLists((GLsizei)strlen(txt), GL_UNSIGNED_BYTE, txt);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
}

// Largura aproximada do texto (7px/char para Segoe UI 14px)
static float TW(const char* s) { return (float)strlen(s) * 7.2f; }

// ── Init ────────────────────────────────────────────────────────────────────
void Init() {
    tabs.clear();

    Tab combat{"Combat", {
        {"KillAura",  false},
        {"Reach",     false},
        {"AutoBlock", false},
        {"Criticals", false},
        {"AimAssist", false},
    }};

    Tab movement{"Movement", {
        {"Speed",   false},
        {"Fly",     false},
        {"Sprint",  false},
        {"NoFall",  false},
        {"Bhop",    false},
    }};

    Tab visual{"Visual", {
        {"ESP",      false},
        {"Tracers",  false},
        {"NameTags", false},
        {"Chams",    false},
        {"Fullbright",false},
    }};

    Tab player{"Player", {
        {"AntiKB",     false},
        {"FastPlace",  false},
        {"NoHunger",   false},
        {"AutoEat",    false},
    }};

    tabs.push_back(combat);
    tabs.push_back(movement);
    tabs.push_back(visual);
    tabs.push_back(player);
}

// ── Render ──────────────────────────────────────────────────────────────────
void Render(HDC hdc, int sw, int sh) {
    EnsureFont(hdc);
    UpdateInput(hdc);
    if (!visible) return;

    float mh = MenuH();
    float mx = x, my = y;

    glPushAttrib(GL_ALL_ATTRIB_BITS);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_TEXTURE_2D);
    glDisable(GL_LIGHTING);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glLineWidth(1.0f);

    // ── Sombra (fundo levemente maior e mais escuro) ────────────────────
    C(0.0f, 0.0f, 0.0f, 0.35f);
    FillRect(mx + 4, my + 4, MW, mh);

    // ── Fundo principal ────────────────────────────────────────────────
    C(0.040f, 0.047f, 0.082f, 0.94f);
    FillRect(mx, my, MW, mh);

    // ── Cabeçalho ──────────────────────────────────────────────────────
    C(0.055f, 0.080f, 0.175f, 0.97f);
    FillRect(mx, my, MW, HDR_H);

    // Linha de acento superior: azul → roxo
    C(0.16f, 0.47f, 1.00f, 1.0f); FillRect(mx,            my, MW * 0.55f, 2.0f);
    C(0.49f, 0.30f, 1.00f, 1.0f); FillRect(mx + MW * 0.55f, my, MW * 0.45f, 2.0f);

    // Título
    C(0.86f, 0.90f, 0.98f, 1.0f);
    DrawStr(mx + 10.0f, my + 8.0f, "TENTAVIA", sh);

    // Ícone de drag (··· dots) no canto direito do header
    C(0.30f, 0.36f, 0.58f, 1.0f);
    for (int i = 0; i < 3; i++) {
        float dx = mx + MW - 20.0f + i * 5.0f;
        FillRect(dx, my + 13.0f, 2.0f, 2.0f);
    }

    // ── Barra de tabs ──────────────────────────────────────────────────
    C(0.028f, 0.033f, 0.062f, 0.98f);
    FillRect(mx, my + HDR_H, MW, TAB_H);

    float tabW = MW / (float)tabs.size();
    for (int i = 0; i < (int)tabs.size(); i++) {
        float tx = mx + i * tabW;
        float ty = my + HDR_H;
        bool active = (i == activeTab);
        bool hov    = InBox(g_mx, g_my, tx, ty, tabW, TAB_H);

        if (active) {
            C(0.080f, 0.110f, 0.230f, 0.95f);
            FillRect(tx, ty, tabW, TAB_H);
            // Underscore azul do tab ativo
            C(0.16f, 0.47f, 1.00f, 1.0f);
            FillRect(tx + 3.0f, ty + TAB_H - 2.0f, tabW - 6.0f, 2.0f);
        } else if (hov) {
            C(0.06f, 0.075f, 0.145f, 0.70f);
            FillRect(tx, ty, tabW, TAB_H);
        }

        // Label da tab
        const char* label = tabs[i].name.c_str();
        float lx = tx + (tabW - TW(label)) * 0.5f;
        if (active)
            C(0.86f, 0.90f, 0.98f, 1.0f);
        else if (hov)
            C(0.60f, 0.66f, 0.84f, 1.0f);
        else
            C(0.38f, 0.44f, 0.64f, 1.0f);
        DrawStr(lx, ty + 6.0f, label, sh);
    }

    // ── Features ───────────────────────────────────────────────────────
    float fy = my + HDR_H + TAB_H;

    if (activeTab < (int)tabs.size()) {
        auto& feats = tabs[activeTab].features;

        if (feats.empty()) {
            C(0.28f, 0.33f, 0.50f, 0.80f);
            DrawStr(mx + 10.0f, fy + 8.0f, "Sem funcionalidades.", sh);
        }

        for (int i = 0; i < (int)feats.size(); i++) {
            float iy  = fy + i * ITM_H;
            bool  on  = feats[i].enabled;
            bool  hov = InBox(g_mx, g_my, mx, iy, MW, ITM_H);

            // Fundo do item (hover)
            if (hov) {
                C(0.09f, 0.11f, 0.20f, 0.60f);
                FillRect(mx, iy, MW, ITM_H);
            }

            // Linha separadora
            C(0.10f, 0.12f, 0.22f, 0.65f);
            HLine(mx + 6.0f, iy + ITM_H - 0.5f, mx + MW - 6.0f);

            // Checkbox (12×12)
            float cbx = mx + 10.0f, cby = iy + (ITM_H - 12.0f) * 0.5f;
            if (on) {
                C(0.16f, 0.47f, 1.00f, 0.25f); FillRect(cbx, cby, 12.0f, 12.0f);
                C(0.16f, 0.47f, 1.00f, 1.00f); StrokeRect(cbx, cby, 12.0f, 12.0f);
                // Checkmark
                glLineWidth(1.6f);
                glBegin(GL_LINE_STRIP);
                    glVertex2f(cbx + 2.0f, cby + 6.0f);
                    glVertex2f(cbx + 5.0f, cby + 9.5f);
                    glVertex2f(cbx + 10.5f, cby + 2.5f);
                glEnd();
                glLineWidth(1.0f);
            } else {
                C(0.12f, 0.14f, 0.25f, 1.00f); FillRect(cbx, cby, 12.0f, 12.0f);
                C(0.20f, 0.24f, 0.40f, 1.00f); StrokeRect(cbx, cby, 12.0f, 12.0f);
            }

            // Nome da feature
            const char* fname = feats[i].name.c_str();
            float textY = iy + (ITM_H - (float)g_fontH) * 0.5f;
            if (on)      C(0.86f, 0.90f, 0.98f, 1.0f);
            else if (hov)C(0.60f, 0.65f, 0.82f, 1.0f);
            else         C(0.45f, 0.50f, 0.68f, 1.0f);
            DrawStr(cbx + 16.0f, textY, fname, sh);

            // Badge ON/OFF
            const char* badge = on ? "ON" : "OFF";
            float bw = TW(badge) + 10.0f;
            float bx = mx + MW - bw - 8.0f, by = iy + (ITM_H - 14.0f) * 0.5f;
            if (on) {
                C(0.16f, 0.47f, 1.00f, 0.18f); FillRect(bx, by, bw, 14.0f);
                C(0.16f, 0.47f, 1.00f, 0.50f); StrokeRect(bx, by, bw, 14.0f);
                C(0.16f, 0.47f, 1.00f, 0.90f); DrawStr(bx + 5.0f, by + 1.0f, badge, sh);
            } else {
                C(0.22f, 0.27f, 0.42f, 0.55f); DrawStr(bx + 5.0f, by + 1.0f, badge, sh);
            }
        }
    }

    // ── Rodapé ─────────────────────────────────────────────────────────
    float footY = my + mh - FOT_H;
    C(0.028f, 0.033f, 0.062f, 0.98f);
    FillRect(mx, footY, MW, FOT_H);

    C(0.22f, 0.28f, 0.45f, 0.80f);
    DrawStr(mx + 8.0f, footY + 2.0f, "INSERT para fechar", sh);

    // Versão à direita
    C(0.18f, 0.22f, 0.38f, 0.70f);
    const char* ver = "v0.1";
    DrawStr(mx + MW - TW(ver) - 8.0f, footY + 2.0f, ver, sh);

    // ── Borda externa ───────────────────────────────────────────────────
    glLineWidth(1.0f);
    C(0.14f, 0.18f, 0.32f, 0.90f);
    StrokeRect(mx, my, MW, mh);

    glPopAttrib();
}

} // namespace Menu
