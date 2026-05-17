/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 */
package cc.unknown.command.commands;

import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@Flips(name="Spy", alias="spy", desc="Spying...", syntax=".spy <user>")
public class SpyCommand
extends Command {
    @Override
    public void onExecute(String[] args) {
        if (args.length < 2) {
            if (mc.func_175606_aa() != SpyCommand.mc.field_71439_g) {
                mc.func_175607_a((Entity)SpyCommand.mc.field_71439_g);
                return;
            }
            this.sendChat(this.getColor("Red") + " Syntax Error. Use: " + this.syntax, new Object[0]);
            return;
        }
        String target = args[1];
        for (EntityPlayer entity : SpyCommand.mc.field_71441_e.field_73010_i) {
            if (!target.equals(entity.func_70005_c_())) continue;
            mc.func_175607_a((Entity)entity);
            this.sendChat("Spying to \u00a78${entity.name}\u00a73.", new Object[0]);
            this.sendChat("Execute \u00a78.spy \u00a73again to go back to yours.", new Object[0]);
            break;
        }
    }
}

