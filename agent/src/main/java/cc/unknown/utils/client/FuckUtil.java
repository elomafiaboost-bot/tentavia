/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.utils.client;

import cc.unknown.ui.clickgui.EditHudPositionScreen;
import cc.unknown.utils.Loona;

public enum FuckUtil implements Loona
{
    instance;

    private PositionMode positionMode;
    private int waifuX = 340;
    private int waifuY = 135;
    public final String WaifuX = "WaifuX:";
    public final String WaifuY = "WaifuY:";

    public PositionMode getPostitionMode(int marginX, int marginY, double height, double width) {
        int halfHeight = (int)(height / 4.0);
        int halfWidth = (int)width;
        PositionMode positionMode = null;
        if (marginY < halfHeight) {
            if (marginX < halfWidth) {
                positionMode = PositionMode.UPLEFT;
            }
            if (marginX > halfWidth) {
                positionMode = PositionMode.UPRIGHT;
            }
        }
        if (marginY > halfHeight) {
            if (marginX < halfWidth) {
                positionMode = PositionMode.DOWNLEFT;
            }
            if (marginX > halfWidth) {
                positionMode = PositionMode.DOWNRIGHT;
            }
        }
        return positionMode;
    }

    public void setArrayListX(int x) {
        EditHudPositionScreen.arrayListX.set(x);
    }

    public void setArrayListY(int x) {
        EditHudPositionScreen.arrayListY.set(x);
    }

    public int getArrayListX() {
        return EditHudPositionScreen.arrayListX.get();
    }

    public int getArrayListY() {
        return EditHudPositionScreen.arrayListY.get();
    }

    public PositionMode getPositionMode() {
        return this.positionMode;
    }

    public void setPositionMode(PositionMode positionMode) {
        this.positionMode = positionMode;
    }

    public int getWaifuX() {
        return this.waifuX;
    }

    public void setWaifuX(int waifuX) {
        this.waifuX = waifuX;
    }

    public int getWaifuY() {
        return this.waifuY;
    }

    public void setWaifuY(int waifuY) {
        this.waifuY = waifuY;
    }

    public static enum PositionMode {
        UPLEFT,
        UPRIGHT,
        DOWNLEFT,
        DOWNRIGHT;

    }
}

