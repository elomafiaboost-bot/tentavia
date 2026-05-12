#include "minecraft.hpp"
#include <iostream>

namespace SDK {
    namespace Classes {
        const char* Minecraft = "net/minecraft/client/Minecraft";
        const char* EntityPlayerSP = "net/minecraft/client/entity/EntityPlayerSP";
        const char* WorldClient = "net/minecraft/client/multiplayer/WorldClient";
    }

    void Minecraft::PrintLocalPlayerName() {
        std::cout << "[SDK] Tentando ler o nome do LocalPlayer..." << std::endl;
    }
}
