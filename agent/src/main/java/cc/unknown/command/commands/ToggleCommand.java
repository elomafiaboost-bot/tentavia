/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.EnumChatFormatting
 */
package cc.unknown.command.commands;

import cc.unknown.Haru;
import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.module.impl.Module;
import net.minecraft.util.EnumChatFormatting;

@Flips(name="Toggle", alias="t", desc="Toggles modules.", syntax=".toggle")
public class ToggleCommand
extends Command {
    @Override
    public void onExecute(String[] args) {
        if (args.length != 1) {
            this.sendChat(this.getColor("Gray") + " " + this.getAll(), new Object[0]);
        } else {
            String module = args[0];
            Module mod = Haru.instance.getModuleManager().getModule(module);
            if (mod == null) {
                this.sendChat(this.getColor("Red") + " Module not found!", new Object[0]);
            } else {
                Haru.instance.getModuleManager().getModule(module).toggle();
                this.sendChat(this.getColor("White") + " %s " + this.getColor("Gray") + "%s", Haru.instance.getModuleManager().getModule(module).getRegister().name(), Haru.instance.getModuleManager().getModule(module).isEnabled() ? EnumChatFormatting.GREEN + "enabled" : this.getColor("Red") + "disabled.");
                Haru.instance.getConfigManager().save();
            }
        }
    }

    public String getAll() {
        return this.syntax + " - " + this.desc;
    }
}

