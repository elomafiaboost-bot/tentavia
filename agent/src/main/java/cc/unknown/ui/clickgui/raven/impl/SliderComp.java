/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Gui
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.ui.clickgui.raven.impl;

import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.ui.clickgui.raven.impl.ModuleComp;
import cc.unknown.ui.clickgui.raven.impl.api.Component;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

public class SliderComp
extends Component {
    private final SliderValue v;
    private final ModuleComp p;
    private int offset;
    private int x;
    private int y;
    private boolean dragging = false;
    private double renderWidth;

    public SliderComp(SliderValue v, ModuleComp b, int offset) {
        this.v = v;
        this.p = b;
        this.x = b.category.getX() + b.category.getWidth();
        this.y = b.category.getY() + b.o;
        this.offset = offset;
    }

    @Override
    public void renderComponent() {
        Gui.func_73734_a((int)(this.p.category.getX() + 4), (int)(this.p.category.getY() + this.offset + 11), (int)(this.p.category.getX() + 4 + this.p.category.getWidth() - 8), (int)(this.p.category.getY() + this.offset + 15), (int)-12302777);
        int l = this.p.category.getX() + 4;
        int r = this.p.category.getX() + 4 + (int)this.renderWidth;
        if (r - l > 84) {
            r = l + 84;
        }
        Gui.func_73734_a((int)l, (int)(this.p.category.getY() + this.offset + 11), (int)r, (int)(this.p.category.getY() + this.offset + 15), (int)Theme.instance.getMainColor().getRGB());
        GL11.glPushMatrix();
        GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
        SliderComp.mc.field_71466_p.func_175063_a(this.v.getName() + ": " + this.v.getInput(), (float)((int)((float)(this.p.category.getX() + 4) * 2.0f)), (float)((int)((float)(this.p.category.getY() + this.offset + 3) * 2.0f)), -1);
        GL11.glPopMatrix();
    }

    @Override
    public void setOffset(int n) {
        this.offset = n;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void updateComponent(int mousePosX, int mousePosY) {
        this.y = this.p.category.getY() + this.offset;
        this.x = this.p.category.getX();
        double d = Math.min(this.p.category.getWidth() - 8, Math.max(0, mousePosX - this.x));
        this.renderWidth = (double)(this.p.category.getWidth() - 8) * (this.v.getInput() - this.v.getMin()) / (this.v.getMax() - this.v.getMin());
        if (this.dragging) {
            if (d == 0.0) {
                this.v.setValue(this.v.getMin());
            } else {
                double n = SliderComp.r(d / (double)(this.p.category.getWidth() - 8) * (this.v.getMax() - this.v.getMin()) + this.v.getMin(), 2);
                this.v.setValue(n);
            }
        }
    }

    private static double r(double v, int p) {
        if (p < 0) {
            return 0.0;
        }
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(p, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void mouseClicked(int x, int y, int b) {
        if (this.u(x, y) && b == 0 && this.p.open) {
            this.dragging = true;
        }
        if (this.i(x, y) && b == 0 && this.p.open) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
        this.dragging = false;
    }

    @Override
    public void keyTyped(char t, int k) {
    }

    public boolean u(int x, int y) {
        return x > this.x && x < this.x + this.p.category.getWidth() / 2 + 1 && y > this.y && y < this.y + 16;
    }

    public boolean i(int x, int y) {
        return x > this.x + this.p.category.getWidth() / 2 && x < this.x + this.p.category.getWidth() && y > this.y && y < this.y + 16;
    }
}

