/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.play.server.S03PacketTimeUpdate
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.SliderValue;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

@Register(name="Ambience", category=Category.Visuals)
public class Ambience
extends Module {
    private SliderValue time = new SliderValue("Time", 18000.0, 0.0, 24000.0, 500.0);

    public Ambience() {
        this.registerSetting(this.time);
    }

    @EventLink
    public void onRender3D(RenderEvent e) {
        if (e.is3D()) {
            Ambience.mc.field_71441_e.func_72877_b(this.time.getInputToLong());
        }
    }

    @EventLink
    public void onReceive(PacketEvent e) {
        if (e.isReceive() && e.getPacket() instanceof S03PacketTimeUpdate) {
            e.setCancelled(true);
        }
    }
}

