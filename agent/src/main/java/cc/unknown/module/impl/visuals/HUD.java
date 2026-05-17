/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.GuiScreen
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.Haru;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.ui.clickgui.EditHudPositionScreen;
import cc.unknown.ui.clickgui.raven.HaruGui;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import cc.unknown.utils.client.ColorUtil;
import cc.unknown.utils.client.FuckUtil;
import cc.unknown.utils.misc.HiddenUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

@Register(name="HUD", category=Category.Visuals)
public class HUD
extends Module {
    private ModeValue colorMode = new ModeValue("ArrayList Theme", "Static", "Static", "Slinky", "Astolfo", "Primavera", "Ocean", "Theme");
    private SliderValue arrayColor = new SliderValue("Array Color [H/S/B]", 0.0, 0.0, 350.0, 10.0);
    private SliderValue saturation = new SliderValue("Saturation [H/S/B]", 1.0, 0.0, 1.0, 0.1);
    private SliderValue brightness = new SliderValue("Brightness [H/S/B]", 1.0, 0.0, 1.0, 0.1);
    private BooleanValue editPosition = new BooleanValue("Edit Position", false);
    private BooleanValue noRenderModules = new BooleanValue("No Render Modules", true);
    private BooleanValue background = new BooleanValue("Background", true);
    private BooleanValue lowercase = new BooleanValue("Lowercase", false);
    public BooleanValue suffix = new BooleanValue("Suffix", false);

    public HUD() {
        this.registerSetting(this.colorMode, this.arrayColor, this.saturation, this.brightness, this.editPosition, this.noRenderModules, this.background, this.lowercase, this.suffix);
    }

    @Override
    public void onEnable() {
        Haru.instance.getModuleManager().sort();
    }

    @Override
    public void guiButtonToggled(BooleanValue b) {
        if (b == this.editPosition) {
            this.editPosition.disable();
            mc.func_147108_a((GuiScreen)new EditHudPositionScreen());
        }
    }

    @EventLink
    public void onDraw(RenderEvent e) {
        if (e.is2D()) {
            ArrayList<Module> en;
            if (HUD.mc.field_71474_y.field_74330_P || HUD.mc.field_71462_r instanceof HaruGui) {
                return;
            }
            HiddenUtil.setVisible(!this.noRenderModules.isToggled());
            int margin = 2;
            AtomicInteger y = new AtomicInteger(EditHudPositionScreen.arrayListY.get());
            if (Arrays.asList(FuckUtil.PositionMode.DOWNLEFT, FuckUtil.PositionMode.DOWNRIGHT).contains((Object)FuckUtil.instance.getPositionMode())) {
                Haru.instance.getModuleManager().sort();
            }
            if ((en = new ArrayList<Module>(Haru.instance.getModuleManager().getModule())).isEmpty()) {
                return;
            }
            AtomicInteger textBoxWidth = new AtomicInteger(Haru.instance.getModuleManager().getLongestActiveModule(HUD.mc.field_71466_p));
            AtomicInteger textBoxHeight = new AtomicInteger(Haru.instance.getModuleManager().getBoxHeight(HUD.mc.field_71466_p, margin));
            if (EditHudPositionScreen.arrayListX.get() < 0) {
                EditHudPositionScreen.arrayListX.set(margin);
            }
            if (EditHudPositionScreen.arrayListY.get() < 0) {
                EditHudPositionScreen.arrayListY.set(margin);
            }
            EditHudPositionScreen.arrayListX.set(EditHudPositionScreen.arrayListX.get() + textBoxWidth.get() > HUD.mc.field_71443_c / 2 ? HUD.mc.field_71443_c / 2 - textBoxWidth.get() - margin : EditHudPositionScreen.arrayListX.get());
            EditHudPositionScreen.arrayListY.set(EditHudPositionScreen.arrayListY.get() + textBoxHeight.get() > HUD.mc.field_71440_d / 2 ? HUD.mc.field_71440_d / 2 - textBoxHeight.get() : EditHudPositionScreen.arrayListY.get());
            AtomicInteger color = new AtomicInteger(0);
            en.stream().filter(m -> m.isEnabled() && m.isHidden()).forEach(m -> {
                String nameOrSuffix = m.getRegister().name();
                if (this.suffix.isToggled()) {
                    nameOrSuffix = nameOrSuffix + " \u00a77" + m.getSuffix();
                }
                if (this.lowercase.isToggled()) {
                    nameOrSuffix = nameOrSuffix.toLowerCase();
                }
                switch (this.colorMode.getMode()) {
                    case "Static": {
                        color.set(Color.getHSBColor(this.arrayColor.getInputToFloat() % 360.0f / 360.0f, this.saturation.getInputToFloat(), this.brightness.getInputToFloat()).getRGB());
                        y.addAndGet(HUD.mc.field_71466_p.field_78288_b + margin);
                        break;
                    }
                    case "Slinky": {
                        color.set(ColorUtil.reverseGradientDraw(new Color(255, 165, 128), new Color(255, 0, 255), y.get()).getRGB());
                        y.addAndGet(HUD.mc.field_71466_p.field_78288_b + margin);
                        break;
                    }
                    case "Astolfo": {
                        color.set(ColorUtil.reverseGradientDraw(new Color(243, 145, 216), new Color(152, 165, 243), new Color(64, 224, 208), y.get()).getRGB());
                        y.addAndGet(HUD.mc.field_71466_p.field_78288_b + margin);
                        break;
                    }
                    case "Primavera": {
                        color.set(ColorUtil.reverseGradientDraw(new Color(0, 206, 209), new Color(255, 255, 224), new Color(211, 211, 211), y.get()).getRGB());
                        y.addAndGet(HUD.mc.field_71466_p.field_78288_b + margin);
                        break;
                    }
                    case "Ocean": {
                        color.set(ColorUtil.reverseGradientDraw(new Color(0, 0, 128), new Color(0, 255, 255), new Color(173, 216, 230), y.get()).getRGB());
                        y.addAndGet(HUD.mc.field_71466_p.field_78288_b + margin);
                        break;
                    }
                    case "Theme": {
                        color.set(Theme.instance.getMainColor().getRGB());
                        y.addAndGet(HUD.mc.field_71466_p.field_78288_b + margin);
                    }
                }
                if (FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.DOWNRIGHT || FuckUtil.instance.getPositionMode() == FuckUtil.PositionMode.UPRIGHT) {
                    if (this.background.isToggled()) {
                        int backgroundWidth = HUD.mc.field_71466_p.func_78256_a(nameOrSuffix) + 5;
                        Gui.func_73734_a((int)(EditHudPositionScreen.arrayListX.get() + textBoxWidth.get() + 4), (int)y.get(), (int)(EditHudPositionScreen.arrayListX.get() + (textBoxWidth.get() - backgroundWidth)), (int)(y.get() + HUD.mc.field_71466_p.field_78288_b + 2), (int)new Color(0, 0, 0, 87).getRGB());
                    }
                    HUD.mc.field_71466_p.func_175065_a(nameOrSuffix, (float)EditHudPositionScreen.arrayListX.get() + (float)(textBoxWidth.get() - HUD.mc.field_71466_p.func_78256_a(nameOrSuffix)), (float)y.get() + 2.0f, color.get(), true);
                } else {
                    if (this.background.isToggled()) {
                        int backgroundWidth = HUD.mc.field_71466_p.func_78256_a(nameOrSuffix) + 4;
                        Gui.func_73734_a((int)(EditHudPositionScreen.arrayListX.get() - 3), (int)y.get(), (int)(EditHudPositionScreen.arrayListX.get() + backgroundWidth), (int)(y.get() + HUD.mc.field_71466_p.field_78288_b + 2), (int)new Color(0, 0, 0, 100).getRGB());
                    }
                    HUD.mc.field_71466_p.func_175065_a(nameOrSuffix, (float)EditHudPositionScreen.arrayListX.get(), (float)y.get() + 2.0f, color.get(), true);
                }
            });
        }
    }
}

