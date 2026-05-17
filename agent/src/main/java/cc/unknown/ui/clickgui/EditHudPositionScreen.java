/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.ScaledResolution
 */
package cc.unknown.ui.clickgui;

import cc.unknown.Haru;
import cc.unknown.utils.client.FuckUtil;
import cc.unknown.utils.helpers.MathHelper;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class EditHudPositionScreen
extends GuiScreen {
    final String hudTextExample = "This is an-Example-HUD";
    GuiButton resetPosButton;
    boolean mouseDown = false;
    int textBoxStartX = 0;
    int textBoxStartY = 0;
    ScaledResolution sr;
    int textBoxEndX = 0;
    int textBoxEndY = 0;
    int marginX = 5;
    int marginY = 70;
    int lastMousePosX = 0;
    int lastMousePosY = 0;
    int sessionMousePosX = 0;
    int sessionMousePosY = 0;
    public static AtomicInteger arrayListX = new AtomicInteger(5);
    public static AtomicInteger arrayListY = new AtomicInteger(70);
    public static final String ArrayListX = "HUDX:";
    public static final String ArrayListY = "HUDY:";

    public void func_73866_w_() {
        super.func_73866_w_();
        this.resetPosButton = new GuiButton(1, this.field_146294_l - 90, 5, 85, 20, "Reset position");
        this.field_146292_n.add(this.resetPosButton);
        this.marginX = arrayListX.get();
        this.marginY = arrayListY.get();
        this.sr = new ScaledResolution(this.field_146297_k);
        FuckUtil.instance.setPositionMode(FuckUtil.instance.getPostitionMode(this.marginX, this.marginY, this.sr.func_78326_a(), this.sr.func_78328_b()));
    }

    public void func_73863_a(int mX, int mY, float pt) {
        EditHudPositionScreen.func_73734_a((int)0, (int)0, (int)this.field_146294_l, (int)this.field_146295_m, (int)-1308622848);
        EditHudPositionScreen.func_73734_a((int)0, (int)(this.field_146295_m / 2), (int)this.field_146294_l, (int)(this.field_146295_m / 2 + 1), (int)-1724499649);
        EditHudPositionScreen.func_73734_a((int)(this.field_146294_l / 2), (int)0, (int)(this.field_146294_l / 2 + 1), (int)this.field_146295_m, (int)-1724499649);
        AtomicInteger textBoxStartX = new AtomicInteger(this.marginX);
        AtomicInteger textBoxStartY = new AtomicInteger(this.marginY);
        int textBoxEndX = textBoxStartX.get() + 50;
        int textBoxEndY = textBoxStartY.get() + 32;
        this.drawArrayList(this.field_146297_k.field_71466_p, this.hudTextExample);
        this.textBoxStartX = textBoxStartX.get();
        this.textBoxStartY = textBoxStartY.get();
        this.textBoxEndX = textBoxEndX;
        this.textBoxEndY = textBoxEndY;
        arrayListX.set(textBoxStartX.get());
        arrayListY.set(textBoxStartY.get());
        try {
            this.func_146269_k();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        super.func_73863_a(mX, mY, pt);
    }

    private void drawArrayList(FontRenderer fr, String t) {
        int x = this.textBoxStartX;
        int gap = this.textBoxEndX - this.textBoxStartX;
        int y = this.textBoxStartY;
        double marginY = fr.field_78288_b + 2;
        String[] var4 = t.split("-");
        ArrayList<String> var5 = MathHelper.toArrayList(var4);
        if (FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.UPLEFT || FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.UPRIGHT) {
            var5.sort((o1, o2) -> this.field_146297_k.field_71466_p.func_78256_a(o2) - this.field_146297_k.field_71466_p.func_78256_a(o1));
        } else if (FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.DOWNLEFT || FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.DOWNRIGHT) {
            var5.sort(Comparator.comparingInt(o2 -> this.field_146297_k.field_71466_p.func_78256_a(o2)));
        }
        if (FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.DOWNRIGHT || FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.UPRIGHT) {
            for (String s : var5) {
                fr.func_175065_a(s, (float)x + (float)(gap - fr.func_78256_a(s)), (float)y, Color.white.getRGB(), true);
                y = (int)((double)y + marginY);
            }
        } else {
            for (String s : var5) {
                fr.func_175065_a(s, (float)x, (float)y, Color.white.getRGB(), true);
                y = (int)((double)y + marginY);
            }
        }
    }

    public void func_146273_a(int mousePosX, int mousePosY, int clickedMouseButton, long timeSinceLastClick) {
        super.func_146273_a(mousePosX, mousePosY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton == 0) {
            if (this.mouseDown) {
                this.marginX = this.lastMousePosX + (mousePosX - this.sessionMousePosX);
                this.marginY = this.lastMousePosY + (mousePosY - this.sessionMousePosY);
                this.sr = new ScaledResolution(this.field_146297_k);
                FuckUtil.instance.setPositionMode(FuckUtil.instance.getPostitionMode(this.marginX, this.marginY, this.sr.func_78326_a(), this.sr.func_78328_b()));
            } else if (mousePosX > this.textBoxStartX && mousePosX < this.textBoxEndX && mousePosY > this.textBoxStartY && mousePosY < this.textBoxEndY) {
                this.mouseDown = true;
                this.sessionMousePosX = mousePosX;
                this.sessionMousePosY = mousePosY;
                this.lastMousePosX = this.marginX;
                this.lastMousePosY = this.marginY;
            }
        }
    }

    public void func_146286_b(int mX, int mY, int state) {
        super.func_146286_b(mX, mY, state);
        if (state == 0) {
            this.mouseDown = false;
        }
    }

    public void func_146284_a(GuiButton b) {
        if (b == this.resetPosButton) {
            int newX = 5;
            int newY = 70;
            this.marginX = newX;
            this.marginY = newY;
            arrayListX.set(newX);
            arrayListY.set(newY);
        }
    }

    public boolean func_73868_f() {
        return false;
    }

    public void func_146281_b() {
        if (Haru.instance.getClientConfig() != null && Haru.instance.getConfigManager() != null) {
            Haru.instance.getConfigManager().save();
            Haru.instance.getClientConfig().saveConfig();
        }
    }
}

