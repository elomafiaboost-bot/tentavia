/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.potion.Potion
 *  net.minecraft.potion.PotionEffect
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.LivingEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

@Register(name="Fullbright", category=Category.Visuals)
public class Fullbright
extends Module {
    private ModeValue mode = new ModeValue("Mode", "Gamma", "Gamma", "Night Vision");
    public BooleanValue confusion = new BooleanValue("Remove confusion effect", true);
    public SliderValue fire = new SliderValue("Fire Alpha", 0.0, 0.0, 1.0, 0.1);
    private float prevGamma = 0.0f;

    public Fullbright() {
        this.registerSetting(this.mode, this.confusion, this.fire);
    }

    @Override
    public void onEnable() {
        this.prevGamma = Fullbright.mc.field_71474_y.field_74333_Y;
    }

    @Override
    public void onDisable() {
        if (this.prevGamma == 0.0f) {
            return;
        }
        Fullbright.mc.field_71474_y.field_74333_Y = this.prevGamma;
        this.prevGamma = 0.0f;
        if (Fullbright.mc.field_71439_g != null) {
            Fullbright.mc.field_71439_g.func_70618_n(Potion.field_76439_r.field_76415_H);
        }
    }

    @EventLink
    public void onUpdate(LivingEvent e) {
        if (this.mode.is("Gamma")) {
            if (Fullbright.mc.field_71474_y.field_74333_Y <= 1.0E7f) {
                Fullbright.mc.field_71474_y.field_74333_Y += 1.0f;
            }
        } else if (this.mode.is("Night Vision")) {
            Fullbright.mc.field_71439_g.func_70690_d(new PotionEffect(Potion.field_76439_r.field_76415_H, 1337, 1));
        } else if (this.prevGamma != 0.0f) {
            Fullbright.mc.field_71474_y.field_74333_Y = this.prevGamma;
            this.prevGamma = 0.0f;
        }
    }
}

