/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.WorldClient
 */
package cc.unknown.event.impl.world;

import cc.unknown.event.Event;
import net.minecraft.client.multiplayer.WorldClient;

public class WorldEvent
extends Event {
    private final WorldClient worldClient;

    public WorldEvent(WorldClient worldClient) {
        this.worldClient = worldClient;
    }

    public WorldClient getWorldClient() {
        return this.worldClient;
    }
}

