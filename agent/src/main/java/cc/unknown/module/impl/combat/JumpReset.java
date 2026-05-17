/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.player.EntityPlayer
 */
package cc.unknown.module.impl.combat;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.LivingEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.player.StrafeEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.helpers.MathHelper;
import cc.unknown.utils.player.PlayerUtil;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

@Register(name="JumpReset", category=Category.Combat)
public class JumpReset
extends Module {
    private ModeValue mode = new ModeValue("Mode", "Legit", "Hit", "Tick", "Normal", "Legit");
    private BooleanValue onlyCombat = new BooleanValue("Enable only during combat", true);
    private SliderValue chance = new SliderValue("Chance", 100.0, 0.0, 100.0, 1.0);
    private DoubleSliderValue tickTicks = new DoubleSliderValue("Ticks", 0.0, 0.0, 0.0, 20.0, 1.0);
    private DoubleSliderValue hitHits = new DoubleSliderValue("Hits", 0.0, 0.0, 0.0, 20.0, 1.0);
    private int limit = 0;
    private boolean reset = false;

    public JumpReset() {
        this.registerSetting(this.mode, this.onlyCombat, this.chance, this.tickTicks, this.hitHits);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.mode.getMode() + "]");
    }

    @EventLink
    public void onLiving(LivingEvent e) {
        if (PlayerUtil.inGame() && (this.mode.is("Tick") || this.mode.is("Hit"))) {
            boolean inRange;
            double direction = Math.atan2(JumpReset.mc.field_71439_g.field_70159_w, JumpReset.mc.field_71439_g.field_70179_y);
            double degreePlayer = PlayerUtil.getDirection();
            double degreePacket = Math.floorMod((int)Math.toDegrees(direction), 360);
            double angle = Math.abs(degreePacket + degreePlayer);
            double threshold = 120.0;
            boolean bl = inRange = (angle = (double)Math.floorMod((int)angle, 360)) >= 180.0 - threshold / 2.0 && angle <= 180.0 + threshold / 2.0;
            if (inRange) {
                this.reset = true;
            }
        }
    }

    @EventLink
    public void onTick(TickEvent e) {
        if (this.mode.is("Normal") && !(JumpReset.mc.field_71462_r instanceof GuiScreen) && !JumpReset.mc.field_71439_g.func_70632_aY() && !JumpReset.mc.field_71439_g.func_71039_bw() && !this.checkLiquids() && JumpReset.mc.field_71439_g instanceof EntityPlayer && JumpReset.mc.field_71439_g.field_70122_E && JumpReset.mc.field_71439_g.field_70737_aN > 0 && JumpReset.mc.field_71439_g.field_70737_aN < 10 && JumpReset.mc.field_71439_g.field_70737_aN == JumpReset.mc.field_71439_g.field_70738_aO - 1 && (double)MathHelper.rand().nextFloat() <= this.chance.getInput() / 100.0) {
            JumpReset.mc.field_71439_g.func_70664_aZ();
        }
    }

    @EventLink
    public void onStrafe(StrafeEvent e) {
        if (PlayerUtil.inGame()) {
            if (this.checkLiquids() || !this.applyChance()) {
                return;
            }
            if (this.mode.is("Ticks") || this.mode.is("Hits") && this.reset) {
                if (!JumpReset.mc.field_71474_y.field_74314_A.field_74513_e && this.shouldJump() && JumpReset.mc.field_71439_g.func_70051_ag() && JumpReset.mc.field_71439_g.field_70737_aN == 9 || !this.onlyCombat.isToggled() && JumpReset.mc.field_71474_y.field_74312_F.func_151470_d() || JumpReset.mc.field_71439_g.field_70122_E) {
                    JumpReset.mc.field_71474_y.field_74314_A.field_74513_e = true;
                    this.limit = 0;
                }
                this.reset = false;
                return;
            }
            switch (this.mode.getMode()) {
                case "Ticks": {
                    ++this.limit;
                    break;
                }
                case "Hits": {
                    if (JumpReset.mc.field_71439_g.field_70737_aN != 9) break;
                    ++this.limit;
                }
            }
        }
    }

    private boolean shouldJump() {
        switch (this.mode.getMode()) {
            case "Ticks": {
                return this.limit >= MathHelper.randomInt(this.tickTicks.getInputMinToInt(), (double)this.tickTicks.getInputMaxToInt() + 0.1);
            }
            case "Hits": {
                return this.limit >= MathHelper.randomInt(this.hitHits.getInputMinToInt(), (double)this.hitHits.getInputMaxToInt() + 0.1);
            }
        }
        return false;
    }

    private boolean checkLiquids() {
        if (JumpReset.mc.field_71439_g == null || JumpReset.mc.field_71441_e == null) {
            return false;
        }
        Supplier[] supplierArray = new Supplier[4];
        supplierArray[0] = () -> ((EntityPlayerSP)JumpReset.mc.field_71439_g).func_180799_ab();
        supplierArray[1] = () -> ((EntityPlayerSP)JumpReset.mc.field_71439_g).func_70027_ad();
        supplierArray[2] = () -> ((EntityPlayerSP)JumpReset.mc.field_71439_g).func_70090_H();
        supplierArray[3] = () -> JumpReset.mc.field_71439_g.field_70134_J;
        return Stream.of(supplierArray).map(Supplier::get).anyMatch(Boolean.TRUE::equals);
    }

    private boolean applyChance() {
        Supplier<Boolean> chanceCheck = () -> this.chance.getInput() != 100.0 && Math.random() >= this.chance.getInput() / 100.0;
        return Stream.of(chanceCheck).map(Supplier::get).anyMatch(Boolean.TRUE::equals);
    }
}

