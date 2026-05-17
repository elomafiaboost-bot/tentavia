/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiChat
 *  net.minecraft.client.gui.inventory.GuiChest
 *  net.minecraft.client.gui.inventory.GuiInventory
 *  net.minecraft.client.settings.GameSettings
 *  net.minecraft.client.settings.KeyBinding
 */
package cc.unknown.module.impl.other;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

@Register(name="Inventory", category=Category.Other)
public class Inventory
extends Module {
    private BooleanValue sprint = new BooleanValue("Sprint", false);
    private final KeyBinding[] moveKeys;

    public Inventory() {
        this.moveKeys = new KeyBinding[]{Inventory.mc.field_71474_y.field_74351_w, Inventory.mc.field_71474_y.field_74368_y, Inventory.mc.field_71474_y.field_74366_z, Inventory.mc.field_71474_y.field_74370_x, Inventory.mc.field_71474_y.field_74314_A, Inventory.mc.field_71474_y.field_151444_V};
        this.registerSetting(this.sprint);
    }

    @EventLink
    public void onTick(TickEvent e) {
        if (Inventory.mc.field_71462_r != null && Inventory.mc.field_71462_r instanceof GuiChat) {
            return;
        }
        for (KeyBinding bind : this.moveKeys) {
            bind.field_74513_e = GameSettings.func_100015_a((KeyBinding)bind);
            if (!this.sprint.isToggled() || !PlayerUtil.isMoving() || !(Inventory.mc.field_71462_r instanceof GuiInventory) && !(Inventory.mc.field_71462_r instanceof GuiChest)) continue;
            Inventory.mc.field_71474_y.field_151444_V.field_74513_e = true;
            Inventory.mc.field_71439_g.func_70031_b(true);
        }
    }

    @Override
    public void onDisable() {
        if (Inventory.mc.field_71462_r != null) {
            for (KeyBinding bind : this.moveKeys) {
                if (!bind.field_74513_e) continue;
                bind.field_74513_e = false;
            }
        }
    }
}

