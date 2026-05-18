/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C02PacketUseEntity
 *  net.minecraft.network.play.client.C02PacketUseEntity$Action
 *  net.minecraft.network.play.client.C03PacketPlayer
 *  net.minecraft.util.Vec3
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.network.DisconnectionEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.event.impl.world.WorldEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import cc.unknown.utils.network.PacketUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

@Register(name="Blink", category=Category.Player)
public class Blink
extends Module {
    private final List<Packet<?>> packets = new ArrayList();
    private final List<Packet<?>> packetsReceived = new ArrayList();
    private final List<Packet<?>> queuedPackets = new ArrayList();
    private final List<Vec3> positions = new ArrayList<Vec3>();
    private BooleanValue renderPosition = new BooleanValue("Render actual position", true);
    private BooleanValue disableDisconnect = new BooleanValue("Disable on disconnect", true);
    private BooleanValue disableAttack = new BooleanValue("Disable when attacking", true);

    public Blink() {
        this.registerSetting(this.renderPosition, this.disableDisconnect, this.disableAttack);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (Blink.mc.field_71439_g == null) {
            this.toggle();
            return;
        }
        this.packets.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (Blink.mc.field_71439_g == null) {
            return;
        }
        this.blink();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventLink
    public void onPacket(PacketEvent e) {
        List<Packet<?>> list;
        Packet<?> p = e.getPacket();
        if (Blink.mc.field_71439_g == null || Blink.mc.field_71439_g.field_70128_L) {
            return;
        }
        if (p.getClass().getSimpleName().startsWith("S") || p.getClass().getSimpleName().startsWith("C00") || p.getClass().getSimpleName().startsWith("C01")) {
            return;
        }
        if (e.isReceive()) {
            list = this.packetsReceived;
            synchronized (list) {
                this.queuedPackets.addAll(this.packetsReceived);
            }
            this.packetsReceived.clear();
        }
        if (e.isSend()) {
            C02PacketUseEntity wrapper;
            e.setCancelled(true);
            list = this.packets;
            synchronized (list) {
                this.packets.add(p);
            }
            if (p instanceof C03PacketPlayer && ((C03PacketPlayer)p).func_149466_j()) {
                C03PacketPlayer c03 = (C03PacketPlayer)p;
                Vec3 packetPos = new Vec3(c03.field_149479_a, c03.field_149477_b, c03.field_149478_c);
                List<Vec3> list2 = this.positions;
                synchronized (list2) {
                    this.positions.add(packetPos);
                }
            }
            if (p instanceof C02PacketUseEntity) {
                wrapper = (C02PacketUseEntity)p;
                if (this.disableAttack.isToggled() && wrapper.func_149565_c() == C02PacketUseEntity.Action.ATTACK) {
                    this.blink();
                }
                return;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventLink
    public void onPost(MotionEvent e) {
        if (e.isPost()) {
            if (Blink.mc.field_71439_g == null || Blink.mc.field_71439_g.field_70128_L || Blink.mc.field_71439_g.field_70173_aa <= 10) {
                this.blink();
            }
            List<Packet<?>> list = this.packetsReceived;
            synchronized (list) {
                this.queuedPackets.addAll(this.packetsReceived);
            }
            this.packetsReceived.clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventLink
    public void onRender3D(RenderEvent e) {
        if (e.is3D() && this.renderPosition.isToggled()) {
            List<Vec3> list = this.positions;
            synchronized (list) {
                GL11.glPushMatrix();
                GL11.glDisable((int)3553);
                GL11.glBlendFunc((int)770, (int)771);
                GL11.glEnable((int)2848);
                GL11.glEnable((int)3042);
                GL11.glDisable((int)2929);
                Blink.mc.field_71460_t.func_175072_h();
                GL11.glBegin((int)3);
                GL11.glColor4f((float)((float)Theme.instance.getMainColor().getRed() / 255.0f), (float)((float)Theme.instance.getMainColor().getGreen() / 255.0f), (float)((float)Theme.instance.getMainColor().getBlue() / 255.0f), (float)((float)Theme.instance.getMainColor().getAlpha() / 255.0f));
                double renderPosX = Blink.mc.func_175598_ae().field_78730_l;
                double renderPosY = Blink.mc.func_175598_ae().field_78731_m;
                double renderPosZ = Blink.mc.func_175598_ae().field_78728_n;
                for (Vec3 pos : this.positions) {
                    GL11.glVertex3d((double)(pos.field_72450_a - renderPosX), (double)(pos.field_72448_b - renderPosY), (double)(pos.field_72449_c - renderPosZ));
                }
                GL11.glColor4d((double)1.0, (double)1.0, (double)1.0, (double)1.0);
                GL11.glEnd();
                GL11.glEnable((int)2929);
                GL11.glDisable((int)2848);
                GL11.glDisable((int)3042);
                GL11.glEnable((int)3553);
                GL11.glPopMatrix();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void blink() {
        List<Packet<?>> list = this.packetsReceived;
        synchronized (list) {
            this.queuedPackets.addAll(this.packetsReceived);
        }
        list = this.packets;
        synchronized (list) {
            PacketUtil.send(this.packets.toArray(new Packet[0]));
        }
        this.reset();
    }

    private void reset() {
        this.packets.clear();
        this.packetsReceived.clear();
        this.positions.clear();
    }

    @EventLink
    public void onWorldLoad(WorldEvent e) {
        if (e.getWorldClient() == null) {
            this.reset();
        }
    }

    @EventLink
    public void onDisconnect(DisconnectionEvent e) {
        this.packets.clear();
        if (this.disableDisconnect.isToggled()) {
            this.disable();
        }
    }
}

