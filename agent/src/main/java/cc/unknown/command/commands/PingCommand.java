/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 */
package cc.unknown.command.commands;

import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.utils.player.CombatUtil;
import net.minecraft.entity.player.EntityPlayer;

@Flips(name="Ping", alias="pong", desc="Show ur ping", syntax=".ping")
public class PingCommand
extends Command {
    @Override
    public void onExecute(String[] args) {
        if (args.length == 0) {
            int ping = CombatUtil.instance.getPing((EntityPlayer)PingCommand.mc.field_71439_g);
            String color = ping >= 0 && ping <= 99 ? this.getColor("Green") : (ping >= 100 && ping <= 199 ? this.getColor("Yellow") : this.getColor("Red"));
            this.sendChat(this.getColor("White") + " Your ping: " + color + ping + "ms", new Object[0]);
        }
    }
}

