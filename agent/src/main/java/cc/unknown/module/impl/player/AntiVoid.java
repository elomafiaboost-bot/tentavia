/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemEnderPearl
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C03PacketPlayer
 *  net.minecraft.network.play.client.C03PacketPlayer$C05PacketPlayerLook
 *  net.minecraft.network.play.client.C03PacketPlayer$C06PacketPlayerPosLook
 *  net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
 *  net.minecraft.network.play.server.S08PacketPlayerPosLook
 *  net.minecraft.util.Vec3
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.network.DisconnectionEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.Vec3;

@Register(name="AntiVoid", category=Category.Player)
public class AntiVoid
extends Module {
    private int overVoidTicks;
    private Vec3 position;
    private Vec3 motion;
    private boolean wasVoid;
    private boolean setBack;
    boolean shouldStuck;
    double x;
    double y;
    double z;
    boolean wait;
    private ModeValue mode = new ModeValue("Mode", "Universocraft", "Universocraft");
    private SliderValue fall = new SliderValue("Min fall distance", 5.0, 0.0, 10.0, 1.0);

    public AntiVoid() {
        this.registerSetting(this.mode, this.fall);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.mode.getMode() + "]");
    }

    @Override
    public void onDisable() {
        AntiVoid.mc.field_71428_T.field_74278_d = 1.0f;
        AntiVoid.mc.field_71439_g.field_70128_L = false;
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        Packet<?> p = e.getPacket();
        if (this.mode.is("Universocraft")) {
            if (e.isSend()) {
                if (!AntiVoid.mc.field_71439_g.field_70122_E && this.shouldStuck && p instanceof C03PacketPlayer && !(p instanceof C03PacketPlayer.C05PacketPlayerLook) && !(p instanceof C03PacketPlayer.C06PacketPlayerPosLook)) {
                    e.setCancelled(true);
                }
                if (p instanceof C08PacketPlayerBlockPlacement && this.wait) {
                    this.shouldStuck = false;
                    AntiVoid.mc.field_71428_T.field_74278_d = 0.2f;
                    this.wait = false;
                }
            }
            if (e.isReceive() && p instanceof S08PacketPlayerPosLook) {
                S08PacketPlayerPosLook wrapper = (S08PacketPlayerPosLook)p;
                this.x = wrapper.func_148932_c();
                this.y = wrapper.func_148928_d();
                this.z = wrapper.func_148933_e();
                AntiVoid.mc.field_71428_T.field_74278_d = 0.2f;
            }
        }
    }

    @EventLink
    public void onUpdate(MotionEvent e) {
        try {
            if (e.isPre() && this.mode.is("Universocraft")) {
                boolean overVoid;
                if (AntiVoid.mc.field_71439_g.func_70694_bm() == null) {
                    AntiVoid.mc.field_71428_T.field_74278_d = 1.0f;
                }
                if (AntiVoid.mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemEnderPearl) {
                    this.wait = true;
                }
                if (this.shouldStuck && !AntiVoid.mc.field_71439_g.field_70122_E) {
                    AntiVoid.mc.field_71439_g.field_70159_w = 0.0;
                    AntiVoid.mc.field_71439_g.field_70181_x = 0.0;
                    AntiVoid.mc.field_71439_g.field_70179_y = 0.0;
                    AntiVoid.mc.field_71439_g.func_70080_a(this.x, this.y, this.z, AntiVoid.mc.field_71439_g.field_70177_z, AntiVoid.mc.field_71439_g.field_70125_A);
                }
                boolean bl = overVoid = !AntiVoid.mc.field_71439_g.field_70122_E && !PlayerUtil.isBlockUnder(30);
                if (!overVoid) {
                    this.shouldStuck = false;
                    this.x = AntiVoid.mc.field_71439_g.field_70165_t;
                    this.y = AntiVoid.mc.field_71439_g.field_70163_u;
                    this.z = AntiVoid.mc.field_71439_g.field_70161_v;
                    AntiVoid.mc.field_71428_T.field_74278_d = 1.0f;
                }
                if (overVoid) {
                    ++this.overVoidTicks;
                } else if (AntiVoid.mc.field_71439_g.field_70122_E) {
                    this.overVoidTicks = 0;
                }
                if (overVoid && this.position != null && this.motion != null && (double)this.overVoidTicks < 30.0 + this.fall.getInput() * 20.0) {
                    if (!this.setBack) {
                        this.wasVoid = true;
                        if ((double)AntiVoid.mc.field_71439_g.field_70143_R > this.fall.getInput() || this.setBack) {
                            AntiVoid.mc.field_71439_g.field_70143_R = 0.0f;
                            this.setBack = true;
                            this.shouldStuck = true;
                            this.x = AntiVoid.mc.field_71439_g.field_70165_t;
                            this.y = AntiVoid.mc.field_71439_g.field_70163_u;
                            this.z = AntiVoid.mc.field_71439_g.field_70161_v;
                        }
                    }
                } else {
                    if (this.shouldStuck) {
                        this.toggle();
                    }
                    this.shouldStuck = false;
                    AntiVoid.mc.field_71428_T.field_74278_d = 1.0f;
                    this.setBack = false;
                    if (this.wasVoid) {
                        this.wasVoid = false;
                    }
                    this.motion = new Vec3(AntiVoid.mc.field_71439_g.field_70159_w, AntiVoid.mc.field_71439_g.field_70181_x, AntiVoid.mc.field_71439_g.field_70179_y);
                    this.position = new Vec3(AntiVoid.mc.field_71439_g.field_70165_t, AntiVoid.mc.field_71439_g.field_70163_u, AntiVoid.mc.field_71439_g.field_70161_v);
                }
            }
        }
        catch (NullPointerException nullPointerException) {
            // empty catch block
        }
    }

    @EventLink
    public void onDisconnect(DisconnectionEvent e) {
        this.disable();
    }
}

