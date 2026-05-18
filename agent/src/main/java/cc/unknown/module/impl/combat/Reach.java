/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityItemFrame
 *  net.minecraft.init.Blocks
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C0FPacketConfirmTransaction
 *  net.minecraft.network.play.server.S37PacketStatistics
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.BlockPos
 *  net.minecraft.util.MovingObjectPosition
 *  net.minecraft.util.Vec3
 *  org.lwjgl.input.Mouse
 */
package cc.unknown.module.impl.combat;

import cc.unknown.Haru;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.other.MouseEvent;
import cc.unknown.mixin.interfaces.network.packets.IC0FPacketConfirmTransaction;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.impl.combat.AutoClick;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.helpers.MathHelper;
import cc.unknown.utils.misc.ClickUtil;
import cc.unknown.utils.player.PlayerUtil;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

@Register(name="Reach", category=Category.Combat)
public class Reach
extends Module {
    private ModeValue mode = new ModeValue("Mode", "Basic", "Basic", "Verus");
    private DoubleSliderValue rangeCombat = new DoubleSliderValue("Range", 3.0, 3.0, 2.9, 6.0, 0.01);
    private SliderValue chance = new SliderValue("Chance", 100.0, 0.0, 100.0, 1.0);
    private BooleanValue moving_only = new BooleanValue("Only Move", false);
    private BooleanValue sprint_only = new BooleanValue("Only Sprint", false);
    private BooleanValue hit_through_blocks = new BooleanValue("Hit through blocks", false);
    private int oldTransaction = 0;
    private short newTransaction = 0;

    public Reach() {
        this.registerSetting(this.mode, this.rangeCombat, this.chance, this.moving_only, this.sprint_only, this.hit_through_blocks);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.rangeCombat.getInputMin() + ", " + this.rangeCombat.getInputMax() + "]");
    }

    @Override
    public void onEnable() {
        this.oldTransaction = 0;
        this.newTransaction = 0;
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        if (Reach.mc.field_71439_g == null || Reach.mc.field_71439_g.field_70173_aa < 20) {
            return;
        }
        Packet<?> p = e.getPacket();
        if (e.isSend() && this.mode.is("Verus") && p instanceof C0FPacketConfirmTransaction) {
            C0FPacketConfirmTransaction wrapper = (C0FPacketConfirmTransaction)p;
            ++this.oldTransaction;
            if (this.oldTransaction <= 1) {
                this.newTransaction = wrapper.func_149533_d();
            }
            ((IC0FPacketConfirmTransaction)wrapper).setUid(this.newTransaction);
        }
        if (e.isReceive() && this.mode.is("Verus") && p instanceof S37PacketStatistics) {
            this.oldTransaction = 0;
        }
    }

    @EventLink
    public void onMouse(MouseEvent e) {
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        if (PlayerUtil.inGame() && e.getButton() == 0 && (!clicker.isEnabled() || !Mouse.isButtonDown((int)0)) || ClickUtil.instance.isClicking()) {
            this.callReach();
        }
    }

    private boolean callReach() {
        BlockPos p;
        if (!PlayerUtil.inGame()) {
            return false;
        }
        if (this.moving_only.isToggled() && (double)Reach.mc.field_71439_g.field_70701_bs == 0.0 && (double)Reach.mc.field_71439_g.field_70702_br == 0.0) {
            return false;
        }
        if (this.sprint_only.isToggled() && !Reach.mc.field_71439_g.func_70051_ag()) {
            return false;
        }
        if (this.chance.getInput() != 100.0 && !(Math.random() <= this.chance.getInput() / 100.0)) {
            return false;
        }
        if (!this.hit_through_blocks.isToggled() && Reach.mc.field_71476_x != null && (p = Reach.mc.field_71476_x.func_178782_a()) != null && Reach.mc.field_71441_e.func_180495_p(p).func_177230_c() != Blocks.field_150350_a) {
            return false;
        }
        double reach = ClickUtil.instance.ranModuleVal(this.rangeCombat, MathHelper.rand());
        Object[] object = this.findEntitiesWithinReach(reach);
        if (object == null) {
            return false;
        }
        Entity en = (Entity)object[0];
        Reach.mc.field_71476_x = new MovingObjectPosition(en, (Vec3)object[1]);
        Reach.mc.field_147125_j = en;
        return true;
    }

    private Object[] findEntitiesWithinReach(double reach) {
        Reach reich = (Reach)Haru.instance.getModuleManager().getModule(Reach.class);
        if (!reich.isEnabled()) {
            reach = Reach.mc.field_71442_b.func_78749_i() ? 6.0 : 3.0;
        }
        Entity renderView = mc.func_175606_aa();
        Entity target = null;
        if (renderView == null) {
            return null;
        }
        Reach.mc.field_71424_I.func_76320_a("pick");
        Vec3 eyePosition = renderView.func_174824_e(1.0f);
        Vec3 playerLook = renderView.func_70676_i(1.0f);
        Vec3 reachTarget = eyePosition.func_72441_c(playerLook.field_72450_a * reach, playerLook.field_72448_b * reach, playerLook.field_72449_c * reach);
        Vec3 targetHitVec = null;
        List<Entity> targetsWithinReach = Reach.mc.field_71441_e.func_72839_b(renderView, renderView.func_174813_aQ().func_72321_a(playerLook.field_72450_a * reach, playerLook.field_72448_b * reach, playerLook.field_72449_c * reach).func_72314_b(1.0, 1.0, 1.0));
        double adjustedReach = reach;
        for (Entity entity : targetsWithinReach) {
            double distanceToVec;
            if (!entity.func_70067_L()) continue;
            float ex = (float)((double)entity.func_70111_Y());
            AxisAlignedBB entityBoundingBox = entity.func_174813_aQ().func_72314_b((double)ex, (double)ex, (double)ex);
            MovingObjectPosition targetPosition = entityBoundingBox.func_72327_a(eyePosition, reachTarget);
            if (entityBoundingBox.func_72318_a(eyePosition)) {
                if (!(0.0 < adjustedReach) && adjustedReach != 0.0) continue;
                target = entity;
                targetHitVec = targetPosition == null ? eyePosition : targetPosition.field_72307_f;
                adjustedReach = 0.0;
                continue;
            }
            if (targetPosition == null || !((distanceToVec = eyePosition.func_72438_d(targetPosition.field_72307_f)) < adjustedReach) && adjustedReach != 0.0) continue;
            if (entity == renderView.field_70154_o) {
                if (adjustedReach != 0.0) continue;
                target = entity;
                targetHitVec = targetPosition.field_72307_f;
                continue;
            }
            target = entity;
            targetHitVec = targetPosition.field_72307_f;
            adjustedReach = distanceToVec;
        }
        if (adjustedReach < reach && !(target instanceof EntityLivingBase) && !(target instanceof EntityItemFrame)) {
            target = null;
        }
        Reach.mc.field_71424_I.func_76319_b();
        if (target != null && targetHitVec != null) {
            return new Object[]{target, targetHitVec};
        }
        return null;
    }
}

