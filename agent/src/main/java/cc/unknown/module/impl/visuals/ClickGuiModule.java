/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.settings.GameSettings
 *  net.minecraft.client.settings.KeyBinding
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.Haru;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.DescValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.ui.clickgui.raven.HaruGui;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

@Register(name="ClickGui", category=Category.Visuals, key=54)
public class ClickGuiModule
extends Module {
    private DescValue a = new DescValue("Color Modes");
    public ModeValue clientTheme = new ModeValue("Color", "Static", "Rainbow", "Pastel", "Memories", "Lilith", "Static", "Cantina");
    private DescValue b = new DescValue("Choose ur perfect waifu");
    public ModeValue waifuMode = new ModeValue("Waifu", "None", "Uzaki", "Megumin", "Ai", "Mai", "Kiwi", "Astolfo", "None");
    private DescValue c = new DescValue("BackGround Modes");
    public ModeValue backGroundMode = new ModeValue("BackGround", "None", "Gradient", "Normal", "None");
    private DescValue d = new DescValue("ClickGui Custom Colors");
    public SliderValue clickGuiColor = new SliderValue("ClickGui Color [H/S/B]", 0.0, 0.0, 350.0, 10.0);
    public SliderValue saturation = new SliderValue("Saturation [H/S/B]", 1.0, 0.0, 1.0, 0.1);
    public SliderValue brightness = new SliderValue("Brightness [H/S/B]", 1.0, 0.0, 1.0, 0.1);
    private final KeyBinding[] moveKeys;

    public ClickGuiModule() {
        this.moveKeys = new KeyBinding[]{ClickGuiModule.mc.field_71474_y.field_74351_w, ClickGuiModule.mc.field_71474_y.field_74368_y, ClickGuiModule.mc.field_71474_y.field_74366_z, ClickGuiModule.mc.field_71474_y.field_74370_x, ClickGuiModule.mc.field_71474_y.field_74314_A, ClickGuiModule.mc.field_71474_y.field_151444_V, ClickGuiModule.mc.field_71474_y.field_74311_E};
        this.registerSetting(this.a, this.clientTheme, this.b, this.waifuMode, this.c, this.backGroundMode, this.d, this.clickGuiColor, this.saturation, this.brightness);
    }

    @Override
    public void onEnable() {
        if (PlayerUtil.inGame() && ClickGuiModule.mc.field_71462_r != Haru.instance.getHaruGui()) {
            mc.func_147108_a((GuiScreen)Haru.instance.getHaruGui());
        }
    }

    @Override
    public void onDisable() {
        if (PlayerUtil.inGame() && ClickGuiModule.mc.field_71462_r instanceof HaruGui) {
            mc.func_147108_a(null);
        }
    }

    @EventLink
    public void onTick(TickEvent e) {
        for (KeyBinding bind : this.moveKeys) {
            bind.field_74513_e = GameSettings.func_100015_a((KeyBinding)bind);
        }
    }
}

