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
};

namespace Classes {
    extern const char* Minecraft;
    extern const char* EntityPlayerSP;
    extern const char* WorldClient;
}

} // namespace SDK
