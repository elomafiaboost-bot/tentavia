/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemBow
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C07PacketPlayerDigging
 *  net.minecraft.network.play.client.C07PacketPlayerDigging$Action
 *  net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
 *  net.minecraft.network.play.client.C09PacketHeldItemChange
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.network.PacketUtil;
import net.minecraft.item.ItemBow;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

@Register(name="NoSlow", category=Category.Player)
public class NoSlow
extends Module {
    public ModeValue mode = new ModeValue("Mode", "Old Grim", "Old Grim", "Vanilla", "No Item Release", "C08 Tick");
    public SliderValue vForward = new SliderValue("Vanilla forward", 1.0, 0.2, 1.0, 0.1);
    public SliderValue vStrafe = new SliderValue("Vanilla strafe", 1.0, 0.2, 1.0, 0.1);

    public NoSlow() {
        this.registerSetting(this.mode, this.vForward, this.vStrafe);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.mode.getMode() + "]");
    }

    public void slow() {
        switch (this.mode.getMode()) {
            case "Old Grim": {
                int slot = NoSlow.mc.field_71439_g.field_71071_by.field_70461_c;
                PacketUtil.sendPacketNoEvent(new C09PacketHeldItemChange(slot < 8 ? slot + 1 : 0));
                PacketUtil.sendPacketNoEvent(new C09PacketHeldItemChange(slot));
                break;
            }
            case "Vanilla": {
                NoSlow.mc.field_71439_g.field_71158_b.field_78900_b *= this.vForward.getInputToFloat();
                NoSlow.mc.field_71439_g.field_71158_b.field_78902_a *= this.vStrafe.getInputToFloat();
                break;
            }
            case "C08 Tick": {
                if (NoSlow.mc.field_71439_g.field_70173_aa % 3 != 0) break;
                mc.func_147114_u().func_147297_a((Packet)new C08PacketPlayerBlockPlacement(NoSlow.mc.field_71439_g.func_70694_bm()));
            }
        }
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        if (e.isSend()) {
            C07PacketPlayerDigging wrapper;
            Packet<?> p = e.getPacket();
            if (this.mode.is("No Item Release") && p instanceof C07PacketPlayerDigging && (wrapper = (C07PacketPlayerDigging)p).func_180762_c() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM && !(NoSlow.mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemBow)) {
                e.setCancelled(true);
            }
        }
    }
}

