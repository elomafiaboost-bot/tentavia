/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.event.impl.move;

import cc.unknown.event.Event;

public class MoveEvent
extends Event {
    private double motionX;
    private double motionY;
    private double motionZ;
    private boolean saveWalk;
    private boolean disableSneak;

    public MoveEvent(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
    }

    public double getMotionX() {
        return this.motionX;
    }

    public void setMotionX(double motionX) {
        this.motionX = motionX;
    }

    public double getMotionY() {
        return this.motionY;
    }

    public void setMotionY(double motionY) {
        this.motionY = motionY;
    }

    public double getMotionZ() {
        return this.motionZ;
    }

    public void setMotionZ(double motionZ) {
        this.motionZ = motionZ;
    }

    public boolean isSaveWalk() {
        return this.saveWalk;
    }

    public void setSaveWalk(boolean saveWalk) {
        this.saveWalk = saveWalk;
    }

    public boolean isDisableSneak() {
        return this.disableSneak;
    }

    public void setDisableSneak(boolean disableSneak) {
        this.disableSneak = disableSneak;
    }
}

