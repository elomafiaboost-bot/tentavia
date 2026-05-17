/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.ScaledResolution
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.helpers.CPSHelper;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.ScaledResolution;

@Register(name="CpsDisplay", category=Category.Visuals)
public class CpsDisplay
extends Module {
    private AtomicInteger width = new AtomicInteger();
    private AtomicInteger height = new AtomicInteger();
    private BooleanValue showLeft = new BooleanValue("Left button", true);
    private BooleanValue showRight = new BooleanValue("Right button", false);
    private SliderValue color = new SliderValue("Color [H/S/B]", 0.0, 0.0, 350.0, 10.0);

    public CpsDisplay() {
        this.registerSetting(this.showLeft, this.showRight, this.color);
    }

    @EventLink
    public void onDraw(RenderEvent e) {
        if (e.is2D()) {
            if (CpsDisplay.mc.field_71462_r != null || CpsDisplay.mc.field_71474_y.field_74330_P) {
                return;
            }
            ScaledResolution res = new ScaledResolution(mc);
            int screenWidth = res.func_78326_a();
            int screenHeight = res.func_78328_b();
            this.width.set(screenWidth / 2);
            this.height.set(screenHeight / 100);
            this.drawWithBackground(this.showLeft, CPSHelper.getCPS(CPSHelper.MouseButton.LEFT) + " Left CPS", () -> this.width.get() - 5, this.height::get, screenWidth, screenHeight);
            this.drawWithBackground(this.showRight, "Right CPS " + CPSHelper.getCPS(CPSHelper.MouseButton.RIGHT), () -> this.width.get() + 72, this.height::get, screenWidth, screenHeight);
        }
    }

    private void drawWithBackground(BooleanValue bool, String text, IntSupplier xSupplier, IntSupplier ySupplier, int screenWidth, int screenHeight) {
        if (bool.isToggled()) {
            int textWidth = CpsDisplay.mc.field_71466_p.func_78256_a(text);
            int x = xSupplier.getAsInt() - textWidth;
            int y = ySupplier.getAsInt();
            CpsDisplay.mc.field_71466_p.func_175065_a(text, (float)x, (float)y, Color.getHSBColor(this.color.getInputToFloat() % 360.0f / 360.0f, 1.0f, 1.0f).getRGB(), true);
        }
    }
}

