/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 */
package cc.unknown.event.impl.render;

import cc.unknown.event.Event;
import net.minecraft.entity.Entity;

public class RenderEvent
extends Event {
    private float partialTicks;
    private Entity target;
    private double x;
    private double y;
    private double z;
    private final RenderType renderType;

    public RenderEvent(RenderType renderType, Entity target, double x, double y, double z) {
        this.renderType = renderType;
        this.target = target;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public RenderEvent(RenderType renderType, float partialTicks) {
        this.renderType = renderType;
        this.partialTicks = partialTicks;
    }

    public RenderEvent(RenderType renderType) {
        this.renderType = renderType;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public Entity getTarget() {
        return this.target;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public RenderType getRenderType() {
        return this.renderType;
    }

    public boolean is3D() {
        return this.renderType == RenderType.Render3D;
    }

    public boolean is2D() {
        return this.renderType == RenderType.Render2D;
    }

    public boolean isLabel() {
        return this.renderType == RenderType.RenderLabel;
    }

    public static enum RenderType {
        Render3D,
        Render2D,
        RenderLabel;

    }
}

