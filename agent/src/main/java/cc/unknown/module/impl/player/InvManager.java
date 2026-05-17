/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.BlockChest
 *  net.minecraft.block.BlockFalling
 *  net.minecraft.block.BlockTNT
 *  net.minecraft.client.gui.inventory.GuiChest
 *  net.minecraft.client.gui.inventory.GuiInventory
 *  net.minecraft.client.multiplayer.PlayerControllerMP
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemAnvilBlock
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemAxe
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemBow
 *  net.minecraft.item.ItemFood
 *  net.minecraft.item.ItemPickaxe
 *  net.minecraft.item.ItemPotion
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.item.ItemTool
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C0DPacketCloseWindow
 *  net.minecraft.network.play.client.C16PacketClientStatus
 *  net.minecraft.network.play.client.C16PacketClientStatus$EnumState
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.util.DamageSource
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.client.Cold;
import java.util.ArrayList;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAnvilBlock;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

@Register(name="InvManager", category=Category.Player)
public class InvManager
extends Module {
    private final SliderValue delay = new SliderValue("Delay", 150.0, 0.0, 300.0, 25.0);
    private final BooleanValue openInv = new BooleanValue("Open Inventory", true);
    private final BooleanValue dropTrash = new BooleanValue("Drop Trash", true);
    private final BooleanValue autoArmor = new BooleanValue("Auto Armor", true);
    private final BooleanValue noMove = new BooleanValue("Disable Movement", true);
    private final int INVENTORY_ROWS = 4;
    private final int INVENTORY_COLUMNS = 9;
    private final int ARMOR_SLOTS = 4;
    private final int INVENTORY_SLOTS = 40;
    private PlayerControllerMP playerController;
    private final Cold timer = new Cold(0L);
    private boolean movedItem;
    private boolean inventoryOpen;

    public InvManager() {
        this.registerSetting(this.delay, this.openInv, this.dropTrash, this.autoArmor, this.noMove);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.delay.getInputToInt() + " ms]");
    }

    @Override
    public void onEnable() {
        this.timer.reset();
    }

    @Override
    public void onDisable() {
        this.closeInventory();
    }

    @EventLink
    public void onPre(MotionEvent e) {
        if (e.isPre()) {
            ItemArmor armor;
            Item item;
            ItemStack itemStack;
            int i;
            if (!this.timer.reached(this.delay.getInputToLong())) {
                this.closeInventory();
                return;
            }
            if (InvManager.mc.field_71462_r instanceof GuiChest) {
                return;
            }
            if ((InvManager.mc.field_71474_y.field_74314_A.func_151470_d() || InvManager.mc.field_71474_y.field_74351_w.func_151470_d() || InvManager.mc.field_71474_y.field_74370_x.func_151470_d() || InvManager.mc.field_71474_y.field_74368_y.func_151470_d() || InvManager.mc.field_71474_y.field_74366_z.func_151470_d()) && this.noMove.isToggled()) {
                return;
            }
            this.movedItem = false;
            this.timer.reset();
            this.timer.reached(this.delay.getInputToLong());
            if (!(InvManager.mc.field_71462_r instanceof GuiInventory) && this.openInv.isToggled()) {
                return;
            }
            this.playerController = InvManager.mc.field_71442_b;
            if (this.dropTrash.isToggled()) {
                for (int i2 = 0; i2 < 40; ++i2) {
                    ItemStack itemStack2 = InvManager.mc.field_71439_g.field_71071_by.func_70301_a(i2);
                    if (itemStack2 == null || itemStack2.func_77973_b() == null || this.itemWhitelisted(itemStack2)) continue;
                    this.throwItem(this.getSlotId(i2));
                }
            }
            Integer bestHelmet = null;
            Integer bestChestPlate = null;
            Integer bestLeggings = null;
            Integer bestBoots = null;
            Integer bestSword = null;
            Integer bestPickaxe = null;
            Integer bestAxe = null;
            Integer bestBlock = null;
            Integer bestBow = null;
            Integer bestPotion = null;
            for (i = 0; i < 40; ++i) {
                itemStack = InvManager.mc.field_71439_g.field_71071_by.func_70301_a(i);
                if (itemStack == null || itemStack.func_77973_b() == null) continue;
                item = itemStack.func_77973_b();
                if (item instanceof ItemArmor) {
                    armor = (ItemArmor)item;
                    int damageReductionItem = this.getArmorDamageReduction(itemStack);
                    if (armor.field_77881_a == 0 && (bestHelmet == null || damageReductionItem > this.getArmorDamageReduction(InvManager.mc.field_71439_g.field_71071_by.func_70301_a(bestHelmet.intValue())))) {
                        bestHelmet = i;
                    }
                    if (armor.field_77881_a == 1 && (bestChestPlate == null || damageReductionItem > this.getArmorDamageReduction(InvManager.mc.field_71439_g.field_71071_by.func_70301_a(bestChestPlate.intValue())))) {
                        bestChestPlate = i;
                    }
                    if (armor.field_77881_a == 2 && (bestLeggings == null || damageReductionItem > this.getArmorDamageReduction(InvManager.mc.field_71439_g.field_71071_by.func_70301_a(bestLeggings.intValue())))) {
                        bestLeggings = i;
                    }
                    if (armor.field_77881_a == 3 && (bestBoots == null || damageReductionItem > this.getArmorDamageReduction(InvManager.mc.field_71439_g.field_71071_by.func_70301_a(bestBoots.intValue())))) {
                        bestBoots = i;
                    }
                }
                if (item instanceof ItemSword) {
                    float damage = this.getSwordDamage(itemStack);
                    if (bestSword == null || damage > this.getSwordDamage(InvManager.mc.field_71439_g.field_71071_by.func_70301_a(bestSword.intValue()))) {
                        bestSword = i;
                    }
                }
                if (item instanceof ItemPickaxe) {
                    float mineSpeed = this.getMineSpeed(itemStack);
                    if (bestPickaxe == null || mineSpeed > this.getMineSpeed(InvManager.mc.field_71439_g.field_71071_by.func_70301_a(bestPickaxe.intValue()))) {
                        bestPickaxe = i;
                    }
                }
                if (item instanceof ItemAxe) {
                    float mineSpeed = this.getMineSpeed(itemStack);
                    if (bestAxe == null || mineSpeed > this.getMineSpeed(InvManager.mc.field_71439_g.field_71071_by.func_70301_a(bestAxe.intValue()))) {
                        bestAxe = i;
                    }
                }
                if (item instanceof ItemBlock && ((ItemBlock)item).func_179223_d().func_149686_d()) {
                    float amountOfBlocks = itemStack.field_77994_a;
                    if (bestBlock == null || amountOfBlocks > (float)InvManager.mc.field_71439_g.field_71071_by.func_70301_a((int)bestBlock.intValue()).field_77994_a) {
                        bestBlock = i;
                    }
                }
                if (item instanceof ItemBow) {
                    int level = EnchantmentHelper.func_77506_a((int)Enchantment.field_77345_t.field_77352_x, (ItemStack)itemStack);
                    if (bestBow == null || level > 1) {
                        bestBow = i;
                    }
                }
                if (!(item instanceof ItemPotion)) continue;
                ItemPotion itemPotion = (ItemPotion)item;
                if (bestPotion != null || !ItemPotion.func_77831_g((int)itemStack.func_77960_j()) || itemPotion.func_77834_f(itemStack.func_77960_j()) == null) continue;
                int potionID = ((PotionEffect)itemPotion.func_77834_f(itemStack.func_77960_j()).get(0)).func_76456_a();
                boolean isPotionActive = false;
                for (PotionEffect potion : InvManager.mc.field_71439_g.func_70651_bq()) {
                    if (potion.func_76456_a() != potionID || potion.func_76459_b() <= 0) continue;
                    isPotionActive = true;
                    break;
                }
                ArrayList<Integer> whitelistedPotions = new ArrayList<Integer>(){
                    {
                        this.add(1);
                        this.add(5);
                        this.add(8);
                        this.add(14);
                        this.add(12);
                        this.add(16);
                    }
                };
                if (isPotionActive || !whitelistedPotions.contains(potionID) && potionID != 10 && potionID != 6) continue;
                bestPotion = i;
            }
            if (this.dropTrash.isToggled()) {
                for (i = 0; i < 40; ++i) {
                    itemStack = InvManager.mc.field_71439_g.field_71071_by.func_70301_a(i);
                    if (itemStack == null || itemStack.func_77973_b() == null) continue;
                    item = itemStack.func_77973_b();
                    if (item instanceof ItemArmor) {
                        armor = (ItemArmor)item;
                        if (armor.field_77881_a == 0 && bestHelmet != null && i != bestHelmet || armor.field_77881_a == 1 && bestChestPlate != null && i != bestChestPlate || armor.field_77881_a == 2 && bestLeggings != null && i != bestLeggings || armor.field_77881_a == 3 && bestBoots != null && i != bestBoots) {
                            this.throwItem(this.getSlotId(i));
                        }
                    }
                    if (item instanceof ItemSword && bestSword != null && i != bestSword) {
                        this.throwItem(this.getSlotId(i));
                    }
                    if (item instanceof ItemPickaxe && bestPickaxe != null && i != bestPickaxe) {
                        this.throwItem(this.getSlotId(i));
                    }
                    if (item instanceof ItemAxe && bestAxe != null && i != bestAxe) {
                        this.throwItem(this.getSlotId(i));
                    }
                    if (!(item instanceof ItemBow) || bestBow == null || i == bestBow) continue;
                    this.throwItem(this.getSlotId(i));
                }
            }
            if (this.autoArmor.isToggled()) {
                if (bestHelmet != null) {
                    this.equipArmor(this.getSlotId(bestHelmet));
                }
                if (bestChestPlate != null) {
                    this.equipArmor(this.getSlotId(bestChestPlate));
                }
                if (bestLeggings != null) {
                    this.equipArmor(this.getSlotId(bestLeggings));
                }
                if (bestBoots != null) {
                    this.equipArmor(this.getSlotId(bestBoots));
                }
            }
        }
    }

    private float getSwordDamage(ItemStack itemStack) {
        ItemSword sword = (ItemSword)itemStack.func_77973_b();
        int efficiencyLevel = EnchantmentHelper.func_77506_a((int)Enchantment.field_180314_l.field_77352_x, (ItemStack)itemStack);
        return (float)((double)sword.func_150931_i() + (double)efficiencyLevel * 1.25);
    }

    private int getArmorDamageReduction(ItemStack itemStack) {
        return ((ItemArmor)itemStack.func_77973_b()).field_77879_b + EnchantmentHelper.func_77508_a((ItemStack[])new ItemStack[]{itemStack}, (DamageSource)DamageSource.field_76377_j);
    }

    private void openInventory() {
        if (!this.inventoryOpen) {
            this.inventoryOpen = true;
            InvManager.mc.field_71439_g.field_71174_a.func_147297_a((Packet)new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
        }
    }

    private void closeInventory() {
        if (this.inventoryOpen) {
            this.inventoryOpen = false;
            InvManager.mc.field_71439_g.field_71174_a.func_147297_a((Packet)new C0DPacketCloseWindow(0));
        }
    }

    private void throwItem(int slot) {
        try {
            if (!this.movedItem) {
                this.openInventory();
                this.playerController.func_78753_a(InvManager.mc.field_71439_g.field_71069_bz.field_75152_c, slot, 1, 4, (EntityPlayer)InvManager.mc.field_71439_g);
                this.movedItem = true;
            }
        }
        catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            // empty catch block
        }
    }

    private void equipArmor(int slot) {
        try {
            if (slot > 8 && !this.movedItem) {
                this.openInventory();
                this.playerController.func_78753_a(InvManager.mc.field_71439_g.field_71069_bz.field_75152_c, slot, 0, 1, (EntityPlayer)InvManager.mc.field_71439_g);
                this.movedItem = true;
            }
        }
        catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            // empty catch block
        }
    }

    public int getSlotId(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        }
        if (slot < 9) {
            return slot + 36;
        }
        return slot;
    }

    private boolean itemWhitelisted(ItemStack itemStack) {
        ArrayList<Item> whitelistedItems = new ArrayList<Item>(){
            {
                this.add(Items.field_151079_bi);
                this.add(Items.field_151031_f);
                this.add(Items.field_151032_g);
                this.add(Items.field_151117_aB);
                this.add(Items.field_151131_as);
            }
        };
        Item item = itemStack.func_77973_b();
        ArrayList<Integer> whitelistedPotions = new ArrayList<Integer>(){
            {
                this.add(6);
                this.add(1);
                this.add(5);
                this.add(8);
                this.add(14);
                this.add(12);
                this.add(10);
                this.add(16);
            }
        };
        if (item instanceof ItemPotion) {
            int potionID = this.getPotionId(itemStack);
            return whitelistedPotions.contains(potionID);
        }
        return item instanceof ItemBlock && !(((ItemBlock)item).func_179223_d() instanceof BlockTNT) && !(((ItemBlock)item).func_179223_d() instanceof BlockChest) && !(((ItemBlock)item).func_179223_d() instanceof BlockFalling) || item instanceof ItemAnvilBlock || item instanceof ItemSword || item instanceof ItemArmor || item instanceof ItemTool || item instanceof ItemFood || whitelistedItems.contains(item) && !item.equals(Items.field_151070_bp);
    }

    private int getPotionId(ItemStack potion) {
        Item item = potion.func_77973_b();
        try {
            if (item instanceof ItemPotion) {
                ItemPotion p = (ItemPotion)item;
                return ((PotionEffect)p.func_77834_f(potion.func_77960_j()).get(0)).func_76456_a();
            }
        }
        catch (NullPointerException nullPointerException) {
            // empty catch block
        }
        return 0;
    }

    private float getMineSpeed(ItemStack itemStack) {
        Item item = itemStack.func_77973_b();
        int efficiencyLevel = EnchantmentHelper.func_77506_a((int)Enchantment.field_77349_p.field_77352_x, (ItemStack)itemStack);
        switch (efficiencyLevel) {
            case 1: {
                efficiencyLevel = 30;
                break;
            }
            case 2: {
                efficiencyLevel = 69;
                break;
            }
            case 3: {
                efficiencyLevel = 120;
                break;
            }
            case 4: {
                efficiencyLevel = 186;
                break;
            }
            case 5: {
                efficiencyLevel = 271;
                break;
            }
            default: {
                efficiencyLevel = 0;
            }
        }
        if (item instanceof ItemPickaxe || item instanceof ItemAxe) {
            return this.getToolEfficiency(item) + (float)efficiencyLevel;
        }
        return 0.0f;
    }

    private float getToolEfficiency(Item item) {
        if (item instanceof ItemPickaxe) {
            return ((ItemPickaxe)item).func_150913_i().func_77998_b();
        }
        if (item instanceof ItemAxe) {
            return ((ItemAxe)item).func_150913_i().func_77998_b();
        }
        return 0.0f;
    }
}

