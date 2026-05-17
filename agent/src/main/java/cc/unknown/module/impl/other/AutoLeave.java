/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.play.server.S02PacketChat
 */
package cc.unknown.module.impl.other;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.LivingEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.client.Cold;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.play.server.S02PacketChat;

@Register(name="AutoLeave", category=Category.Other)
public class AutoLeave
extends Module {
    private ModeValue mode = new ModeValue("Mode", "/salir", "/salir");
    private final SliderValue delay = new SliderValue("Delay", 0.0, 0.0, 4000.0, 50.0);
    private final AtomicBoolean waiting = new AtomicBoolean(false);
    private final Cold timer = new Cold(0L);

    public AutoLeave() {
        this.registerSetting(this.mode, this.delay);
    }

    @Override
    public void onEnable() {
        this.timer.reset();
    }

    @EventLink
    public void onTick(LivingEvent event) {
        if (this.waiting.get() && (double)this.timer.getTime() >= this.delay.getInput()) {
            AutoLeave.mc.field_71439_g.func_71165_d(this.mode.getMode());
            this.timer.reset();
            this.waiting.set(false);
        }
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        S02PacketChat packet;
        String message;
        if (e.isReceive() && e.getPacket() instanceof S02PacketChat && (message = (packet = (S02PacketChat)e.getPacket()).func_148915_c().func_150260_c()).contains("Deseas salirte de la arena")) {
            this.waiting.set(true);
            this.timer.reset();
        }
    }
}

