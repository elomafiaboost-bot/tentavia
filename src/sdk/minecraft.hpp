#pragma once
#include <string>
#include <vector>

namespace SDK {

struct EntityInfo {
    std::string name;
    double posX, posY, posZ;
    float  yaw = 0.0f, pitch = 0.0f;
};

struct CameraInfo {
    double eyeX, eyeY, eyeZ;
    float  yaw, pitch, fov;
    bool   valid = false;
};

class Minecraft {
public:
    static void PrintLocalPlayerName();
    static bool GetNearbyPlayers(std::vector<EntityInfo>& out);
    static bool GetCameraInfo(CameraInfo& out);
    // Zera motionX/Z do jogador local para cancelar knockback horizontal.
    static bool ApplyAntiKB();
    // Escreve diretamente no keyDownBuffer do LWJGL (KEY_LSHIFT=42).
    // Mais confiável que SendInput pois bypassa o Raw Input do LWJGL 2.
    static bool SetSneakKeyState(bool pressed);
};

namespace Classes {
    extern const char* Minecraft;
    extern const char* EntityPlayerSP;
    extern const char* WorldClient;
}

} // namespace SDK
