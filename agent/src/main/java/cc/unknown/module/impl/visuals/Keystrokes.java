/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import cc.unknown.utils.client.RenderUtil;
import java.awt.Color;

@Register(name="Keystrokes", category=Category.Visuals)
public class Keystrokes
extends Module {
    public int lastA = 0;
    public int lastW = 0;
    public int lastS = 0;
    public int lastD = 0;
    public long deltaAnim = 0L;

    @EventLink
    public void onDraw(RenderEvent e) {
        if (e.is2D()) {
            boolean A = Keystrokes.mc.field_71474_y.field_74370_x.func_151470_d();
            boolean W = Keystrokes.mc.field_71474_y.field_74351_w.func_151470_d();
            boolean S = Keystrokes.mc.field_71474_y.field_74368_y.func_151470_d();
            boolean D = Keystrokes.mc.field_71474_y.field_74366_z.func_151470_d();
            int targetA = A ? 255 : 0;
            int targetW = W ? 255 : 0;
            int targetS = S ? 255 : 0;
            int targetD = D ? 255 : 0;
            float delta = (float)this.deltaAnim / 1000.0f;
            float speed = 8.0f;
            this.lastA = (int)this.approach(this.lastA, targetA, speed * delta);
            this.lastW = (int)this.approach(this.lastW, targetW, speed * delta);
            this.lastS = (int)this.approach(this.lastS, targetS, speed * delta);
            this.lastD = (int)this.approach(this.lastD, targetD, speed * delta);
            this.drawKeyIndicator("A", this.lastA, 5.0f, 49.0f);
            this.drawKeyIndicator("W", this.lastW, 27.0f, 27.0f);
            this.drawKeyIndicator("S", this.lastS, 27.0f, 49.0f);
            this.drawKeyIndicator("D", this.lastD, 49.0f, 49.0f);
        }
    }

    private void drawKeyIndicator(String keyLabel, int alpha, float x1, float y1) {
        float size = 20.0f;
        float x2 = x1 + size;
        float y2 = y1 + size;
        RenderUtil.drawRect(x1, y1, x2, y2, new Color(0, 0, 0, 150).getRGB());
        RenderUtil.drawRect(x1, y1, x2, y2, new Color(alpha, alpha, alpha, 150).getRGB());
        Keystrokes.mc.field_71466_p.func_175063_a(keyLabel, x1 + 8.0f, y1 + 5.0f, Theme.instance.getMainColor().getRGB());
    }

    private float approach(float current, float target, float maxChange) {
        if (current == target) {
            return current;
        }
        float difference = target - current;
        float sign = Math.signum(difference);
        float change = Math.min(Math.abs(difference), maxChange);
        return current + sign * change;
    }
}

