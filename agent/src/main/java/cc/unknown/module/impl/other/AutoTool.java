/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.ItemStack
 *  net.minecraft.network.play.client.C02PacketUseEntity
 *  net.minecraft.network.play.client.C02PacketUseEntity$Action
 *  net.minecraft.util.MovingObjectPosition$MovingObjectType
 */
package cc.unknown.module.impl.other;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MovingObjectPosition;

@Register(name="AutoTool", category=Category.Other)
public class AutoTool
extends Module {
    private int prevItem = 0;
    private boolean mining = false;
    private int bestSlot = 0;

    @EventLink
    public void onPacket(PacketEvent e) {
        C02PacketUseEntity wrapper;
        if (e.isSend() && e.getPacket() instanceof C02PacketUseEntity && (wrapper = (C02PacketUseEntity)e.getPacket()).func_149565_c() == C02PacketUseEntity.Action.ATTACK) {
            this.mining = false;
        }
    }

    @EventLink
    public void onRender(RenderEvent e) {
        if (e.is3D()) {
            if (!AutoTool.mc.field_71474_y.field_74313_G.func_151470_d() && AutoTool.mc.field_71474_y.field_74312_F.func_151470_d() && AutoTool.mc.field_71476_x != null && AutoTool.mc.field_71476_x.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK) {
                int bestSpeed = 0;
                this.bestSlot = -1;
                if (!this.mining) {
                    this.prevItem = AutoTool.mc.field_71439_g.field_71071_by.field_70461_c;
                }
                Block block = AutoTool.mc.field_71441_e.func_180495_p(AutoTool.mc.field_71476_x.func_178782_a()).func_177230_c();
                for (int i = 0; i <= 8; ++i) {
                    ItemStack item = AutoTool.mc.field_71439_g.field_71071_by.func_70301_a(i);
                    if (item == null) continue;
                    float speed = item.func_150997_a(block);
                    if (speed > (float)bestSpeed) {
                        bestSpeed = (int)speed;
                        this.bestSlot = i;
                    }
                    if (this.bestSlot == -1) continue;
                    AutoTool.mc.field_71439_g.field_71071_by.field_70461_c = this.bestSlot;
                }
                this.mining = true;
            } else if (this.mining) {
                this.mining = false;
            } else {
                this.prevItem = AutoTool.mc.field_71439_g.field_71071_by.field_70461_c;
            }
        }
    }
}

