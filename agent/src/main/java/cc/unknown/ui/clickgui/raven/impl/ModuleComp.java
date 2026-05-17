/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.ui.clickgui.raven.impl;

import cc.unknown.module.impl.Module;
import cc.unknown.module.setting.Setting;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DescValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.ui.clickgui.raven.impl.BindComp;
import cc.unknown.ui.clickgui.raven.impl.BooleanComp;
import cc.unknown.ui.clickgui.raven.impl.CategoryComp;
import cc.unknown.ui.clickgui.raven.impl.DescComp;
import cc.unknown.ui.clickgui.raven.impl.DoubleSliderComp;
import cc.unknown.ui.clickgui.raven.impl.ModeComp;
import cc.unknown.ui.clickgui.raven.impl.SliderComp;
import cc.unknown.ui.clickgui.raven.impl.api.Component;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class ModuleComp
extends Component {
    public Module mod;
    public CategoryComp category;
    public int o;
    private final ArrayList<Component> settings;
    public boolean open;

    public ModuleComp(Module mod, CategoryComp p, int o) {
        this.mod = mod;
        this.category = p;
        this.o = o;
        this.settings = new ArrayList();
        this.open = false;
        AtomicInteger y = new AtomicInteger(o + 12);
        mod.getSettings().forEach(setting -> this.addComp((Setting)setting, y.getAndAdd(this.getOffset((Setting)setting))));
        this.settings.add(new BindComp(this, y));
    }

    @Override
    public void setOffset(int n) {
        this.o = n;
        int y = this.o + 16;
        for (Component c : this.settings) {
            c.setOffset(y);
            if (c instanceof SliderComp || c instanceof DoubleSliderComp) {
                y += 16;
                continue;
            }
            if (!(c instanceof BooleanComp) && !(c instanceof DescComp) && !(c instanceof ModeComp) && !(c instanceof BindComp)) continue;
            y += 12;
        }
    }

    public static void e() {
        GL11.glDisable((int)2929);
        GL11.glEnable((int)3042);
        GL11.glDisable((int)3553);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2848);
        GL11.glHint((int)3154, (int)4354);
        GL11.glHint((int)3155, (int)4354);
    }

    public static void f() {
        GL11.glEnable((int)3553);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)2929);
        GL11.glDisable((int)2848);
        GL11.glHint((int)3154, (int)4352);
        GL11.glHint((int)3155, (int)4352);
        GL11.glEdgeFlag((boolean)true);
    }

    public static void g(int h) {
        float a = 0.0f;
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        GL11.glColor4f((float)r, (float)g, (float)b, (float)a);
    }

    public static void v(float x, float y, float x1, float y1, int t, int b) {
        ModuleComp.e();
        GL11.glShadeModel((int)7425);
        GL11.glBegin((int)7);
        ModuleComp.g(t);
        GL11.glVertex2f((float)x, (float)y1);
        GL11.glVertex2f((float)x1, (float)y1);
        ModuleComp.g(b);
        GL11.glVertex2f((float)x1, (float)y);
        GL11.glVertex2f((float)x, (float)y);
        GL11.glEnd();
        GL11.glShadeModel((int)7424);
        ModuleComp.f();
    }

    @Override
    public void renderComponent() {
        ModuleComp.v(this.category.getX(), this.category.getY() + this.o, this.category.getX() + this.category.getWidth(), this.category.getY() + 15 + this.o, this.mod.isEnabled() ? Theme.instance.getMainColor().getRGB() : -12829381, this.mod.isEnabled() ? Theme.instance.getMainColor().getRGB() : -12302777);
        GL11.glPushMatrix();
        int button_rgb = this.mod.isEnabled() ? Theme.instance.getMainColor().getRGB() : (this.mod.canBeEnabled() ? Color.lightGray.getRGB() : new Color(102, 102, 102).getRGB());
        ModuleComp.mc.field_71466_p.func_175063_a(this.mod.getRegister().name(), (float)(this.category.getX() + this.category.getWidth() / 2 - Minecraft.func_71410_x().field_71466_p.func_78256_a(this.mod.getRegister().name()) / 2), (float)(this.category.getY() + this.o + 4), button_rgb);
        GL11.glPopMatrix();
        if (this.open && !this.settings.isEmpty()) {
            this.settings.forEach(Component::renderComponent);
        }
    }

    @Override
    public int getHeight() {
        if (!this.open) {
            return 16;
        }
        int h = 16;
        for (Component c : this.settings) {
            if (c instanceof SliderComp || c instanceof DoubleSliderComp) {
                h += 16;
                continue;
            }
            if (!(c instanceof BooleanComp) && !(c instanceof DescComp) && !(c instanceof ModeComp) && !(c instanceof BindComp)) continue;
            h += 12;
        }
        return h;
    }

    @Override
    public void updateComponent(int mousePosX, int mousePosY) {
        if (!this.settings.isEmpty()) {
            this.settings.forEach(comp -> comp.updateComponent(mousePosX, mousePosY));
        }
    }

    @Override
    public void mouseClicked(int x, int y, int b) {
        if (this.mod.canBeEnabled() && this.isMouseOnButton(x, y)) {
            switch (b) {
                case 0: {
                    this.mod.toggle();
                    break;
                }
                case 1: {
                    this.open = !this.open;
                    this.category.refresh();
                }
            }
        }
        this.settings.forEach(comp -> comp.mouseClicked(x, y, b));
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
        this.settings.forEach(comp -> comp.mouseReleased(x, y, m));
    }

    @Override
    public void keyTyped(char t, int k) {
        this.settings.forEach(comp -> comp.keyTyped(t, k));
    }

    public boolean isMouseOnButton(int x, int y) {
        return x > this.category.getX() && x < this.category.getX() + this.category.getWidth() && y > this.category.getY() + this.o && y < this.category.getY() + 16 + this.o;
    }

    private void addComp(Setting setting, int y) {
        if (setting instanceof SliderValue) {
            this.settings.add(new SliderComp((SliderValue)setting, this, y));
        } else if (setting instanceof BooleanValue) {
            this.settings.add(new BooleanComp(this.mod, (BooleanValue)setting, this, y));
        } else if (setting instanceof DescValue) {
            this.settings.add(new DescComp((DescValue)setting, this, y));
        } else if (setting instanceof DoubleSliderValue) {
            this.settings.add(new DoubleSliderComp((DoubleSliderValue)setting, this, y));
        } else if (setting instanceof ModeValue) {
            this.settings.add(new ModeComp((ModeValue)setting, this, y));
        }
    }

    private int getOffset(Setting setting) {
        return setting instanceof SliderValue || setting instanceof DoubleSliderValue ? 16 : 12;
    }
}

