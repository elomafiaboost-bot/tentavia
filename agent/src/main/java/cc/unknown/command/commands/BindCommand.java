/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.command.commands;

import cc.unknown.Haru;
import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.module.impl.Module;
import cc.unknown.utils.misc.KeybindUtil;

@Flips(name="Bind", alias="b", desc="Sets binds for modules.", syntax=".bind <module> <key>")
public class BindCommand
extends Command {
    @Override
    public void onExecute(String[] args) {
        Module mod;
        if (args.length != 2) {
            this.sendChat(this.getColor("Red") + "Syntax Error.", new Object[0]);
            return;
        }
        String key = args[0];
        String value = args[1];
        if (key.equals(Haru.instance.getCommandManager().getPrefix())) {
            key = "none";
        }
        if ((mod = Haru.instance.getModuleManager().getModule(key)) != null) {
            KeybindUtil.instance.bind(mod, KeybindUtil.instance.toInt(value));
            this.sendChat(String.format("Bound %s to %s!", mod.getRegister().name(), value), new Object[0]);
            Haru.instance.getConfigManager().save();
        } else {
            this.sendChat(this.getColor("Red") + "Key or module \u00a7cwas not found!", value);
        }
    }
}

