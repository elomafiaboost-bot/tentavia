#include "esp.hpp"
#include "../sdk/gl_capture.hpp"
#include "../sdk/minecraft.hpp"
#include "../menu/menu.hpp"
#include <GL/gl.h>
#include <cmath>
#include <cstring>

#pragma comment(lib, "opengl32.lib")

namespace ESP {

// ── Proje��o usando matrizes GL capturadas pelo hook ─────────────────────────
// Equiv. ao projectToScreen() do aimbot do projeto de refer�ncia.
// mv = modelview j� com offset de centro aplicado, pr = projection.
// Retorna false se o ponto est� atr�s da c�mera.
static bool MatrixProject(const float* mv, const float* pr,
                           float mx, float my, float mz,
                           int sw, int sh,
                           float& sx, float& sy)
{
    // Espa�o de vis�o: MV * [mx, my, mz, 1]
    float vx = mv[0]*mx + mv[4]*my + mv[ 8]*mz + mv[12];
    float vy = mv[1]*mx + mv[5]*my + mv[ 9]*mz + mv[13];
    float vz = mv[2]*mx + mv[6]*my + mv[10]*mz + mv[14];
    float vw = mv[3]*mx + mv[7]*my + mv[11]*mz + mv[15];

    // Clip space: Proj * viewPos
    float cx = pr[0]*vx + pr[4]*vy + pr[ 8]*vz + pr[12]*vw;
    float cy = pr[1]*vx + pr[5]*vy + pr[ 9]*vz + pr[13]*vw;
    float cw = pr[3]*vx + pr[7]*vy + pr[11]*vz + pr[15]*vw;

    if (cw <= 0.00001f) return false;

    // NDC → tela (Y invertido: OpenGL Y-up, tela Y-down)
    sx = ((cx / cw) + 1.0f) * 0.5f * (float)sw;
    sy = (1.0f - (cy / cw + 1.0f) * 0.5f) * (float)sh;
    return true;
}

// ── Proje��o 3D→2D via yaw/pitch (fallback JNI) ──────────────────────────────
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

    float yr  = (yaw + 180.0f) * 3.14159265f / 180.0f;
    float sinY = sinf(yr), cosY = cosf(yr);
    float d1x =  dx * cosY + dz * sinY;
    float d1y =  dy;
    float d1z = -dx * sinY + dz * cosY;

    float pr   = pitch * 3.14159265f / 180.0f;
    float sinP = sinf(pr), cosP = cosf(pr);
    float vx   = d1x;
    float vy   = d1y * cosP - d1z * sinP;
    float vz   = d1y * sinP + d1z * cosP;

    if (-vz <= 0.001f) return false;

    float tanH = tanf(fovY * 0.5f * 3.14159265f / 180.0f);
    sx = ((vx / (-vz * tanH * aspect)) + 1.0f) * 0.5f * (float)sw;
    sy = ((vy / (-vz * tanH) * -1.0f)  + 1.0f) * 0.5f * (float)sh;
    return true;
}

// ── Helpers de desenho ─────────────────────────────────────────────────────────

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

