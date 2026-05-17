/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.EnumPacketDirection
 *  net.minecraft.network.Packet
 */
package cc.unknown.event.impl.network;

import cc.unknown.event.Event;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;

public class PacketEvent
extends Event {
    private final EnumPacketDirection direction;
    private Packet<?> packet;

    public PacketEvent(EnumPacketDirection direction, Packet<?> packet) {
        this.direction = direction;
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public boolean isSend() {
        return this.direction == EnumPacketDirection.CLIENTBOUND;
    }

    public boolean isReceive() {
        return this.direction == EnumPacketDirection.SERVERBOUND;
    }
}

