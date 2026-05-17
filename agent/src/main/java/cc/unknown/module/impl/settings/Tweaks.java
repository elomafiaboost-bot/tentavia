/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.play.client.C15PacketClientSettings
 */
package cc.unknown.module.impl.settings;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.network.play.client.C15PacketClientSettings;

@Register(name="Tweaks", category=Category.Settings)
public class Tweaks
extends Module {
    private BooleanValue noClickDelay = new BooleanValue("No Click Delay", true);
    private BooleanValue noJumpDelay = new BooleanValue("No Jump Delay", true);
    public BooleanValue noScoreboard = new BooleanValue("No Scoreboard", false);
    public BooleanValue noHurtCam = new BooleanValue("No Hurt Cam", true);
    private BooleanValue cancelC15 = new BooleanValue("Cancel C15", true);
    public BooleanValue rots = new BooleanValue("Rotations", true);

    public Tweaks() {
        this.registerSetting(this.noClickDelay, this.noJumpDelay, this.noScoreboard, this.noHurtCam, this.cancelC15, this.rots);
    }

    @EventLink
    public void onClick(TickEvent e) {
        if (this.noClickDelay.isToggled()) {
            Tweaks.mc.field_71429_W = 0;
        }
    }

    @EventLink
    public void onCancelPacket(PacketEvent e) {
        if (this.cancelC15.isToggled() && PlayerUtil.inGame() && e.isSend() && e.getPacket() instanceof C15PacketClientSettings) {
            e.setCancelled(true);
        }
    }

    @EventLink
    public void onJump(TickEvent e) {
        if (this.noJumpDelay.isToggled()) {
            Tweaks.mc.field_71439_g.field_70773_bE = 0;
        }
    }
}

