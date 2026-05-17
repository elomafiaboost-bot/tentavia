/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.inventory.GuiInventory
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemPotion
 *  net.minecraft.item.ItemStack
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C16PacketClientStatus
 *  net.minecraft.network.play.client.C16PacketClientStatus$EnumState
 *  net.minecraft.potion.Potion
 */
package cc.unknown.module.impl.combat;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;

@Register(name="AutoRefill", category=Category.Combat)
public class AutoRefill
extends Module {
    private DoubleSliderValue delay = new DoubleSliderValue("Delay", 0.0, 0.0, 0.0, 500.0, 1.0);
    private BooleanValue pots = new BooleanValue("Pots", true);
    private BooleanValue soup = new BooleanValue("Soup", true);
    private int lastShiftedPotIndex = -1;
    private long lastUsageTime = 0L;
    private long delay1 = 800L;
    private boolean refillOpened = false;

    public AutoRefill() {
        this.registerSetting(this.delay, this.pots, this.soup);
    }

    @Override
    public void onEnable() {
        if (PlayerUtil.inGame() && AutoRefill.mc.field_71462_r == null) {
            this.refillOpened = true;
            this.newDelay();
            this.openInventory();
            if (this.isHotbarFull()) {
                this.closeInventory();
            }
        }
    }

    @EventLink
    public void onPost(MotionEvent e) {
        if (e.isPost()) {
            long currentTime = System.currentTimeMillis();
            if (AutoRefill.mc.field_71462_r instanceof GuiInventory && !this.isHotbarFull()) {
                if (this.refillOpened && currentTime - this.lastUsageTime >= this.delay1) {
                    this.refillHotbar();
                    this.lastUsageTime = currentTime;
                }
            } else if (AutoRefill.mc.field_71462_r == null && this.isEnabled()) {
                this.disable();
            }
        }
    }

    private boolean isHotbarFull() {
        for (int i = 36; i < 45; ++i) {
            if (AutoRefill.mc.field_71439_g.field_71069_bz.func_75139_a(i).func_75216_d()) continue;
            return false;
        }
        return true;
    }

    private void openInventory() {
        mc.func_147114_u().func_147297_a((Packet)new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
        mc.func_147108_a((GuiScreen)new GuiInventory((EntityPlayer)AutoRefill.mc.field_71439_g));
    }

    private void refillHotbar() {
        int nextPotIndex = this.findNextPotIndex();
        if (nextPotIndex != -1) {
            this.newDelay();
            this.shiftRightClickItem(nextPotIndex);
            this.lastShiftedPotIndex = nextPotIndex;
            if (this.isHotbarFull()) {
                this.closeInventory();
            }
        } else {
            this.closeInventory();
        }
    }

    private int findNextPotIndex() {
        int startIndex;
        int inventorySize = AutoRefill.mc.field_71439_g.field_71071_by.func_70302_i_();
        int i = startIndex = (this.lastShiftedPotIndex + 1 + 9) % inventorySize;
        while (i != startIndex - 1) {
            int slotIndex = i % inventorySize;
            if (slotIndex >= 9) {
                ItemStack stack = AutoRefill.mc.field_71439_g.field_71071_by.func_70301_a(slotIndex);
                if (this.isValidStack(stack)) {
                    this.lastShiftedPotIndex = slotIndex;
                    return slotIndex;
                }
                if (i == (startIndex - 1 + inventorySize) % inventorySize) break;
            }
            i = (i + 1) % inventorySize;
        }
        return -1;
    }

    private void newDelay() {
        this.delay1 = (long)(this.delay.getInputMin() + Math.random() * (this.delay.getInputMax() - this.delay.getInputMin()));
    }

    private boolean isValidStack(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return this.pots.isToggled() && this.isPot(stack) || this.soup.isToggled() && this.isSoup(stack);
    }

    private boolean isSoup(ItemStack stack) {
        return stack.func_77973_b() == Items.field_151009_A;
    }

    private boolean isPot(ItemStack stack) {
        return stack != null && stack.func_77973_b() instanceof ItemPotion && ((ItemPotion)stack.func_77973_b()).func_77832_l(stack).stream().anyMatch(effect -> effect.func_76456_a() == Potion.field_76432_h.field_76415_H);
    }

    private void shiftRightClickItem(int slotIndex) {
        AutoRefill.mc.field_71442_b.func_78753_a(AutoRefill.mc.field_71439_g.field_71069_bz.field_75152_c, slotIndex, 0, 1, (EntityPlayer)AutoRefill.mc.field_71439_g);
        AutoRefill.mc.field_71442_b.func_78765_e();
    }

    private void closeInventory() {
        AutoRefill.mc.field_71439_g.func_71053_j();
        AutoRefill.mc.field_71442_b.func_78752_a(AutoRefill.mc.field_71439_g.field_71071_by.func_70445_o());
        this.refillOpened = false;
        this.disable();
    }
}

