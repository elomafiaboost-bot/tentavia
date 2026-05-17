/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLiquid
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.util.BlockPos
 *  org.apache.commons.lang3.RandomUtils
 *  org.lwjgl.input.Mouse
 */
package cc.unknown.module.impl.combat;

import cc.unknown.Haru;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.LivingEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.impl.combat.AutoClick;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.Loona;
import cc.unknown.utils.misc.ClickUtil;
import cc.unknown.utils.player.CombatUtil;
import cc.unknown.utils.player.FriendUtil;
import cc.unknown.utils.player.PlayerUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Mouse;

@Register(name="AimAssist", category=Category.Combat)
public class AimAssist
extends Module {
    private SliderValue horizontalAimSpeed = new SliderValue("Horizontal Aim Speed", 45.0, 5.0, 100.0, 1.0);
    private SliderValue horizontalAimFineTuning = new SliderValue("Horizontal Aim Fine-tuning", 15.0, 2.0, 97.0, 1.0);
    private BooleanValue horizontalRandomization = new BooleanValue("Horizontal Randomization", false);
    private SliderValue horizontalRandomizationAmount = new SliderValue("Horizontal Randomization", 1.2, 0.1, 5.0, 0.01);
    private SliderValue fieldOfView = new SliderValue("Field of View", 90.0, 15.0, 360.0, 1.0);
    public SliderValue enemyDetectionRange = new SliderValue("Enemy Detection Range", 4.5, 1.0, 10.0, 0.5);
    private BooleanValue verticalAlignmentCheck = new BooleanValue("Vertical Alignment Check", false);
    private BooleanValue verticalRandomization = new BooleanValue("Vertical Randomization", false);
    private SliderValue verticalRandomizationAmount = new SliderValue("Vertical Randomization", 1.2, 0.1, 5.0, 0.01);
    private SliderValue verticalAimSpeed = new SliderValue("Vertical Aim Speed", 10.0, 1.0, 15.0, 1.0);
    private SliderValue verticalAimFineTuning = new SliderValue("Vertical Aim Fine-tuning", 5.0, 1.0, 10.0, 1.0);
    private BooleanValue clickAim = new BooleanValue("Auto Aim on Click", true);
    private BooleanValue centerAim = new BooleanValue("Instant Aim Centering", false);
    private BooleanValue ignoreFriendlyEntities = new BooleanValue("Ignore Friendly Entities", false);
    private BooleanValue ignoreTeammates = new BooleanValue("Ignore Teammates", false);
    private BooleanValue aimAtInvisibleEnemies = new BooleanValue("Aim at Invisible Enemies", false);
    private BooleanValue lineOfSightCheck = new BooleanValue("Line of Sight Check", true);
    private BooleanValue disableAimWhileBreakingBlock = new BooleanValue("Disable Aim While Breaking Block", false);
    private BooleanValue weaponOnly = new BooleanValue("Weapon Only Aim", false);
    private Random random = new Random();

    public AimAssist() {
        this.registerSetting(this.horizontalAimSpeed, this.horizontalAimFineTuning, this.horizontalRandomization, this.horizontalRandomizationAmount, this.fieldOfView, this.enemyDetectionRange, this.verticalAlignmentCheck, this.verticalRandomization, this.verticalRandomizationAmount, this.verticalAimSpeed, this.verticalAimFineTuning, this.clickAim, this.centerAim, this.ignoreFriendlyEntities, this.ignoreTeammates, this.aimAtInvisibleEnemies, this.lineOfSightCheck, this.disableAimWhileBreakingBlock, this.weaponOnly);
    }

    @EventLink
    public void onLiving(LivingEvent e) {
        Block bl;
        BlockPos p;
        if (AimAssist.mc.field_71439_g == null || AimAssist.mc.field_71462_r != null || !AimAssist.mc.field_71415_G) {
            return;
        }
        if (this.disableAimWhileBreakingBlock.isToggled() && AimAssist.mc.field_71476_x != null && (p = AimAssist.mc.field_71476_x.func_178782_a()) != null && (bl = AimAssist.mc.field_71441_e.func_180495_p(p).func_177230_c()) != Blocks.field_150350_a && !(bl instanceof BlockLiquid) && bl instanceof Block) {
            return;
        }
        if (!this.weaponOnly.isToggled() || PlayerUtil.isHoldingWeapon()) {
            Entity enemy;
            AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
            if ((this.clickAim.isToggled() && ClickUtil.instance.isClicking() || Mouse.isButtonDown((int)0) && clicker != null && !clicker.isEnabled() || !this.clickAim.isToggled()) && (enemy = this.getEnemy()) != null) {
                if (this.centerAim.isToggled()) {
                    CombatUtil.instance.aim(enemy, 0.0f);
                }
                double fovEntity = PlayerUtil.fovFromEntity(enemy);
                double complimentHorizontal = fovEntity * (ThreadLocalRandom.current().nextDouble(this.horizontalAimFineTuning.getInput() - 1.47328, this.horizontalAimFineTuning.getInput() + 2.48293) / 100.0);
                float resultHorizontal = (float)(-(complimentHorizontal + fovEntity / (101.0 - (double)((float)ThreadLocalRandom.current().nextDouble(this.horizontalAimSpeed.getInput() - 4.723847, this.horizontalAimSpeed.getInput())))));
                double complimentVertical = fovEntity * (ThreadLocalRandom.current().nextDouble(this.verticalAimFineTuning.getInput() - 1.47328, this.verticalAimFineTuning.getInput() + 2.48293) / 100.0);
                float resultVertical = (float)(-(complimentVertical + fovEntity / (101.0 - (double)((float)ThreadLocalRandom.current().nextDouble(this.verticalAimSpeed.getInput() - 4.723847, this.verticalAimSpeed.getInput())))));
                if (fovEntity > 1.0 || fovEntity < -1.0) {
                    boolean yaw = this.random.nextBoolean();
                    float yawChange = yaw ? -RandomUtils.nextFloat((float)0.0f, (float)this.horizontalRandomizationAmount.getInputToFloat()) : RandomUtils.nextFloat((float)0.0f, (float)this.horizontalRandomizationAmount.getInputToFloat());
                    float yawAdjustment = this.horizontalRandomization.isToggled() ? yawChange : resultHorizontal;
                    AimAssist.mc.field_71439_g.field_70177_z += yawAdjustment;
                }
                if (this.verticalAlignmentCheck.isToggled()) {
                    boolean pitch = this.random.nextBoolean();
                    float pitchChange = pitch ? -RandomUtils.nextFloat((float)0.0f, (float)this.verticalRandomizationAmount.getInputToFloat()) : RandomUtils.nextFloat((float)0.0f, (float)this.verticalRandomizationAmount.getInputToFloat());
                    float pitchAdjustment = this.verticalRandomization.isToggled() ? pitchChange : resultVertical;
                    float newPitch = AimAssist.mc.field_71439_g.field_70125_A + pitchAdjustment;
                    AimAssist.mc.field_71439_g.field_70125_A += pitchAdjustment;
                    AimAssist.mc.field_71439_g.field_70125_A = newPitch >= 90.0f ? newPitch - 180.0f : (newPitch <= -90.0f ? newPitch + 180.0f : newPitch);
                }
            }
        }
    }

    public Entity getEnemy() {
        int fov = (int)this.fieldOfView.getInput();
        ArrayList<EntityPlayer> playerList = new ArrayList<EntityPlayer>(AimAssist.mc.field_71441_e.field_73010_i);
        playerList.sort(new Comparator<EntityPlayer>(){

            @Override
            public int compare(EntityPlayer player1, EntityPlayer player2) {
                if (Loona.mc.field_71439_g.func_70685_l((Entity)player1) && !Loona.mc.field_71439_g.func_70685_l((Entity)player2)) {
                    return -1;
                }
                if (!Loona.mc.field_71439_g.func_70685_l((Entity)player1) && Loona.mc.field_71439_g.func_70685_l((Entity)player2)) {
                    return 1;
                }
                int health1 = (int)player1.func_110143_aJ();
                int health2 = (int)player2.func_110143_aJ();
                return Integer.compare(health1, health2);
            }
        });
        for (EntityPlayer entityPlayer : AimAssist.mc.field_71441_e.field_73010_i) {
            if (entityPlayer == AimAssist.mc.field_71439_g || entityPlayer.field_70725_aQ != 0 || this.isFriend(entityPlayer) && this.ignoreFriendlyEntities.isToggled() || !AimAssist.mc.field_71439_g.func_70685_l((Entity)entityPlayer) && this.lineOfSightCheck.isToggled() || this.isTeamMate(entityPlayer) && this.ignoreTeammates.isToggled() || entityPlayer == AimAssist.mc.field_71439_g || entityPlayer.field_70128_L || this.isNPCShop(entityPlayer) || !this.aimAtInvisibleEnemies.isToggled() && entityPlayer.func_82150_aj() || (double)AimAssist.mc.field_71439_g.func_70032_d((Entity)entityPlayer) > this.enemyDetectionRange.getInput() || !this.centerAim.isToggled() && fov != 360 && !this.isWithinFOV(entityPlayer, fov)) continue;
            return entityPlayer;
        }
        return null;
    }

    private boolean isFriend(EntityPlayer player) {
        return this.ignoreFriendlyEntities.isToggled() && FriendUtil.instance.isAFriend((Entity)player);
    }

    private boolean isTeamMate(EntityPlayer player) {
        return this.ignoreTeammates.isToggled() && CombatUtil.instance.isATeamMate((Entity)player);
    }

    private boolean isNPCShop(EntityPlayer player) {
        return player.func_70005_c_().matches("[\\[\u00a7]?[NPC] ?\\]?|\u00a7a?Shop|SHOP|UPGRADES");
    }

    private boolean isWithinFOV(EntityPlayer player, int fieldOfView) {
        return PlayerUtil.fov((Entity)player, fieldOfView);
    }
}

