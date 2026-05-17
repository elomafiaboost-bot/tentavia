/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.command.commands;

import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.module.impl.api.Category;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Flips(name="Category", alias="cat", desc="Replace category name", syntax=".category <old name> <new name>")
public class CategoryCommand
extends Command {
    private Map<Category, String> originalNames = new HashMap<Category, String>();

    @Override
    public void onExecute(String[] args) {
        if (args.length == 0) {
            this.sendChat(this.getColor("Blue") + this.syntax, new Object[0]);
            return;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            this.resetCategory();
            return;
        }
        if (args.length != 2) {
            this.sendChat(this.getColor("Blue") + this.syntax, new Object[0]);
            return;
        }
        String oldName = args[0];
        String newName = args[1];
        AtomicBoolean replaced = new AtomicBoolean(false);
        for (Category cat : Category.values()) {
            if (!cat.getName().equalsIgnoreCase(oldName)) continue;
            this.originalNames.put(cat, cat.getName());
            cat.setName(newName);
            replaced.set(true);
            break;
        }
        if (replaced.get()) {
            this.sendChat("Category " + args[0] + " replaced to " + args[1], new Object[0]);
        }
    }

    private void resetCategory() {
        for (Map.Entry<Category, String> entry : this.originalNames.entrySet()) {
            Category cat = entry.getKey();
            String originalName = entry.getValue();
            cat.setName(originalName);
        }
        this.originalNames.clear();
    }
}

