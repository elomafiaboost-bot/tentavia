package cc.unknown.mixin.interfaces.network;

import net.minecraft.network.Packet;

public interface INetHandlerPlayClient {
    void receiveQueueNoEvent(Packet<?> packet);
}
