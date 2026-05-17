/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.command.commands;

import cc.unknown.Haru;
import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.config.Config;

@Flips(name="Config", alias="cfg", desc="Save or load ur config", syntax=".cfg save <name>")
public class ConfigCommand
extends Command {
    @Override
    public void onExecute(String[] args) {
        if (Haru.instance.getClientConfig() != null) {
            Haru.instance.getClientConfig().saveConfig();
            Haru.instance.getConfigManager().save();
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                this.listConfigs();
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("load")) {
                boolean found = false;
                for (Config config : Haru.instance.getConfigManager().getConfigs()) {
                    if (!config.getName().equalsIgnoreCase(args[1])) continue;
                    found = true;
                    Haru.instance.getConfigManager().setConfig(config);
                    this.sendChat(this.getColor("Gray") + " Loaded config!", new Object[0]);
                    break;
                }
                if (!found) {
                    this.sendChat(this.getColor("Red") + " Unable to find a config with the name " + args[1], new Object[0]);
                }
            } else if (args[0].equalsIgnoreCase("save")) {
                Haru.instance.getConfigManager().copyConfig(Haru.instance.getConfigManager().getConfig(), args[1] + ".haru");
                this.sendChat(this.getColor("Gray") + " Saved as " + args[1] + "!", new Object[0]);
                Haru.instance.getConfigManager().discoverConfigs();
            } else if (args[0].equalsIgnoreCase("remove")) {
                boolean found = false;
                for (Config config : Haru.instance.getConfigManager().getConfigs()) {
                    if (!config.getName().equalsIgnoreCase(args[1])) continue;
                    Haru.instance.getConfigManager().deleteConfig(config);
                    found = true;
                    this.sendChat(this.getColor("Gray") + " Removed " + args[1] + " successfully!", new Object[0]);
                    break;
                }
                if (!found) {
                    this.sendChat(this.getColor("Red") + " Failed to delete " + args[1], new Object[0]);
                }
            } else {
                this.sendChat(this.getColor("Red") + " Syntax Error.", new Object[0]);
                this.sendChat(this.getColor("Red") + " Use: .config remove <config name>", new Object[0]);
            }
        }
    }

    private void listConfigs() {
        this.sendChat(this.getColor("Green") + " Available configs: ", new Object[0]);
        for (Config config : Haru.instance.getConfigManager().getConfigs()) {
            this.sendChat(" " + this.getColor("Gray") + config.getName(), new Object[0]);
        }
    }
}

