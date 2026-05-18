package cc.unknown.mixin.interfaces.network;

import net.minecraft.network.Packet;

public interface INetworkManager {
    void sendPacketNoEvent(Packet<?> packet);
}
