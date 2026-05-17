/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.ui.clickgui.raven.impl;

import cc.unknown.module.setting.impl.DescValue;
import cc.unknown.ui.clickgui.raven.impl.ModuleComp;
import cc.unknown.ui.clickgui.raven.impl.api.Component;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import org.lwjgl.opengl.GL11;

public class DescComp
extends Component {
    private final DescValue desc;
    private final ModuleComp p;
    private int o;

    public DescComp(DescValue desc, ModuleComp b, int o) {
        this.desc = desc;
        this.p = b;
        this.o = o;
    }

    @Override
    public void renderComponent() {
        GL11.glPushMatrix();
        GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
        DescComp.mc.field_71466_p.func_175063_a(this.desc.getDesc(), (float)((this.p.category.getX() + 4) * 2), (float)((this.p.category.getY() + this.o + 4) * 2), Theme.instance.getMainColor().getRGB());
        GL11.glPopMatrix();
    }

    @Override
    public void updateComponent(int mousePosX, int mousePosY) {
    }

    @Override
    public void mouseClicked(int x, int y, int b) {
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
    }

    @Override
    public void keyTyped(char t, int k) {
    }

    @Override
    public void setOffset(int n) {
        this.o = n;
    }

    @Override
    public int getHeight() {
        return 12;
    }
}

