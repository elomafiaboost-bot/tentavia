/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.play.client.C02PacketUseEntity
 *  net.minecraft.network.play.client.C02PacketUseEntity$Action
 */
package cc.unknown.module.impl.combat;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.network.play.client.C02PacketUseEntity;

@Register(name="WTap", category=Category.Combat)
public class WTap
extends Module {
    private ModeValue mode = new ModeValue("Mode", "Pre", "Pre", "Post");
    private SliderValue chance = new SliderValue("Tap Chance", 100.0, 0.0, 100.0, 1.0);
    private boolean unsprint;
    private boolean tap;

    public WTap() {
        this.registerSetting(this.mode, this.chance);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.mode.getMode() + "]");
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        C02PacketUseEntity wrapper;
        if (e.isSend() && e.getPacket() instanceof C02PacketUseEntity && (wrapper = (C02PacketUseEntity)e.getPacket()).func_149565_c() == C02PacketUseEntity.Action.ATTACK) {
            boolean bl = this.tap = Math.random() * 100.0 < this.chance.getInput();
            if (!this.tap) {
                return;
            }
            if (this.mode.is("Pre") && (WTap.mc.field_71439_g.func_70051_ag() || WTap.mc.field_71474_y.field_151444_V.func_151470_d())) {
                WTap.mc.field_71474_y.field_151444_V.field_74513_e = true;
                this.unsprint = true;
            }
            if (this.mode.is("Post") && (WTap.mc.field_71439_g.func_70051_ag() || WTap.mc.field_71474_y.field_151444_V.func_151470_d())) {
                WTap.mc.field_71474_y.field_151444_V.field_74513_e = false;
                this.unsprint = false;
            }
        }
    }

    @EventLink
    public void onMotion(MotionEvent e) {
        if (!PlayerUtil.inGame()) {
            return;
        }
        if (e.isPre() && this.mode.is("Pre")) {
            if (!this.tap) {
                return;
            }
            if (this.unsprint) {
                WTap.mc.field_71474_y.field_151444_V.field_74513_e = false;
                this.unsprint = false;
            }
        }
        if (e.isPost() && this.mode.is("Post")) {
            if (!this.tap) {
                return;
            }
            if (this.unsprint) {
                WTap.mc.field_71474_y.field_151444_V.field_74513_e = true;
                this.unsprint = true;
            }
        }
    }
}

