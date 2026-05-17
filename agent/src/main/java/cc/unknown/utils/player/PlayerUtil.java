/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockAir
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemAxe
 *  net.minecraft.item.ItemPickaxe
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.util.BlockPos
 *  net.minecraft.util.ChatComponentText
 *  net.minecraft.util.IChatComponent
 *  net.minecraft.util.MathHelper
 *  org.lwjgl.input.Mouse
 */
package cc.unknown.utils.player;

import cc.unknown.utils.Loona;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

public class PlayerUtil
implements Loona {
    public static void send(Object message, Object ... objects) {
        if (PlayerUtil.inGame()) {
            String format = String.format(message.toString(), objects);
            PlayerUtil.mc.field_71439_g.func_145747_a((IChatComponent)new ChatComponentText("" + format));
        }
    }

    public static boolean inGame() {
        return PlayerUtil.mc.field_71439_g != null && PlayerUtil.mc.field_71441_e != null;
    }

    public static boolean isMoving() {
        return PlayerUtil.mc.field_71439_g.field_70701_bs != 0.0f || PlayerUtil.mc.field_71439_g.field_70702_br != 0.0f;
    }

    public static boolean tryingToCombo() {
        return Mouse.isButtonDown((int)0) && Mouse.isButtonDown((int)1);
    }

    public static boolean lookingAtPlayer(EntityPlayer v, EntityPlayer e, double m) {
        double deltaZ;
        double deltaX = e.field_70165_t - v.field_70165_t;
        double deltaY = e.field_70163_u - v.field_70163_u + (double)v.func_70047_e();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + (deltaZ = e.field_70161_v - v.field_70161_v) * deltaZ);
        return distance < m;
    }

    public static double fovFromEntity(Entity en) {
        return ((double)(PlayerUtil.mc.field_71439_g.field_70177_z - PlayerUtil.fovToEntity(en)) % 360.0 + 540.0) % 360.0 - 180.0;
    }

    public static float fovToEntity(Entity ent) {
        double x = ent.field_70165_t - PlayerUtil.mc.field_71439_g.field_70165_t;
        double z = ent.field_70161_v - PlayerUtil.mc.field_71439_g.field_70161_v;
        double yaw = Math.atan2(x, z) * 57.2957795;
        return (float)(yaw * -1.0);
    }

    public static boolean fov(Entity entity, float fov) {
        fov = (float)((double)fov * 0.5);
        double v = ((double)(PlayerUtil.mc.field_71439_g.field_70177_z - PlayerUtil.fovToEntity(entity)) % 360.0 + 540.0) % 360.0 - 180.0;
        return v > 0.0 && v < (double)fov || (double)(-fov) < v && v < 0.0;
    }

    public static boolean onMouseOver() {
        return PlayerUtil.mc.field_71476_x != null && PlayerUtil.mc.field_71476_x.field_72308_g != null;
    }

    public static boolean playerOverAir() {
        return PlayerUtil.mc.field_71441_e.func_175623_d(new BlockPos(MathHelper.func_76128_c((double)PlayerUtil.mc.field_71439_g.field_70165_t), MathHelper.func_76128_c((double)(PlayerUtil.mc.field_71439_g.field_70163_u - 1.0)), MathHelper.func_76128_c((double)PlayerUtil.mc.field_71439_g.field_70161_v)));
    }

    public static boolean isBlockUnder(int offset) {
        for (int i = (int)(PlayerUtil.mc.field_71439_g.field_70163_u - (double)offset); i > 0; --i) {
            BlockPos pos = new BlockPos(PlayerUtil.mc.field_71439_g.field_70165_t, (double)i, PlayerUtil.mc.field_71439_g.field_70161_v);
            if (PlayerUtil.mc.field_71441_e.func_180495_p(pos).func_177230_c() instanceof BlockAir) continue;
            return true;
        }
        return false;
    }

    public static boolean isHoldingWeapon() {
        if (PlayerUtil.mc.field_71439_g.func_71045_bC() == null) {
            return false;
        }
        Item item = PlayerUtil.mc.field_71439_g.func_71045_bC().func_77973_b();
        return item instanceof ItemSword || item instanceof ItemAxe;
    }

    public static double getDirection() {
        float moveYaw = PlayerUtil.mc.field_71439_g.field_70177_z;
        if (PlayerUtil.mc.field_71439_g.field_70701_bs != 0.0f && PlayerUtil.mc.field_71439_g.field_70702_br == 0.0f) {
            moveYaw += PlayerUtil.mc.field_71439_g.field_70701_bs > 0.0f ? 0.0f : 180.0f;
        } else if (PlayerUtil.mc.field_71439_g.field_70701_bs != 0.0f && PlayerUtil.mc.field_71439_g.field_70702_br != 0.0f) {
            moveYaw = PlayerUtil.mc.field_71439_g.field_70701_bs > 0.0f ? (moveYaw += PlayerUtil.mc.field_71439_g.field_70702_br > 0.0f ? -45.0f : 45.0f) : (moveYaw -= PlayerUtil.mc.field_71439_g.field_70702_br > 0.0f ? -45.0f : 45.0f);
            moveYaw += PlayerUtil.mc.field_71439_g.field_70701_bs > 0.0f ? 0.0f : 180.0f;
        } else if (PlayerUtil.mc.field_71439_g.field_70702_br != 0.0f && PlayerUtil.mc.field_71439_g.field_70701_bs == 0.0f) {
            moveYaw += PlayerUtil.mc.field_71439_g.field_70702_br > 0.0f ? -90.0f : 90.0f;
        }
        return Math.floorMod((int)moveYaw, 360);
    }

    public static ItemStack getBestSword() {
        int size = PlayerUtil.mc.field_71439_g.field_71069_bz.func_75138_a().size();
        ItemStack lastSword = null;
        for (int i = 0; i < size; ++i) {
            ItemStack stack = (ItemStack)PlayerUtil.mc.field_71439_g.field_71069_bz.func_75138_a().get(i);
            if (stack == null || !(stack.func_77973_b() instanceof ItemSword)) continue;
            if (lastSword == null) {
                lastSword = stack;
                continue;
            }
            if (!PlayerUtil.isBetterSword(stack, lastSword)) continue;
            lastSword = stack;
        }
        return lastSword;
    }

    public static ItemStack getBestAxe() {
        int size = PlayerUtil.mc.field_71439_g.field_71069_bz.func_75138_a().size();
        ItemStack lastAxe = null;
        for (int i = 0; i < size; ++i) {
            ItemStack stack = (ItemStack)PlayerUtil.mc.field_71439_g.field_71069_bz.func_75138_a().get(i);
            if (stack == null || !(stack.func_77973_b() instanceof ItemAxe)) continue;
            if (lastAxe == null) {
                lastAxe = stack;
                continue;
            }
            if (!PlayerUtil.isBetterTool(stack, lastAxe, Blocks.field_150344_f)) continue;
            lastAxe = stack;
        }
        return lastAxe;
    }

    public static ItemStack getBestPickaxe() {
        int size = PlayerUtil.mc.field_71439_g.field_71069_bz.func_75138_a().size();
        ItemStack lastPickaxe = null;
        for (int i = 0; i < size; ++i) {
            ItemStack stack = (ItemStack)PlayerUtil.mc.field_71439_g.field_71069_bz.func_75138_a().get(i);
            if (stack == null || !(stack.func_77973_b() instanceof ItemPickaxe)) continue;
            if (lastPickaxe == null) {
                lastPickaxe = stack;
                continue;
            }
            if (!PlayerUtil.isBetterTool(stack, lastPickaxe, Blocks.field_150348_b)) continue;
            lastPickaxe = stack;
        }
        return lastPickaxe;
    }

    public static boolean isBetterTool(ItemStack better, ItemStack than, Block versus) {
        return PlayerUtil.getToolDigEfficiency(better, versus) > PlayerUtil.getToolDigEfficiency(than, versus);
    }

    public static boolean isBetterSword(ItemStack better, ItemStack than) {
        return PlayerUtil.getSwordDamage((ItemSword)better.func_77973_b(), better) > PlayerUtil.getSwordDamage((ItemSword)than.func_77973_b(), than);
    }

    public static float getSwordDamage(ItemSword sword, ItemStack stack) {
        float base = sword.func_77612_l();
        return base + (float)EnchantmentHelper.func_77506_a((int)Enchantment.field_180314_l.field_77352_x, (ItemStack)stack) * 1.25f;
    }

    public static float getToolDigEfficiency(ItemStack stack, Block block) {
        int i;
        float f = stack.func_150997_a(block);
        if (f > 1.0f && (i = EnchantmentHelper.func_77506_a((int)Enchantment.field_77349_p.field_77352_x, (ItemStack)stack)) > 0) {
            f += (float)(i * i + 1);
        }
        return f;
    }
}

