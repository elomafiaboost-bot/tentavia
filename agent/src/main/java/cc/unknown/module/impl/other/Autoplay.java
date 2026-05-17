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
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.play.server.S02PacketChat;

@Register(name="Autoplay", category=Category.Other)
public class Autoplay
extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Uni Bed", "Uni Bed", "Uni Sw", "Hyp Solo Insane", "Hyp Solo Normal");
    private final SliderValue delay = new SliderValue("Delay", 1500.0, 0.0, 4000.0, 50.0);
    private final Cold timer = new Cold(0L);
    private final AtomicReference<String> message = new AtomicReference<String>("");
    private final AtomicReference<String> command = new AtomicReference<String>("");

    public Autoplay() {
        this.registerSetting(this.mode, this.delay);
    }

    @Override
    public void onEnable() {
        this.message.set("");
        this.timer.reset();
    }

    @EventLink
    public void onUpdate(LivingEvent event) {
        String cmd;
        if (!this.message.get().isEmpty() && (double)this.timer.getTime() >= this.delay.getInput() && !(cmd = this.command.get()).isEmpty()) {
            Autoplay.mc.field_71439_g.func_71165_d(cmd);
            this.timer.reset();
            this.message.set("");
        }
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        String msg;
        if (e.isReceive() && e.getPacket() instanceof S02PacketChat && this.containsAny(msg = ((S02PacketChat)e.getPacket()).func_148915_c().func_150260_c(), "Jugar de nuevo", "ha ganado", "Want to play again?")) {
            this.message.set(msg);
            this.command.set(this.getCommand());
            this.timer.reset();
        }
    }

    private String getCommand() {
        if (this.mode.is("Uni Bed")) {
            return "/bedwars random";
        }
        if (this.mode.is("Uni Sw")) {
            return "/skywars random";
        }
        if (this.mode.is("Hyp Solo Insane")) {
            return "/play solo_insane";
        }
        if (this.mode.is("Hyp Solo Normal")) {
            return "/play solo_normal";
        }
        return "";
    }

    private boolean containsAny(String source, String ... targets) {
        for (String target : targets) {
            if (!source.contains(target)) continue;
            return true;
        }
        return false;
    }
}

