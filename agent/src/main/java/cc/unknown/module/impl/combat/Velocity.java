/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.client.multiplayer.WorldClient
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C03PacketPlayer
 *  net.minecraft.network.play.client.C07PacketPlayerDigging
 *  net.minecraft.network.play.client.C07PacketPlayerDigging$Action
 *  net.minecraft.network.play.server.S12PacketEntityVelocity
 *  net.minecraft.network.play.server.S27PacketExplosion
 *  net.minecraft.util.BlockPos
 *  net.minecraft.util.EnumFacing
 */
package cc.unknown.module.impl.combat;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.helpers.MathHelper;
import cc.unknown.utils.network.PacketUtil;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@Register(name="Velocity", category=Category.Combat)
public class Velocity
extends Module {
    private ModeValue mode = new ModeValue("Mode", "S12Packet", "S12Packet", "Verus", "Ground Grim", "Polar");
    public SliderValue horizontal = new SliderValue("Horizontal", 90.0, -100.0, 100.0, 1.0);
    public SliderValue vertical = new SliderValue("Vertical", 100.0, -100.0, 100.0, 1.0);
    public SliderValue chance = new SliderValue("Chance", 100.0, 0.0, 100.0, 1.0);
    private BooleanValue onlyCombat = new BooleanValue("Only During Combat", false);
    private BooleanValue onlyGround = new BooleanValue("Only While on Ground", false);
    private boolean reset;
    private int timerTicks = 0;

    public Velocity() {
        this.registerSetting(this.mode, this.horizontal, this.vertical, this.chance, this.onlyCombat, this.onlyGround);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.mode.getMode() + "]");
    }

    @Override
    public void onDisable() {
        Velocity.mc.field_71428_T.field_74278_d = 1.0f;
        this.timerTicks = 0;
        this.reset = false;
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        if ((double)(this.chance.getInputToInt() / 100) > Math.random()) {
            return;
        }
        Packet<?> p = e.getPacket();
        if (e.isReceive()) {
            S12PacketEntityVelocity wrapper;
            if (this.mode.is("S12Packet")) {
                if (p instanceof S12PacketEntityVelocity) {
                    wrapper = (S12PacketEntityVelocity)p;
                    if (wrapper.func_149412_c() == Velocity.mc.field_71439_g.func_145782_y()) {
                        if (this.horizontal.getInput() == 0.0) {
                            e.setCancelled(true);
                            if (this.vertical.getInput() != 0.0) {
                                Velocity.mc.field_71439_g.field_70181_x = (double)wrapper.func_149410_e() / 8000.0;
                            }
                            return;
                        }
                        wrapper.field_149415_b = (int)((double)wrapper.field_149415_b * (this.horizontal.getInput() / 100.0));
                        wrapper.field_149416_c = (int)((double)wrapper.field_149416_c * (this.vertical.getInput() / 100.0));
                        wrapper.field_149414_d = (int)((double)wrapper.field_149414_d * (this.horizontal.getInput() / 100.0));
                        e.setPacket((Packet<?>)wrapper);
                    }
                } else if (p instanceof S27PacketExplosion) {
                    S27PacketExplosion s27 = (S27PacketExplosion)p;
                    if (this.horizontal.getInput() != 0.0 && this.vertical.getInput() != 0.0) {
                        s27.field_149152_f = 0.0f;
                        s27.field_149153_g = 0.0f;
                        s27.field_149159_h = 0.0f;
                        return;
                    }
                    s27.field_149152_f = (float)((double)s27.field_149152_f * this.horizontal.getInput());
                    s27.field_149153_g = (float)((double)s27.field_149153_g * this.vertical.getInput());
                    s27.field_149159_h = (float)((double)s27.field_149159_h * this.horizontal.getInput());
                }
            }
            if (this.mode.is("Ground Grim") && PlayerUtil.isMoving() && Velocity.mc.field_71439_g.field_70122_E) {
                if (p instanceof S12PacketEntityVelocity) {
                    wrapper = (S12PacketEntityVelocity)p;
                    if (wrapper.func_149412_c() == Velocity.mc.field_71439_g.func_145782_y()) {
                        e.setCancelled(true);
                        this.reset = true;
                    }
                } else if (p instanceof S27PacketExplosion) {
                    e.setCancelled(true);
                    this.reset = true;
                }
            }
            if (this.mode.is("Polar") && p instanceof S12PacketEntityVelocity) {
                wrapper = (S12PacketEntityVelocity)p;
                if (PlayerUtil.isMoving() && wrapper.func_149412_c() == Velocity.mc.field_71439_g.func_145782_y() && wrapper.field_149416_c > 0 && (Velocity.mc.field_71439_g.field_70737_aN <= 14 || Velocity.mc.field_71439_g.field_70737_aN <= 1)) {
                    this.reset = true;
                }
            }
        }
    }

    @EventLink
    public void onTick(TickEvent e) {
        if (this.mode.is("Ground Grim")) {
            BlockPos pos;
            if (this.timerTicks > 0 && Velocity.mc.field_71428_T.field_74278_d <= 1.0f) {
                float timerSpeed = 0.8f + 0.2f * (float)(20 - this.timerTicks) / 20.0f;
                Velocity.mc.field_71428_T.field_74278_d = Math.min(timerSpeed, 1.0f);
                --this.timerTicks;
            } else if (Velocity.mc.field_71428_T.field_74278_d <= 1.0f) {
                Velocity.mc.field_71428_T.field_74278_d = 1.0f;
            }
            if (this.reset && this.checkAir(pos = new BlockPos(Velocity.mc.field_71439_g.field_70165_t, Velocity.mc.field_71439_g.field_70163_u, Velocity.mc.field_71439_g.field_70161_v))) {
                this.reset = false;
            }
        }
    }

    @EventLink
    public void onPre(MotionEvent e) {
        if (PlayerUtil.inGame() && e.isPre() && this.mode.is("Verus") && Velocity.mc.field_71439_g.field_70737_aN == 10 - MathHelper.randomInt(3.0, 4.0)) {
            Velocity.mc.field_71439_g.field_70159_w = 0.0;
            Velocity.mc.field_71439_g.field_70181_x = 0.0;
            Velocity.mc.field_71439_g.field_70179_y = 0.0;
        }
    }

    private boolean checkAir(BlockPos blockPos) {
        WorldClient world = Velocity.mc.field_71441_e;
        if (world == null) {
            return false;
        }
        if (!world.func_175623_d(blockPos)) {
            return false;
        }
        this.timerTicks = 20;
        EntityPlayerSP player = Velocity.mc.field_71439_g;
        if (player != null) {
            PacketUtil.sendPacketNoEvent(new C03PacketPlayer(true));
            PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));
        }
        world.func_175698_g(blockPos);
        return true;
    }
}