static void CornerBox(float bx, float by, float bw, float bh,
                      float r, float g, float b, float a)
{
    float cw = bw * 0.25f;
    float ch = bh * 0.25f;

    glColor4f(0.0f, 0.0f, 0.0f, a * 0.6f);
    glLineWidth(2.8f);
    glBegin(GL_LINES);
    glVertex2f(bx - 1,      by - 1);  glVertex2f(bx + cw - 1, by - 1);
    glVertex2f(bx - 1,      by - 1);  glVertex2f(bx - 1,       by + ch - 1);
    glVertex2f(bx + bw + 1, by - 1);  glVertex2f(bx + bw - cw + 1, by - 1);
    glVertex2f(bx + bw + 1, by - 1);  glVertex2f(bx + bw + 1, by + ch - 1);
    glVertex2f(bx - 1,      by + bh + 1); glVertex2f(bx + cw - 1, by + bh + 1);
    glVertex2f(bx - 1,      by + bh + 1); glVertex2f(bx - 1,       by + bh - ch + 1);
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

static void HealthBar(float bx, float by, float bh) {
    float hp  = 1.0f;
    float barH = bh * hp;
    float barX = bx - 4.5f;
    glColor4f(0.0f, 0.0f, 0.0f, 0.55f);
    glBegin(GL_QUADS);
    glVertex2f(barX, by); glVertex2f(barX + 2, by);
    glVertex2f(barX + 2, by + bh); glVertex2f(barX, by + bh);
    glEnd();
    float gr = 1.0f - hp, re = hp;
    glColor4f(re * 0.9f, gr * 0.85f + 0.15f, 0.1f, 0.85f);
    glBegin(GL_QUADS);
    glVertex2f(barX, by + bh - barH); glVertex2f(barX + 2, by + bh - barH);
    glVertex2f(barX + 2, by + bh);    glVertex2f(barX, by + bh);
    glEnd();
}

// ── Bounding box a partir de 8 cantos em espa�o de modelo ────────────────────
// Cantos do hitbox de jogador: Radius(0.8, 2.0, 0.8) / 2 (igual ao refer�ncia)
static const float PLAYER_HX = 0.4f, PLAYER_HZ = 0.4f;
static const float PLAYER_CORNERS[8][3] = {
    {-PLAYER_HX, -1.0f, -PLAYER_HZ}, { PLAYER_HX, -1.0f, -PLAYER_HZ},
    { PLAYER_HX, -1.0f,  PLAYER_HZ}, {-PLAYER_HX, -1.0f,  PLAYER_HZ},
    {-PLAYER_HX,  1.0f, -PLAYER_HZ}, { PLAYER_HX,  1.0f, -PLAYER_HZ},
    { PLAYER_HX,  1.0f,  PLAYER_HZ}, {-PLAYER_HX,  1.0f,  PLAYER_HZ},
};

// Hitbox ba�: Radius(1.0, 1.0, 1.0) / 2
static const float CHEST_H = 0.5f;
static const float CHEST_CORNERS[8][3] = {
    {-CHEST_H, -CHEST_H, -CHEST_H}, { CHEST_H, -CHEST_H, -CHEST_H},
    { CHEST_H, -CHEST_H,  CHEST_H}, {-CHEST_H, -CHEST_H,  CHEST_H},
    {-CHEST_H,  CHEST_H, -CHEST_H}, { CHEST_H,  CHEST_H, -CHEST_H},
    { CHEST_H,  CHEST_H,  CHEST_H}, {-CHEST_H,  CHEST_H,  CHEST_H},
};

// Hitbox ba� duplo: Radius(2.0, 1.0, 1.0) / 2
static const float LARGE_CORNERS[8][3] = {
    {-1.0f, -CHEST_H, -CHEST_H}, { 1.0f, -CHEST_H, -CHEST_H},
    { 1.0f, -CHEST_H,  CHEST_H}, {-1.0f, -CHEST_H,  CHEST_H},
    {-1.0f,  CHEST_H, -CHEST_H}, { 1.0f,  CHEST_H, -CHEST_H},
    { 1.0f,  CHEST_H,  CHEST_H}, {-1.0f,  CHEST_H,  CHEST_H},
};

static bool CalcBBox(const GLCapture::Entity& e,
                     const float corners[][3], int nCorners,
                     int sw, int sh,
                     float& minX, float& maxX, float& minY, float& maxY)
{
    minX = 1e9f; maxX = -1e9f;
    minY = 1e9f; maxY = -1e9f;
    bool any = false;
    for (int i = 0; i < nCorners; i++) {
        float sx, sy;
        if (MatrixProject(e.mv, e.pr, corners[i][0], corners[i][1], corners[i][2],
                          sw, sh, sx, sy)) {
            any = true;
            if (sx < minX) minX = sx; if (sx > maxX) maxX = sx;
            if (sy < minY) minY = sy; if (sy > maxY) maxY = sy;
        }
    }
    return any;
}

// ── Lookup de feature no menu ─────────────────────────────────────────────────
static bool IsEnabled(const char* name) {
    for (auto& tab : Menu::tabs)
        for (auto& ft : tab.features)
            if (ft.name == name && ft.enabled) return true;
    return false;
}

// ── Dot de debug no canto ────────────────────────────────────────────────────
// Verde = captura GL ativa | Azul = JNI ativo | Vermelho = nada funcionando
static void DrawDebugDot(bool glOk, bool jniOk) {
    if (glOk)       glColor4f(0.0f, 1.0f, 0.2f, 1.0f); // verde
    else if (jniOk) glColor4f(0.2f, 0.5f, 1.0f, 1.0f); // azul
    else            glColor4f(1.0f, 0.1f, 0.1f, 1.0f); // vermelho
    glBegin(GL_QUADS);
    glVertex2f(0.f,  0.f);
    glVertex2f(16.f, 0.f);
    glVertex2f(16.f, 16.f);
    glVertex2f(0.f,  16.f);
    glEnd();
}

// ── Render ────────────────────────────────────────────────────────────────────
void Render(int sw, int sh) {
    bool espOn      = IsEnabled("ESP");
    bool tracerOn   = IsEnabled("Tracers");
    bool chestOn    = IsEnabled("Chest ESP");
    bool nameTagOn  = IsEnabled("NameTags");

    bool glOk  = !GLCapture::players.empty();
    bool jniOk = false;

    // Tenta JNI como fallback se GL n�o capturou nada
    SDK::CameraInfo cam{};
    std::vector<SDK::EntityInfo> jniPlayers;
    if (!glOk && (espOn || tracerOn)) {
        jniOk = SDK::Minecraft::GetCameraInfo(cam) && cam.valid;
        if (jniOk) SDK::Minecraft::GetNearbyPlayers(jniPlayers);
    }

    DrawDebugDot(glOk, jniOk);

    if (!espOn && !tracerOn && !chestOn && !nameTagOn) return;

    // Cores dinâmicas do menu (fallback para os defaults se ainda não inicializado)
    float pr = 1.0f, pg = 0.18f, pb = 0.18f;
    float cr = 1.0f, cg = 0.70f, cb = 0.20f;
    Menu::GetColor("Player Color", pr, pg, pb);
    Menu::GetColor("Chest Color",  cr, cg, cb);

    glPushAttrib(GL_ALL_ATTRIB_BITS);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_TEXTURE_2D);
    glDisable(GL_LIGHTING);
    glDisable(GL_CULL_FACE);
    glDisable(GL_ALPHA_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // ── Jogadores via matrizes GL (m�todo principal) ──────────────────────────
    if (glOk && (espOn || tracerOn)) {
        for (size_t i = 0; i < GLCapture::players.size(); i++) {
            if (i == 0) continue; // pula jogador local (primeiro a renderizar)

            auto& ent = GLCapture::players[i];
            float minX, maxX, minY, maxY;
            if (!CalcBBox(ent, PLAYER_CORNERS, 8, sw, sh, minX, maxX, minY, maxY))
                continue;

            float bw = maxX - minX, bh = maxY - minY;
            if (bw > (float)sw || bh > (float)sh || bw < 1.f || bh < 1.f)
                continue;

            if (espOn) {
                BoxOutline(minX, minY, bw, bh, pr, pg, pb, 1.0f);
                CornerBox (minX, minY, bw, bh, pr, pg, pb, 0.92f);
                HealthBar (minX, minY, bh);
            }

            if (tracerOn) {
                // Tra�o do centro da tela inferior at� o centro do bot do hitbox
                float fsx, fsy;
                if (MatrixProject(ent.mv, ent.pr, 0.f, -1.f, 0.f, sw, sh, fsx, fsy)) {
                    glColor4f(pr, pg, pb, 0.50f);
                    glLineWidth(1.0f);
                    glBegin(GL_LINES);
                    glVertex2f((float)sw * 0.5f, (float)sh);
                    glVertex2f(fsx, fsy);
                    glEnd();
                }
            }

            if (nameTagOn && Menu::FontReady()) {
                // Projeta ponto acima da cabeça (y=+1.15 = ligeiramente acima do topo)
                float hsx, hsy;
                if (MatrixProject(ent.mv, ent.pr, 0.f, 1.15f, 0.f, sw, sh, hsx, hsy)) {
                    char label[16];
                    snprintf(label, sizeof(label), "Player %d", (int)i);
                    Menu::DrawText2D(sw, sh, hsx, hsy - 14.f,
                                     1.0f, 1.0f, 1.0f, label, /*centerX=*/true);
                }
            }
        }
    }

    // ── Jogadores via JNI (fallback quando GL n�o capturou) ──────────────────
    if (!glOk && jniOk && !jniPlayers.empty() && (espOn || tracerOn)) {
        float aspect = (float)sw / (float)sh;
        for (size_t idx = 1; idx < jniPlayers.size(); idx++) {
            auto& e = jniPlayers[idx];
            const float hw = 0.3f, ht = 1.8f;
            const double xo[2] = {e.posX - hw, e.posX + hw};
            const double zo[2] = {e.posZ - hw, e.posZ + hw};
            const double yo[2] = {e.posY,      e.posY + ht};

            float minX = 1e9f, maxX = -1e9f, minY = 1e9f, maxY = -1e9f;
            bool any = false;
            for (int xi = 0; xi < 2; xi++)
            for (int yi = 0; yi < 2; yi++)
            for (int zi = 0; zi < 2; zi++) {
                float sx, sy;
                if (WorldToScreen(xo[xi], yo[yi], zo[zi],
                                  cam.eyeX, cam.eyeY, cam.eyeZ,
                                  cam.yaw, cam.pitch, cam.fov, aspect,
                                  sw, sh, sx, sy)) {
                    any = true;
                    if (sx < minX) minX = sx; if (sx > maxX) maxX = sx;
                    if (sy < minY) minY = sy; if (sy > maxY) maxY = sy;
                }
            }
            if (!any) continue;
            float bw = maxX - minX, bh = maxY - minY;
            if (bw > (float)sw || bh > (float)sh || bw < 1.f || bh < 1.f) continue;

            if (espOn) {
                BoxOutline(minX, minY, bw, bh, pr, pg, pb, 1.0f);
                CornerBox (minX, minY, bw, bh, pr, pg, pb, 0.92f);
                HealthBar (minX, minY, bh);
            }
            if (tracerOn) {
                float fsx, fsy;
                if (WorldToScreen(e.posX, e.posY, e.posZ,
                                  cam.eyeX, cam.eyeY, cam.eyeZ,
                                  cam.yaw, cam.pitch, cam.fov, aspect,
                                  sw, sh, fsx, fsy)) {
                    glColor4f(pr, pg, pb, 0.50f);
                    glLineWidth(1.0f);
                    glBegin(GL_LINES);
                    glVertex2f((float)sw * 0.5f, (float)sh);
                    glVertex2f(fsx, fsy);
                    glEnd();
                }
            }
        }
    }

    // ── Chest ESP (s� via GL) ─────────────────────────────────────────────────
    if (chestOn) {
        // Ba�s simples — caixa dourada
        for (auto& chest : GLCapture::chests) {
            float minX, maxX, minY, maxY;
            if (!CalcBBox(chest, CHEST_CORNERS, 8, sw, sh, minX, maxX, minY, maxY))
                continue;
            float bw = maxX - minX, bh = maxY - minY;
            if (bw > (float)sw || bh > (float)sh || bw < 1.f || bh < 1.f) continue;
            BoxOutline(minX, minY, bw, bh, cr, cg, cb, 1.0f);
            CornerBox (minX, minY, bw, bh, cr, cg, cb, 0.85f);
        }
        // Ba�s duplos — caixa dourada mais larga
        for (auto& lc : GLCapture::largeChests) {
            float minX, maxX, minY, maxY;
            if (!CalcBBox(lc, LARGE_CORNERS, 8, sw, sh, minX, maxX, minY, maxY))
                continue;
            float bw = maxX - minX, bh = maxY - minY;
            if (bw > (float)sw || bh > (float)sh || bw < 1.f || bh < 1.f) continue;
            BoxOutline(minX, minY, bw, bh, cr, cg, cb, 1.0f);
            CornerBox (minX, minY, bw, bh, cr, cg, cb, 0.85f);
        }
    }

    glPopAttrib();
}

} // namespace ESP
