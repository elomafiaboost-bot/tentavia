#include "menu.hpp"
#include <GL/gl.h>
#include <cstring>
#include <cstdio>
#include <cmath>

#pragma comment(lib, "opengl32.lib")

namespace Menu {

// ── Estado público ──────────────────────────────────────────────────────────
bool              visible   = false;
float             x         = 40.0f;
float             y         = 40.0f;
std::vector<Tab>  tabs;
int               activeTab = 0;

// ── Paleta (igual ao launcher) ──────────────────────────────────────────────
struct Col4 { float r, g, b, a; };
static constexpr Col4 C_BG    = {0.039f, 0.047f, 0.078f, 0.94f};
static constexpr Col4 C_HDR   = {0.055f, 0.080f, 0.175f, 0.97f};
static constexpr Col4 C_TAB   = {0.028f, 0.033f, 0.062f, 0.98f};
static constexpr Col4 C_TABON = {0.080f, 0.110f, 0.230f, 0.95f};
static constexpr Col4 C_TABHV = {0.060f, 0.075f, 0.145f, 0.70f};
static constexpr Col4 C_ITMHV = {0.090f, 0.110f, 0.200f, 0.60f};
static constexpr Col4 C_FOOT  = {0.028f, 0.033f, 0.062f, 0.98f};
static constexpr Col4 C_BORD  = {0.140f, 0.175f, 0.310f, 0.90f};
static constexpr Col4 C_TEXT  = {0.863f, 0.894f, 0.988f, 1.00f};
static constexpr Col4 C_DIM   = {0.380f, 0.435f, 0.635f, 1.00f};
static constexpr Col4 C_DIMLO = {0.220f, 0.265f, 0.415f, 1.00f};
static constexpr Col4 C_BLUE  = {0.161f, 0.475f, 1.000f, 1.00f};
static constexpr Col4 C_PURP  = {0.486f, 0.302f, 1.000f, 1.00f};
static constexpr Col4 C_GREEN = {0.180f, 0.835f, 0.451f, 1.00f};
static constexpr Col4 C_SEP   = {0.100f, 0.125f, 0.225f, 0.65f};

// ── Dimensões ───────────────────────────────────────────────────────────────
static constexpr float MW    = 288.0f;
static constexpr float HDR_H =  32.0f;
static constexpr float TAB_H =  26.0f;
static constexpr float ITM_H =  30.0f;
static constexpr float FOT_H =  18.0f;
static constexpr float PAD   =  10.0f;

static float MenuH() {
    int n = (activeTab < (int)tabs.size()) ? (int)tabs[activeTab].features.size() : 0;
    return HDR_H + TAB_H + ITM_H * (n ? n : 1) + FOT_H;
}

// ── Font bitmap ─────────────────────────────────────────────────────────────
static GLuint g_fontBase  = 0;
static bool   g_fontReady = false;
static int    g_fontH     = 14;
static int    g_sw = 800, g_sh = 600; // cache atualizado a cada Render

static void EnsureFont(HDC hdc) {
    if (g_fontReady) return;
    g_fontBase = glGenLists(128);
    HFONT hf = CreateFontA(g_fontH, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, "Segoe UI");
    HFONT old = (HFONT)SelectObject(hdc, hf);
    wglUseFontBitmapsA(hdc, 0, 128, g_fontBase);
    SelectObject(hdc, old); DeleteObject(hf);
    g_fontReady = true;
}

// Renderiza texto em coords de tela (sy = topo do glyph).
// Usa projeção y-up temporária para compatibilidade com wglUseFontBitmaps.
static void DrawStr(float sx, float sy, const char* txt) {
    if (!g_fontReady || !txt || !*txt) return;
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();
    // y-up com dimensões reais da tela — imprescindível para posição correta em x
    glOrtho(0.0, (double)g_sw, 0.0, (double)g_sh, -1.0, 1.0);
    glMatrixMode(GL_MODELVIEW);
    // Baseline: topo do glyph em screen-y → GL y = (sh - sy - fontH + descent_offset)
    glRasterPos2f(sx, (float)g_sh - sy - (float)g_fontH + 2.0f);
    glListBase(g_fontBase);
    glCallLists((GLsizei)strlen(txt), GL_UNSIGNED_BYTE, txt);
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
}

// Largura aproximada do texto (7.2 px/char — Segoe UI 14px)
static float TW(const char* s) { return s ? (float)strlen(s) * 7.2f : 0.0f; }

// ── API pública de texto para outros módulos ─────────────────────────────────
bool FontReady() { return g_fontReady; }

float GetValue(const char* name) {
    for (auto& tab : tabs)
        for (auto& ft : tab.features)
            if (ft.name == name) return ft.value;
    return 0.0f;
}

void GetColor(const char* name, float& r, float& g, float& b) {
    for (auto& tab : tabs)
        for (auto& ft : tab.features)
            if (ft.name == name && ft.hasColor) { r = ft.r; g = ft.g; b = ft.b; return; }
}

void DrawText2D(int sw, int sh, float px, float py,
                float r, float g, float b,
                const char* txt, bool centerX)
{
    if (!g_fontReady || !txt || !*txt) return;
    if (centerX) px -= TW(txt) * 0.5f;
    glColor4f(r, g, b, 1.0f);
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();
    glOrtho(0.0, (double)sw, 0.0, (double)sh, -1.0, 1.0);
    glMatrixMode(GL_MODELVIEW);
    glRasterPos2f(px, (float)sh - py - (float)g_fontH + 2.0f);
    glListBase(g_fontBase);
    glCallLists((GLsizei)strlen(txt), GL_UNSIGNED_BYTE, txt);
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
}

// ── Helpers de desenho ───────────────────────────────────────────────────────
static void Set(const Col4& c, float a = -1.f) {
    glColor4f(c.r, c.g, c.b, a < 0 ? c.a : a);
}

static void FillR(float rx, float ry, float rw, float rh) {
    glBegin(GL_QUADS);
    glVertex2f(rx,      ry);      glVertex2f(rx + rw, ry);
    glVertex2f(rx + rw, ry + rh); glVertex2f(rx,      ry + rh);
    glEnd();
}

static void LineR(float rx, float ry, float rw, float rh) {
    glBegin(GL_LINE_LOOP);
    glVertex2f(rx,      ry);      glVertex2f(rx + rw, ry);
    glVertex2f(rx + rw, ry + rh); glVertex2f(rx,      ry + rh);
    glEnd();
}

static void HLine(float x1, float y0, float x2) {
    glBegin(GL_LINES); glVertex2f(x1, y0); glVertex2f(x2, y0); glEnd();
}

// Círculo preenchido — simula o dot do launcher
static void FilledDot(float cx, float cy, float r, const Col4& c, float alpha = 1.f) {
    Set(c, alpha);
    glBegin(GL_TRIANGLE_FAN);
    glVertex2f(cx, cy);
    for (int i = 0; i <= 20; i++) {
        float a = (float)i / 20.f * 6.28318f;
        glVertex2f(cx + cosf(a) * r, cy + sinf(a) * r);
    }
    glEnd();
}

// Halo suave (anéis concêntricos) — igual ao launcher
static void Glow(float cx, float cy, float r, const Col4& c) {
    for (int i = (int)r + 8; i > (int)r; i -= 2) {
        float t = (float)(i - r) / 8.f;
        Col4 dim{c.r*(1-t*0.92f), c.g*(1-t*0.92f), c.b*(1-t*0.92f), 1.f};
        FilledDot(cx, cy, (float)i, dim);
    }
    FilledDot(cx, cy, r, c);
}

// ── Input ────────────────────────────────────────────────────────────────────
static bool  g_prevIns  = false;
static bool  g_prevLBtn = false;
static bool  g_clicked  = false;
static float g_mx = 0, g_my = 0;
static bool  g_dragging = false;
static float g_dragOX, g_dragOY;

// Keybinds: mapeamento VK → nome da feature (edge-triggered, sem hold repeat)
struct KeyBind { int vk; const char* feature; bool prev; };
static KeyBind g_keybinds[] = {
    { VK_F1, "ESP",        false },
    { VK_F2, "Tracers",    false },
    { VK_F3, "Chest ESP",  false },
    { VK_F4, "Aimbot",     false },
    { VK_F5, "XRay",       false },
    { VK_F6, "NameTags",   false },
    { VK_F7, "Fullbright", false },
};

static void UpdateKeybinds() {
    for (auto& kb : g_keybinds) {
        bool down = (GetAsyncKeyState(kb.vk) & 0x8000) != 0;
        if (down && !kb.prev) {
            // Toggle a feature correspondente em qualquer tab
            for (auto& tab : tabs)
                for (auto& ft : tab.features)
                    if (ft.name == kb.feature) ft.enabled = !ft.enabled;
        }
        kb.prev = down;
    }
}

static bool Hit(float rx, float ry, float rw, float rh) {
    return g_mx >= rx && g_mx <= rx + rw && g_my >= ry && g_my <= ry + rh;
}

static void UpdateInput(HDC hdc) {
    // Keybinds globais — funcionam mesmo com o menu fechado
    UpdateKeybinds();

    // Toggle INSERT
    bool ins = (GetAsyncKeyState(VK_INSERT) & 0x8000) != 0;
    if (ins && !g_prevIns) visible = !visible;
    g_prevIns = ins;
    if (!visible) return;

    // Mouse em coords de cliente (= viewport coords com nosso glOrtho y-down)
    POINT pt; GetCursorPos(&pt);
    HWND hw = WindowFromDC(hdc);
    if (hw) ScreenToClient(hw, &pt);
    g_mx = (float)pt.x;
    g_my = (float)pt.y;

    bool lbtn  = (GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
    g_clicked  = lbtn && !g_prevLBtn;
    g_prevLBtn = lbtn;

    // Drag pelo header
    if (Hit(x, y, MW, HDR_H) && g_clicked && !g_dragging) {
        g_dragging = true; g_dragOX = g_mx - x; g_dragOY = g_my - y;
    }
    if (!lbtn) g_dragging = false;
    if (g_dragging) { x = g_mx - g_dragOX; y = g_my - g_dragOY; }

    // Clique em tab
    if (!tabs.empty()) {
        float tw = MW / (float)tabs.size();
        for (int i = 0; i < (int)tabs.size(); i++)
            if (Hit(x + i * tw, y + HDR_H, tw, TAB_H) && g_clicked)
                activeTab = i;
    }

    // Clique / drag em feature
    if (activeTab < (int)tabs.size()) {
        auto& fts = tabs[activeTab].features;
        for (int i = 0; i < (int)fts.size(); i++) {
            float iy = y + HDR_H + TAB_H + i * ITM_H;
            if (!Hit(x, iy, MW, ITM_H)) continue;

            if (fts[i].hasValue) {
                // Slider: arrastar muda o valor (funciona enquanto o botão está pressionado)
                if (lbtn) {
                    float barX = x + PAD + 18.f + TW(fts[i].name.c_str()) + 6.f;
                    float barW = x + MW - 52.f - barX;
                    if (barW > 1.f) {
                        float t = (g_mx - barX) / barW;
                        t = t < 0.f ? 0.f : t > 1.f ? 1.f : t;
                        fts[i].value = fts[i].vMin + t * (fts[i].vMax - fts[i].vMin);
                    }
                }
            } else if (fts[i].hasColor) {
                // Color bars fixas: R[142,180] G[182,220] B[222,260]
                if (lbtn) {
                    float localX = g_mx - x;
                    auto dragBar = [&](float bx, float& comp) {
                        if (localX >= bx && localX <= bx + 38.f) {
                            float t = (localX - bx) / 38.f;
                            comp = t < 0.f ? 0.f : t > 1.f ? 1.f : t;
                        }
                    };
                    dragBar(142.f, fts[i].r);
                    dragBar(182.f, fts[i].g);
                    dragBar(222.f, fts[i].b);
                }
            } else {
                if (g_clicked) fts[i].enabled = !fts[i].enabled;
            }
        }
    }
}

// ── Init ─────────────────────────────────────────────────────────────────────
void Init() {
    tabs.clear();
    tabs.push_back({"Combat",   {
        {"KillAura",false}, {"Reach",false}, {"AutoBlock",false}, {"Criticals",false},
        {"Aimbot",  false},
        {"Aim FOV",    false, true, 300.f,  50.f, 800.f},
        {"Aim Speed",  false, true,  0.6f,  0.1f,   2.0f},
        {"Aim Height", false, true,  0.5f,  0.0f,   1.0f},
    }});
    tabs.push_back({"Movement", {{"Speed",false},{"Fly",false},{"Sprint",false},{"NoFall",false},{"Bhop",false}}});
    tabs.push_back({"Visual",   {
        {"ESP",true},{"Tracers",false},{"Chest ESP",false},{"NameTags",false},{"XRay",false},{"Fullbright",false},
        // Color pickers — hasColor=true, default: jogador=vermelho, bau=dourado
        {"Player Color", false, false, 0.f, 0.f, 0.f, true, 1.0f, 0.18f, 0.18f},
        {"Chest Color",  false, false, 0.f, 0.f, 0.f, true, 1.0f, 0.70f, 0.20f},
    }});
    tabs.push_back({"Player",   {{"AntiKB",false},{"FastPlace",false},{"NoHunger",false},{"AutoEat",false}}});
}

// ── Render ────────────────────────────────────────────────────────────────────
void Render(HDC hdc, int sw, int sh) {
    g_sw = sw; g_sh = sh;
    EnsureFont(hdc);
    UpdateInput(hdc);
    if (!visible) return;

    float mh = MenuH();
    float mx = x, my = y;

    glPushAttrib(GL_ALL_ATTRIB_BITS);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_TEXTURE_2D);
    glDisable(GL_LIGHTING);
    glDisable(GL_CULL_FACE);
    glDisable(GL_ALPHA_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glLineWidth(1.0f);

    // ── Sombra ──────────────────────────────────────────────────────────
    glColor4f(0, 0, 0, 0.30f);
    FillR(mx + 5, my + 5, MW, mh);

    // ── Fundo principal ──────────────────────────────────────────────────
    Set(C_BG); FillR(mx, my, MW, mh);

    // ── Header ───────────────────────────────────────────────────────────
    Set(C_HDR); FillR(mx, my, MW, HDR_H);

    // Linha de acento azul→roxo (2px, igual ao launcher)
    Set(C_BLUE); FillR(mx,              my, MW * 0.55f, 2.0f);
    Set(C_PURP); FillR(mx + MW * 0.55f, my, MW * 0.45f, 2.0f);

    // Título "TENTAVIA"
    Set(C_TEXT); DrawStr(mx + PAD, my + 9.0f, "TENTAVIA");

    // Pontos de drag no canto direito
    Set(C_DIM);
    for (int i = 0; i < 3; i++) FillR(mx + MW - 20.f + i * 5.f, my + 15.f, 2.f, 2.f);

    // ── Tab bar ───────────────────────────────────────────────────────────
    Set(C_TAB); FillR(mx, my + HDR_H, MW, TAB_H);

    float tabW = MW / (float)tabs.size();
    for (int i = 0; i < (int)tabs.size(); i++) {
        float tx = mx + i * tabW, ty = my + HDR_H;
        bool  act = (i == activeTab);
        bool  hov = Hit(tx, ty, tabW, TAB_H);

        if (act) { Set(C_TABON); FillR(tx, ty, tabW, TAB_H); }
        else if (hov) { Set(C_TABHV); FillR(tx, ty, tabW, TAB_H); }

        // Underline azul no tab ativo
        if (act) { Set(C_BLUE); FillR(tx + 3.f, ty + TAB_H - 2.f, tabW - 6.f, 2.f); }

        // Label centrado
        const char* lbl = tabs[i].name.c_str();
        float lx = tx + (tabW - TW(lbl)) * 0.5f;
        if (act)        Set(C_TEXT);
        else if (hov)   Set(C_DIM);
        else            Set(C_DIMLO);
        DrawStr(lx, ty + 6.0f, lbl);
    }

    // ── Features ─────────────────────────────────────────────────────────
    float fy = my + HDR_H + TAB_H;

    if (activeTab < (int)tabs.size()) {
        auto& fts = tabs[activeTab].features;

        if (fts.empty()) {
            Set(C_DIM); DrawStr(mx + PAD, fy + 8.f, "Sem funcionalidades.");
        }

        for (int i = 0; i < (int)fts.size(); i++) {
            float iy  = fy + i * ITM_H;
            bool  on  = fts[i].enabled;
            bool  hov = Hit(mx, iy, MW, ITM_H);

            // Fundo hover
            if (hov) { Set(C_ITMHV); FillR(mx, iy, MW, ITM_H); }

            // Fundo do item ligado (leve tint azul)
            if (on) { glColor4f(C_BLUE.r, C_BLUE.g, C_BLUE.b, 0.07f); FillR(mx, iy, MW, ITM_H); }

            // Separador
            Set(C_SEP);
            HLine(mx + 8.f, iy + ITM_H - 0.5f, mx + MW - 8.f);

            // Dot de status
            float dotX = mx + PAD + 6.f, dotY = iy + ITM_H * 0.5f;
            const char* fname = fts[i].name.c_str();

            if (fts[i].hasColor) {
                // ── Color picker ──────────────────────────────────────────
                // Dot colorido (mostra a cor atual)
                FilledDot(dotX, dotY, 4.f, Col4{fts[i].r, fts[i].g, fts[i].b, 1.0f});

                if (hov) Set(C_DIM); else Set(C_DIMLO);
                DrawStr(mx + PAD + 18.f, iy + 9.f, fname);

                float barY = iy + (ITM_H - 5.f) * 0.5f;

                // R / G / B bars em posições fixas a partir da esquerda do menu
                struct { float bx; float val; float tr, tg, tb; } bars[3] = {
                    {mx + 142.f, fts[i].r, 0.85f, 0.10f, 0.10f},
                    {mx + 182.f, fts[i].g, 0.10f, 0.80f, 0.10f},
                    {mx + 222.f, fts[i].b, 0.10f, 0.10f, 0.85f},
                };
                for (auto& bar : bars) {
                    glColor4f(0.07f, 0.09f, 0.18f, 0.85f);
                    FillR(bar.bx, barY, 38.f, 5.f);
                    glColor4f(bar.tr, bar.tg, bar.tb, 0.80f);
                    FillR(bar.bx, barY, 38.f * bar.val, 5.f);
                    Set(C_BORD);
                    LineR(bar.bx, barY, 38.f, 5.f);
                }

                // Swatch: retângulo com a cor resultante
                float swX = mx + MW - 26.f;
                float swY = iy + (ITM_H - 12.f) * 0.5f;
                glColor4f(fts[i].r, fts[i].g, fts[i].b, 1.0f);
                FillR(swX, swY, 20.f, 12.f);
                glColor4f(fts[i].r * 0.55f, fts[i].g * 0.55f, fts[i].b * 0.55f, 1.0f);
                LineR(swX, swY, 20.f, 12.f);

            } else if (fts[i].hasValue) {
                // ── Slider ────────────────────────────────────────────────
                FilledDot(dotX, dotY, 3.f, C_DIMLO, 0.5f);

                if (hov) Set(C_DIM); else Set(C_DIMLO);
                DrawStr(mx + PAD + 18.f, iy + 9.f, fname);

                float barX = mx + PAD + 18.f + TW(fname) + 6.f;
                float barW = mx + MW - 52.f - barX;
                float barY = iy + (ITM_H - 5.f) * 0.5f;

                if (barW > 10.f) {
                    float t = (fts[i].vMax > fts[i].vMin)
                        ? (fts[i].value - fts[i].vMin) / (fts[i].vMax - fts[i].vMin)
                        : 0.f;
                    // Fundo
                    glColor4f(0.07f, 0.09f, 0.18f, 0.85f);
                    FillR(barX, barY, barW, 5.f);
                    // Fill azul
                    Set(C_BLUE, 0.75f);
                    FillR(barX, barY, barW * t, 5.f);
                    // Borda
                    Set(C_BORD);
                    LineR(barX, barY, barW, 5.f);
                }

                // Valor numérico
                char valStr[16];
                if ((fts[i].vMax - fts[i].vMin) > 10.f)
                    snprintf(valStr, sizeof(valStr), "%.0f", fts[i].value);
                else
                    snprintf(valStr, sizeof(valStr), "%.2f", fts[i].value);
                Set(C_DIM);
                DrawStr(mx + MW - TW(valStr) - 6.f, iy + 9.f, valStr);

            } else {
                // ── Toggle ────────────────────────────────────────────────
                if (on)  Glow(dotX, dotY, 5.f, C_BLUE);
                else     FilledDot(dotX, dotY, 4.f, C_DIMLO, 0.7f);

                if (on)       Set(C_TEXT);
                else if (hov) Set(C_DIM);
                else          Set(C_DIMLO);
                DrawStr(mx + PAD + 18.f, iy + 9.f, fname);

                if (on) {
                    float bw = TW("ON") + 12.f;
                    float bx = mx + MW - bw - 8.f, by = iy + (ITM_H - 14.f) * 0.5f;
                    glColor4f(C_BLUE.r, C_BLUE.g, C_BLUE.b, 0.20f); FillR(bx, by, bw, 14.f);
                    Set(C_BLUE); LineR(bx, by, bw, 14.f);
                    DrawStr(bx + 6.f, by + 1.f, "ON");
                } else {
                    float bx = mx + MW - TW("OFF") - 14.f;
                    Set(C_DIMLO); DrawStr(bx, iy + 9.f, "OFF");
                }
            }
        }
    }

    // ── Rodapé ────────────────────────────────────────────────────────────
    float footY = my + mh - FOT_H;
    Set(C_FOOT); FillR(mx, footY, MW, FOT_H);
    Set(C_DIMLO); DrawStr(mx + PAD, footY + 2.f, "INSERT  F1-F7: toggles");
    Set(C_DIMLO); DrawStr(mx + MW - TW("v0.1") - PAD, footY + 2.f, "v0.1");

    // ── Borda externa ─────────────────────────────────────────────────────
    glLineWidth(1.0f);
    Set(C_BORD); LineR(mx, my, MW, mh);

    glPopAttrib();
}

} // namespace Menu
