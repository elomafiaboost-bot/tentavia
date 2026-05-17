/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemEgg
 *  net.minecraft.item.ItemFishingRod
 *  net.minecraft.item.ItemSnowball
 *  net.minecraft.item.ItemStack
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;

@Register(name="FastPlace", category=Category.Player)
public class FastPlace
extends Module {
    private SliderValue delaySlider = new SliderValue("Delay", 1.0, 0.0, 4.0, 0.5);
    private BooleanValue blockOnly = new BooleanValue("Blocks only", true);
    private BooleanValue projSeparate = new BooleanValue("Separate Projectile Delay", true);
    private BooleanValue pitchCheck = new BooleanValue("Pitch Check", false);
    private SliderValue projSlider = new SliderValue("Projectile Delay", 2.0, 0.0, 4.0, 0.5);

    public FastPlace() {
        this.registerSetting(this.delaySlider, this.blockOnly, this.projSeparate, this.pitchCheck, this.projSlider);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.delaySlider.getInput() + " ticks]");
    }

    @Override
    public boolean canBeEnabled() {
        return FastPlace.mc.field_71467_ac != 4;
    }

    @EventLink
    public void onTick(TickEvent e) {
        try {
            if (PlayerUtil.inGame() && FastPlace.mc.field_71415_G) {
                ItemStack item = FastPlace.mc.field_71439_g.func_70694_bm();
                if (item.func_77973_b() instanceof ItemFishingRod && item != null) {
                    return;
                }
                if (!this.pitchCheck.isToggled() || !(FastPlace.mc.field_71439_g.field_70125_A < 70.0f)) {
                    if (this.blockOnly.isToggled() && item != null) {
                        if (item.func_77973_b() instanceof ItemBlock) {
                            this.rightDelay(this.delaySlider.getInputToInt());
                        } else if ((item.func_77973_b() instanceof ItemSnowball || item.func_77973_b() instanceof ItemEgg) && this.projSeparate.isToggled()) {
                            this.rightDelay(this.projSlider.getInputToInt());
                        }
                    } else {
                        this.rightDelay(this.delaySlider.getInputToInt());
                    }
                }
            }
        }
        catch (NullPointerException nullPointerException) {
            // empty catch block
        }
    }

    private void rightDelay(int x) {
        if (x == 0) {
            FastPlace.mc.field_71467_ac = 0;
        } else if (x != 4 && FastPlace.mc.field_71467_ac == 4) {
            FastPlace.mc.field_71467_ac = x;
        }
    }
}

