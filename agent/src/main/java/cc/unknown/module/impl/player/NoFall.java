/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.network.play.client.C03PacketPlayer
 *  net.minecraft.util.BlockPos
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.MovingObjectPosition
 *  net.minecraft.util.MovingObjectPosition$MovingObjectType
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.utils.network.PacketUtil;
import cc.unknown.utils.player.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@Register(name="NoFall", category=Category.Player)
public class NoFall
extends Module {
    private boolean handling;
    public static ModeValue mode = new ModeValue("Mode", "Legit", "Legit", "Packet", "Tick No Ground", "Sneak jump");

    public NoFall() {
        this.registerSetting(mode);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + mode.getMode() + "]");
    }

    @EventLink
    public void onTick(TickEvent e) {
        switch (mode.getMode()) {
            case "Legit": {
                if (!PlayerUtil.inGame() || mc.func_147113_T()) break;
                if (this.inNether()) {
                    this.disable();
                }
                if (this.inPosition() && this.holdWaterBucket()) {
                    this.handling = true;
                }
                if (!this.handling) break;
                this.mlg();
                if (!NoFall.mc.field_71439_g.field_70122_E && !(NoFall.mc.field_71439_g.field_70181_x > 0.0)) break;
                this.reset();
                break;
            }
            case "Packet": {
                PacketUtil.send(new C03PacketPlayer(true));
                break;
            }
            case "Sneak jump": {
                if (!(NoFall.mc.field_71439_g.field_70143_R > 10.0f) || !NoFall.mc.field_71474_y.field_74311_E.field_74513_e) break;
                NoFall.mc.field_71474_y.field_74311_E.field_74513_e = NoFall.mc.field_71441_e.func_72945_a((Entity)NoFall.mc.field_71439_g, NoFall.mc.field_71439_g.func_70046_E().func_72317_d(0.0, NoFall.mc.field_71439_g.field_70159_w, 0.0)) != null;
            }
        }
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        if (e.isSend() && mode.is("Tick No Ground") && e.getPacket() instanceof C03PacketPlayer) {
            C03PacketPlayer c03 = (C03PacketPlayer)e.getPacket();
            if (NoFall.mc.field_71439_g != null && (double)NoFall.mc.field_71439_g.field_70143_R > 1.5) {
                c03.field_149474_g = NoFall.mc.field_71439_g.field_70173_aa % 2 == 0;
            }
        }
    }

    private boolean inPosition() {
        if (!(!(NoFall.mc.field_71439_g.field_70181_x < -0.6) || NoFall.mc.field_71439_g.field_70122_E || NoFall.mc.field_71439_g.field_71075_bZ.field_75100_b || NoFall.mc.field_71439_g.field_71075_bZ.field_75098_d || this.handling)) {
            BlockPos playerPos = NoFall.mc.field_71439_g.func_180425_c();
            for (int i = 1; i < 3; ++i) {
                BlockPos blockPos = playerPos.func_177979_c(i);
                Block block = NoFall.mc.field_71441_e.func_180495_p(blockPos).func_177230_c();
                if (!block.func_176212_b((IBlockAccess)NoFall.mc.field_71441_e, blockPos, EnumFacing.UP)) continue;
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean holdWaterBucket() {
        if (this.containsItem(NoFall.mc.field_71439_g.func_70694_bm(), Items.field_151131_as)) {
            return true;
        }
        for (int i = 0; i < InventoryPlayer.func_70451_h(); ++i) {
            if (!this.containsItem(NoFall.mc.field_71439_g.field_71071_by.field_70462_a[i], Items.field_151131_as)) continue;
            NoFall.mc.field_71439_g.field_71071_by.field_70461_c = i;
            return true;
        }
        return false;
    }

    private void mlg() {
        ItemStack heldItem = NoFall.mc.field_71439_g.func_70694_bm();
        if (this.containsItem(heldItem, Items.field_151131_as) && NoFall.mc.field_71439_g.field_70125_A >= 70.0f) {
            MovingObjectPosition object = NoFall.mc.field_71476_x;
            if (object.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK && object.field_178784_b == EnumFacing.UP) {
                NoFall.mc.field_71442_b.func_78769_a((EntityPlayer)NoFall.mc.field_71439_g, (World)NoFall.mc.field_71441_e, heldItem);
            }
        }
    }

    private void reset() {
        ItemStack heldItem = NoFall.mc.field_71439_g.func_70694_bm();
        if (this.containsItem(heldItem, Items.field_151133_ar)) {
            NoFall.mc.field_71442_b.func_78769_a((EntityPlayer)NoFall.mc.field_71439_g, (World)NoFall.mc.field_71441_e, heldItem);
        }
        this.handling = false;
    }

    private boolean containsItem(ItemStack itemStack, Item item) {
        return itemStack != null && itemStack.func_77973_b() == item;
    }

    private boolean inNether() {
        if (!PlayerUtil.inGame()) {
            return false;
        }
        return NoFall.mc.field_71439_g.field_71093_bK == -1;
    }
}

