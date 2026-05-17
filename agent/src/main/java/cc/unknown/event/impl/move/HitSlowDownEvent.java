/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.event.impl.move;

import cc.unknown.event.Event;

public class HitSlowDownEvent
extends Event {
    private double slowDown;
    private boolean sprint;

    public HitSlowDownEvent(double slowDown, boolean sprint) {
        this.slowDown = slowDown;
        this.sprint = sprint;
    }

    public double getSlowDown() {
        return this.slowDown;
    }

    public void setSlowDown(double slowDown) {
        this.slowDown = slowDown;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }
}

