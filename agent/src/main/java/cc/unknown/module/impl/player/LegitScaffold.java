/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.WorldSettings$GameType
 *  org.lwjgl.input.Keyboard
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.utils.client.Cold;
import cc.unknown.utils.helpers.MathHelper;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import org.lwjgl.input.Keyboard;

@Register(name="LegitScaffold", category=Category.Player)
public class LegitScaffold
extends Module {
    public BooleanValue shiftOnJump = new BooleanValue("Shift While in Air", false);
    public DoubleSliderValue shiftTime = new DoubleSliderValue("Shift Time", 140.0, 200.0, 0.0, 280.0, 5.0);
    public DoubleSliderValue pitchRange = new DoubleSliderValue("Pitch Angle Range", 70.0, 85.0, 0.0, 90.0, 1.0);
    private BooleanValue onHold = new BooleanValue("On Shift Hold", false);
    public BooleanValue blocksOnly = new BooleanValue("Blocks Only", true);
    public BooleanValue backwards = new BooleanValue("Backwards Movement Only", true);
    public BooleanValue slotSwap = new BooleanValue("Block Switching", true);
    private boolean shouldBridge = false;
    private boolean isShifting = false;
    private Cold shiftTimer = new Cold(0L);

    public LegitScaffold() {
        this.registerSetting(this.shiftOnJump, this.shiftTime, this.pitchRange, this.onHold, this.blocksOnly, this.backwards, this.slotSwap);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.shiftTime.getInputMinToInt() + ", " + this.shiftTime.getInputMaxToInt() + " ms]");
    }

    @Override
    public void onDisable() {
        this.setSneak(false);
        if (PlayerUtil.playerOverAir()) {
            this.setSneak(false);
        }
        this.shouldBridge = false;
        this.isShifting = false;
    }

    @EventLink
    public void onSuicide(TickEvent e) {
        ItemStack i;
        boolean x;
        if (LegitScaffold.mc.field_71462_r != null || !PlayerUtil.inGame()) {
            return;
        }
        boolean bl = x = this.shiftTime.getInputMax() > 0.0;
        if ((double)LegitScaffold.mc.field_71439_g.field_70125_A < this.pitchRange.getInputMin() || (double)LegitScaffold.mc.field_71439_g.field_70125_A > this.pitchRange.getInputMax()) {
            this.shouldBridge = false;
            if (Keyboard.isKeyDown((int)LegitScaffold.mc.field_71474_y.field_74311_E.func_151463_i())) {
                this.setSneak(true);
            }
            return;
        }
        if (this.onHold.isToggled() && !Keyboard.isKeyDown((int)LegitScaffold.mc.field_71474_y.field_74311_E.func_151463_i())) {
            this.shouldBridge = false;
            return;
        }
        if (LegitScaffold.mc.field_71442_b.func_178889_l() == WorldSettings.GameType.SPECTATOR) {
            return;
        }
        if (this.blocksOnly.isToggled() && ((i = LegitScaffold.mc.field_71439_g.func_70694_bm()) == null || !(i.func_77973_b() instanceof ItemBlock))) {
            if (this.isShifting) {
                this.isShifting = false;
                this.setSneak(false);
            }
            return;
        }
        if (this.backwards.isToggled() && (LegitScaffold.mc.field_71439_g.field_71158_b.field_78900_b > 0.0f && LegitScaffold.mc.field_71439_g.field_71158_b.field_78902_a == 0.0f || LegitScaffold.mc.field_71439_g.field_71158_b.field_78900_b >= 0.0f)) {
            this.shouldBridge = false;
            return;
        }
        if (LegitScaffold.mc.field_71439_g.field_70122_E) {
            if (PlayerUtil.playerOverAir()) {
                if (x) {
                    this.shiftTimer.setCooldown(MathHelper.randomInt(this.shiftTime.getInputMin(), this.shiftTime.getInputMax() + 0.1));
                    this.shiftTimer.start();
                }
                this.isShifting = true;
                this.setSneak(true);
                this.shouldBridge = true;
            } else if (LegitScaffold.mc.field_71439_g.func_70093_af() && !Keyboard.isKeyDown((int)LegitScaffold.mc.field_71474_y.field_74311_E.func_151463_i()) && this.onHold.isToggled()) {
                this.isShifting = false;
                this.shouldBridge = false;
                this.setSneak(false);
            } else if (this.onHold.isToggled() && !Keyboard.isKeyDown((int)LegitScaffold.mc.field_71474_y.field_74311_E.func_151463_i())) {
                this.isShifting = false;
                this.shouldBridge = false;
                this.setSneak(false);
            } else if (LegitScaffold.mc.field_71439_g.func_70093_af() && Keyboard.isKeyDown((int)LegitScaffold.mc.field_71474_y.field_74311_E.func_151463_i()) && this.onHold.isToggled() && (!x || this.shiftTimer.hasFinished())) {
                this.isShifting = false;
                this.setSneak(false);
                this.shouldBridge = true;
            } else if (LegitScaffold.mc.field_71439_g.func_70093_af() && !this.onHold.isToggled() && (!x || this.shiftTimer.hasFinished())) {
                this.isShifting = false;
                this.setSneak(false);
                this.shouldBridge = true;
            }
        } else if (this.shouldBridge && LegitScaffold.mc.field_71439_g.field_71075_bZ.field_75100_b) {
            this.setSneak(false);
            this.shouldBridge = false;
        } else if (this.shouldBridge && PlayerUtil.playerOverAir() && this.shiftOnJump.isToggled()) {
            this.isShifting = true;
            this.setSneak(true);
        } else {
            this.isShifting = false;
            this.setSneak(false);
        }
    }

    @EventLink
    public void onRender(RenderEvent e) {
        if (PlayerUtil.inGame() && e.is3D()) {
            if ((LegitScaffold.mc.field_71439_g.func_70694_bm() == null || !(LegitScaffold.mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemBlock)) && this.slotSwap.isToggled()) {
                this.swapToBlock();
            }
            if (LegitScaffold.mc.field_71462_r != null || LegitScaffold.mc.field_71439_g.func_70694_bm() == null) {
                return;
            }
        }
    }

    public void swapToBlock() {
        for (int slot = 0; slot <= 8; ++slot) {
            ItemStack itemInSlot = LegitScaffold.mc.field_71439_g.field_71071_by.func_70301_a(slot);
            if (itemInSlot == null || !(itemInSlot.func_77973_b() instanceof ItemBlock) || itemInSlot.field_77994_a <= 0) continue;
            ItemBlock itemBlock = (ItemBlock)itemInSlot.func_77973_b();
            Block block = itemBlock.func_179223_d();
            if (LegitScaffold.mc.field_71439_g.field_71071_by.field_70461_c == slot || !block.func_149686_d()) {
                return;
            }
            LegitScaffold.mc.field_71439_g.field_71071_by.field_70461_c = slot;
            return;
        }
    }

    private void setSneak(boolean sneak) {
        LegitScaffold.mc.field_71474_y.field_74311_E.field_74513_e = sneak;
    }
}

