/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.INetHandlerPlayServer
 *  net.minecraft.network.play.client.C02PacketUseEntity
 *  net.minecraft.network.play.client.C02PacketUseEntity$Action
 *  net.minecraft.network.play.client.C0APacketAnimation
 *  net.minecraft.network.play.server.S08PacketPlayerPosLook
 *  net.minecraft.world.World
 */
package cc.unknown.module.impl.combat;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.DisconnectionEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.client.Cold;
import cc.unknown.utils.network.PacketUtil;
import cc.unknown.utils.player.PlayerUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.world.World;

@Register(name="Criticals", category=Category.Combat)
public class Criticals
extends Module {
    private BooleanValue aggressive = new BooleanValue("Aggressive", true);
    private SliderValue packetSendingRate = new SliderValue("Packet Sending Rate", 500.0, 250.0, 1000.0, 1.0);
    private SliderValue criticalHitChance = new SliderValue("Critical Hit Chance (%)", 100.0, 0.0, 100.0, 1.0);
    private boolean onAir;
    private boolean hitGround;
    private List<Packet<?>> packets = new ArrayList<Packet<?>>();
    private List<Packet<?>> attackPackets = new ArrayList<Packet<?>>();
    private Cold timer = new Cold();

    public Criticals() {
        this.registerSetting(this.aggressive, this.packetSendingRate, this.criticalHitChance);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.packetSendingRate.getInputToInt() + " ms]");
    }

    @Override
    public void onEnable() {
        this.onAir = false;
        this.hitGround = false;
    }

    @Override
    public void onDisable() {
        this.releasePackets();
    }

    @EventLink
    public void onSend(PacketEvent e) {
        if (e.isSend()) {
            if (Criticals.mc.field_71439_g.field_70122_E) {
                this.hitGround = true;
            }
            if (!this.timer.reached(this.packetSendingRate.getInputToLong()) && this.onAir) {
                e.setCancelled(true);
                if (e.getPacket() instanceof C02PacketUseEntity && e.getPacket() instanceof C0APacketAnimation) {
                    if (this.aggressive.isToggled()) {
                        e.setCancelled(false);
                    } else {
                        this.attackPackets.add(e.getPacket());
                    }
                } else {
                    this.packets.add(e.getPacket());
                }
            }
            if (this.timer.reached(this.packetSendingRate.getInputToLong()) && this.onAir) {
                this.onAir = false;
                this.releasePackets();
            }
            if (e.getPacket() instanceof C02PacketUseEntity) {
                C02PacketUseEntity wrapper = (C02PacketUseEntity)e.getPacket();
                Entity entity = wrapper.func_149564_a((World)Criticals.mc.field_71441_e);
                if (entity == null) {
                    return;
                }
                if (wrapper.func_149565_c() == C02PacketUseEntity.Action.ATTACK) {
                    if (!Criticals.mc.field_71439_g.field_70122_E) {
                        if (!this.onAir && this.hitGround && Criticals.mc.field_71439_g.field_70143_R <= 1.0f && (double)(this.criticalHitChance.getInputToInt() / 100) > Math.random()) {
                            this.timer.reset();
                            this.onAir = true;
                            this.hitGround = false;
                        }
                        return;
                    }
                    if (this.onAir) {
                        Criticals.mc.field_71439_g.func_71009_b(entity);
                        PlayerUtil.send("Crit", new Object[0]);
                    }
                }
            }
        }
        if (e.isReceive()) {
            if (Criticals.mc.field_71439_g == null) {
                this.hitGround = true;
            }
            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                this.hitGround = true;
            }
        }
    }

    @EventLink
    public void onDisconnect(DisconnectionEvent e) {
        this.disable();
    }

    private void releasePackets() {
        if (PlayerUtil.inGame()) {
            if (!this.attackPackets.isEmpty()) {
                this.attackPackets.forEach(PacketUtil::sendPacketNoEvent);
            }
            if (!this.packets.isEmpty()) {
                this.packets.forEach(PacketUtil::sendPacketNoEvent);
            }
        }
        this.packets.clear();
        this.attackPackets.clear();
        this.timer.reset();
    }
}

