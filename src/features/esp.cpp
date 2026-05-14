#include "esp.hpp"
#include "../sdk/minecraft.hpp"
#include "../menu/menu.hpp"
#include <GL/gl.h>
#include <cmath>
#include <cstring>

#pragma comment(lib, "opengl32.lib")

namespace ESP {

// ── Projeção 3D→2D ─────────────────────────────────────────────────────────
// Reconstrói a matriz view a partir de yaw/pitch do jogador (igual ao MC 1.8).
// MC aplica: Rx(pitch) * Ry(yaw+180) * T(-eye)
// Ponto em frente da câmera tem vz < 0 (convenção OpenGL -Z forward).

static bool WorldToScreen(
    double wx, double wy, double wz,
    double camX, double camY, double camZ,
    float yaw, float pitch, float fovY, float aspect,
    int sw, int sh,
    float& sx, float& sy)
{
    float dx = (float)(wx - camX);
    float dy = (float)(wy - camY);
    float dz = (float)(wz - camZ);

    // Ry(yaw + 180)
    float yr  = (yaw + 180.0f) * 3.14159265f / 180.0f;
    float sinY = sinf(yr), cosY = cosf(yr);
    float d1x =  dx * cosY + dz * sinY;
    float d1y =  dy;
    float d1z = -dx * sinY + dz * cosY;

    // Rx(pitch)
    float pr   = pitch * 3.14159265f / 180.0f;
    float sinP = sinf(pr), cosP = cosf(pr);
    float vx   = d1x;
    float vy   = d1y * cosP - d1z * sinP;
    float vz   = d1y * sinP + d1z * cosP;

    if (-vz <= 0.001f) return false; // atrás da câmera

    float tanH = tanf(fovY * 0.5f * 3.14159265f / 180.0f);
    sx = ((vx / (-vz * tanH * aspect)) + 1.0f) * 0.5f * (float)sw;
    sy = ((vy / (-vz * tanH) * -1.0f)  + 1.0f) * 0.5f * (float)sh; // y-down
    return true;
}

// ── Helpers de desenho ────────────────────────────────────────────────────────

static void BoxOutline(float bx, float by, float bw, float bh,
                       float r, float g, float b, float a)
{
    glColor4f(r, g, b, a);
    glBegin(GL_LINE_LOOP);
    glVertex2f(bx,      by);
    glVertex2f(bx + bw, by);
    glVertex2f(bx + bw, by + bh);
    glVertex2f(bx,      by + bh);
    glEnd();
}

// Cantos estilo "L" — visual limpo sem poluir o centro
static void CornerBox(float bx, float by, float bw, float bh,
                      float r, float g, float b, float a)
{
    float cw = bw * 0.25f;
    float ch = bh * 0.25f;

    glColor4f(0.0f, 0.0f, 0.0f, a * 0.6f); // sombra preta
    glLineWidth(2.8f);
    glBegin(GL_LINES);
    // top-left
    glVertex2f(bx - 1,      by - 1);  glVertex2f(bx + cw - 1, by - 1);
    glVertex2f(bx - 1,      by - 1);  glVertex2f(bx - 1,       by + ch - 1);
    // top-right
    glVertex2f(bx + bw + 1, by - 1);  glVertex2f(bx + bw - cw + 1, by - 1);
    glVertex2f(bx + bw + 1, by - 1);  glVertex2f(bx + bw + 1, by + ch - 1);
    // bottom-left
    glVertex2f(bx - 1,      by + bh + 1); glVertex2f(bx + cw - 1, by + bh + 1);
    glVertex2f(bx - 1,      by + bh + 1); glVertex2f(bx - 1,       by + bh - ch + 1);
    // bottom-right
    glVertex2f(bx + bw + 1, by + bh + 1); glVertex2f(bx + bw - cw + 1, by + bh + 1);
    glVertex2f(bx + bw + 1, by + bh + 1); glVertex2f(bx + bw + 1, by + bh - ch + 1);
    glEnd();

    glColor4f(r, g, b, a);
    glLineWidth(1.6f);
    glBegin(GL_LINES);
    glVertex2f(bx,      by);  glVertex2f(bx + cw, by);
    glVertex2f(bx,      by);  glVertex2f(bx,      by + ch);
    glVertex2f(bx + bw, by);  glVertex2f(bx + bw - cw, by);
    glVertex2f(bx + bw, by);  glVertex2f(bx + bw, by + ch);
    glVertex2f(bx,      by + bh); glVertex2f(bx + cw, by + bh);
    glVertex2f(bx,      by + bh); glVertex2f(bx,      by + bh - ch);
    glVertex2f(bx + bw, by + bh); glVertex2f(bx + bw - cw, by + bh);
    glVertex2f(bx + bw, by + bh); glVertex2f(bx + bw, by + bh - ch);
    glEnd();
}

// Barra de vida simples (verde→vermelho) — usa posY para simular HP 20/20
static void HealthBar(float bx, float by, float bh) {
    float hp = 1.0f; // sem API de HP por agora — full verde
    float barH = bh * hp;
    float barX = bx - 4.5f;
    // fundo escuro
    glColor4f(0.0f, 0.0f, 0.0f, 0.55f);
    glBegin(GL_QUADS);
    glVertex2f(barX, by); glVertex2f(barX + 2, by);
    glVertex2f(barX + 2, by + bh); glVertex2f(barX, by + bh);
    glEnd();
    // barra colorida
    float gr = 1.0f - hp, re = hp;
    glColor4f(re * 0.9f, gr * 0.85f + 0.15f, 0.1f, 0.85f);
    glBegin(GL_QUADS);
    glVertex2f(barX, by + bh - barH); glVertex2f(barX + 2, by + bh - barH);
    glVertex2f(barX + 2, by + bh);    glVertex2f(barX, by + bh);
    glEnd();
}

// ── Lookup de feature no menu ─────────────────────────────────────────────────

static bool IsEnabled(const char* name) {
    for (auto& tab : Menu::tabs)
        for (auto& ft : tab.features)
            if (ft.name == name && ft.enabled) return true;
    return false;
}

// ── Render ────────────────────────────────────────────────────────────────────

// Desenha um pequeno quadrado colorido no canto para confirmar que ESP está rodando
static void DrawDebugDot(int sw, int sh, bool jniOk) {
    glPushAttrib(GL_ALL_ATTRIB_BITS);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    // Verde = JNI funcionando, Vermelho = JNI falhou
    if (jniOk) glColor4f(0.0f, 1.0f, 0.2f, 0.9f);
    else        glColor4f(1.0f, 0.1f, 0.1f, 0.9f);
    float bx = (float)sw - 10.f, by = (float)sh - 10.f;
    glBegin(GL_QUADS);
    glVertex2f(bx, by); glVertex2f(bx+8, by);
    glVertex2f(bx+8, by+8); glVertex2f(bx, by+8);
    glEnd();
    glPopAttrib();
}

void Render(int sw, int sh) {
    bool espOn     = IsEnabled("ESP");
    bool tracersOn = IsEnabled("Tracers");
    if (!espOn && !tracersOn) return;

    SDK::CameraInfo cam{};
    bool camOk = SDK::Minecraft::GetCameraInfo(cam) && cam.valid;
    DrawDebugDot(sw, sh, camOk); // verde=JNI OK, vermelho=JNI falhou
    if (!camOk) return;

    std::vector<SDK::EntityInfo> players;
    SDK::Minecraft::GetNearbyPlayers(players);
    if (players.empty()) return;

    float aspect = (float)sw / (float)sh;

    glPushAttrib(GL_ALL_ATTRIB_BITS);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_TEXTURE_2D);
    glDisable(GL_LIGHTING);
    glDisable(GL_CULL_FACE);
    glDisable(GL_ALPHA_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    for (auto& e : players) {
        const float hw = 0.3f;  // meia-largura do hitbox
        const float ht = 1.8f;  // altura do hitbox

        // 8 cantos do bounding box
        const double xo[2] = {e.posX - hw, e.posX + hw};
        const double zo[2] = {e.posZ - hw, e.posZ + hw};
        const double yo[2] = {e.posY,      e.posY + ht};

        float minX = 1e9f, maxX = -1e9f;
        float minY = 1e9f, maxY = -1e9f;
        bool  anyHit = false;

        for (int xi = 0; xi < 2; xi++)
        for (int yi = 0; yi < 2; yi++)
        for (int zi = 0; zi < 2; zi++) {
            float sx, sy;
            if (WorldToScreen(xo[xi], yo[yi], zo[zi],
                              cam.eyeX, cam.eyeY, cam.eyeZ,
                              cam.yaw, cam.pitch, cam.fov, aspect, sw, sh, sx, sy)) {
                anyHit = true;
                if (sx < minX) minX = sx;
                if (sx > maxX) maxX = sx;
                if (sy < minY) minY = sy;
                if (sy > maxY) maxY = sy;
            }
        }

        if (!anyHit) continue;

        float bw = maxX - minX;
        float bh = maxY - minY;

        // Clamp para não explodir fora da tela
        if (bw > (float)sw || bh > (float)sh) continue;

        if (espOn) {
            CornerBox(minX, minY, bw, bh, 1.0f, 0.25f, 0.25f, 0.92f);
            HealthBar(minX, minY, bh);
        }

        if (tracersOn) {
            // Ponto dos pés (centro inferior do box)
            float feetSX, feetSY;
            if (WorldToScreen(e.posX, e.posY, e.posZ,
                              cam.eyeX, cam.eyeY, cam.eyeZ,
                              cam.yaw, cam.pitch, cam.fov, aspect, sw, sh, feetSX, feetSY)) {
                glColor4f(1.0f, 0.25f, 0.25f, 0.50f);
                glLineWidth(1.0f);
                glBegin(GL_LINES);
                glVertex2f((float)sw * 0.5f, (float)sh);
                glVertex2f(feetSX, feetSY);
                glEnd();
            }
        }
    }

    glPopAttrib();
}

} // namespace ESP
