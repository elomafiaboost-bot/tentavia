/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.ui.clickgui.raven.impl;

import cc.unknown.module.impl.visuals.ClickGuiModule;
import cc.unknown.ui.clickgui.raven.impl.ModuleComp;
import cc.unknown.ui.clickgui.raven.impl.api.Component;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class BindComp
extends Component {
    private boolean isBinding;
    private final ModuleComp p;
    private final AtomicInteger o;
    private int x;
    private int y;

    public BindComp(ModuleComp b, AtomicInteger o) {
        this.p = b;
        this.o = o;
        this.x = b.category.getX() + b.category.getWidth();
        this.y = b.category.getY() + o.get();
    }

    @Override
    public void renderComponent() {
        GL11.glPushMatrix();
        GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
        this.dr(this.isBinding ? "Select a key..." : "Bind: " + Keyboard.getKeyName((int)this.p.mod.getKey()));
        GL11.glPopMatrix();
    }

    @Override
    public void updateComponent(int mousePosX, int mousePosY) {
        this.y = this.p.category.getY() + this.o.get();
        this.x = this.p.category.getX();
    }

    @Override
    public void mouseClicked(int x, int y, int b) {
        if (this.i(x, y) && b == 0 && this.p.open) {
            this.isBinding = !this.isBinding;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
    }

    @Override
    public void keyTyped(char t, int k) {
        if (this.isBinding) {
            this.p.mod.setKey(k == 11 && this.p.mod instanceof ClickGuiModule ? 28 : k);
            this.isBinding = false;
        }
    }

    @Override
    public void setOffset(int n) {
        this.o.set(n);
    }

    @Override
    public int getHeight() {
        return 16;
    }

    private boolean i(int x, int y) {
        return x > this.x && x < this.x + this.p.category.getWidth() && y > this.y - 1 && y < this.y + 12;
    }

    private void dr(String s) {
        BindComp.mc.field_71466_p.func_175063_a(s, (float)((this.p.category.getX() + 4) * 2), (float)((this.p.category.getY() + this.o.get() + 3) * 2), Theme.instance.getMainColor().getRGB());
    }
}

