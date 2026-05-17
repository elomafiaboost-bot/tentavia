/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.Packet
 */
package cc.unknown.utils.network;

import cc.unknown.utils.client.Cold;
import net.minecraft.network.Packet;

public class TimedPacket {
    private final Packet<?> packet;
    private final Cold time;
    private final long millis;

    public TimedPacket(Packet<?> packet) {
        this.packet = packet;
        this.time = new Cold();
        this.millis = System.currentTimeMillis();
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public Cold getCold() {
        return this.getTime();
    }

    public Cold getTime() {
        return this.time;
    }

    public long getMillis() {
        return this.millis;
    }
}

