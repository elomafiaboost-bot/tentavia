/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.MathHelper
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MoveEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.helpers.MathHelper;
import cc.unknown.utils.player.CombatUtil;
import cc.unknown.utils.player.PlayerUtil;

@Register(name="BridgeAssist", category=Category.Player)
public class BridgeAssist
extends Module {
    private boolean waitingForAim;
    private boolean gliding;
    private long startWaitTime;
    private final float[] godbridgePos = new float[]{75.6f, -315.0f, -225.0f, -135.0f, -45.0f, 0.0f, 45.0f, 135.0f, 225.0f, 315.0f};
    private final float[] moonwalkPos = new float[]{79.6f, -340.0f, -290.0f, -250.0f, -200.0f, -160.0f, -110.0f, -70.0f, -20.0f, 0.0f, 20.0f, 70.0f, 110.0f, 160.0f, 200.0f, 250.0f, 290.0f, 340.0f};
    private final float[] breezilyPos = new float[]{79.9f, -360.0f, -270.0f, -180.0f, -90.0f, 0.0f, 90.0f, 180.0f, 270.0f, 360.0f};
    private final float[] normalPos = new float[]{78.0f, -315.0f, -225.0f, -135.0f, -45.0f, 0.0f, 45.0f, 135.0f, 225.0f, 315.0f};
    private double speedYaw;
    private double speedPitch;
    private float waitingForYaw;
    private float waitingForPitch;
    private ModeValue assistMode = new ModeValue("Assist Mode", "Basic", "God Bridge", "Moon Walk", "Breezily", "Basic");
    private SliderValue assistChance = new SliderValue("Assist Range", 38.0, 1.0, 40.0, 1.0);
    private SliderValue speedAngle = new SliderValue("Angle Speed", 50.0, 1.0, 100.0, 1.0);
    private SliderValue waitFor = new SliderValue("Wait Time", 70.0, 0.0, 200.0, 1.0);
    private BooleanValue onlySneaking = new BooleanValue("Only While Sneaking", false);
    private BooleanValue enableSafeWalk = new BooleanValue("Enable SafeWalk", true);
    private BooleanValue safeInAir = new BooleanValue("Safe in Air", false);

    public BridgeAssist() {
        this.registerSetting(this.assistMode, this.assistChance, this.waitFor, this.speedAngle, this.onlySneaking, this.enableSafeWalk, this.safeInAir);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.assistMode.getMode() + "]");
    }

    @Override
    public void onEnable() {
        this.waitingForAim = false;
        this.gliding = false;
        super.onEnable();
    }

    @EventLink
    public void onSafe(MoveEvent e) {
        if (this.enableSafeWalk.isToggled() && BridgeAssist.mc.field_71439_g.field_70122_E || this.safeInAir.isToggled() && PlayerUtil.playerOverAir()) {
            e.setSaveWalk(true);
        }
    }

    @EventLink
    public void onRender(RenderEvent e) {
        if (e.is3D()) {
            if (!PlayerUtil.inGame() || !PlayerUtil.playerOverAir() && BridgeAssist.mc.field_71439_g.field_70122_E || this.onlySneaking.isToggled() && !BridgeAssist.mc.field_71439_g.func_70093_af()) {
                return;
            }
            if (this.gliding) {
                float yaw = net.minecraft.util.MathHelper.func_76142_g((float)BridgeAssist.mc.field_71439_g.field_70177_z);
                float pitch = MathHelper.wrapAngleTo90_float(BridgeAssist.mc.field_71439_g.field_70125_A);
                double d0 = Math.abs((double)yaw - this.speedYaw);
                double d1 = Math.abs((double)yaw + this.speedYaw);
                double d2 = Math.abs((double)pitch - this.speedPitch);
                double d3 = Math.abs((double)pitch + this.speedPitch);
                if (this.speedYaw > d0 || this.speedYaw > d1 || this.speedPitch > d2 || this.speedPitch > d3) {
                    BridgeAssist.mc.field_71439_g.field_70177_z = this.waitingForYaw;
                    BridgeAssist.mc.field_71439_g.field_70125_A = this.waitingForPitch;
                } else {
                    BridgeAssist.mc.field_71439_g.field_70177_z = (float)((double)BridgeAssist.mc.field_71439_g.field_70177_z + (BridgeAssist.mc.field_71439_g.field_70177_z < this.waitingForYaw ? this.speedYaw : -this.speedYaw));
                    BridgeAssist.mc.field_71439_g.field_70125_A = (float)((double)BridgeAssist.mc.field_71439_g.field_70125_A + (BridgeAssist.mc.field_71439_g.field_70125_A < this.waitingForPitch ? this.speedPitch : -this.speedPitch));
                }
                if (BridgeAssist.mc.field_71439_g.field_70177_z == this.waitingForYaw && BridgeAssist.mc.field_71439_g.field_70125_A == this.waitingForPitch) {
                    this.gliding = false;
                    this.waitingForAim = false;
                }
                return;
            }
            if (!this.waitingForAim) {
                this.waitingForAim = true;
                this.startWaitTime = System.currentTimeMillis();
                return;
            }
            if ((double)(System.currentTimeMillis() - this.startWaitTime) < this.waitFor.getInput()) {
                return;
            }
            float yaw = net.minecraft.util.MathHelper.func_76142_g((float)BridgeAssist.mc.field_71439_g.field_70177_z);
            float pitch = MathHelper.wrapAngleTo90_float(BridgeAssist.mc.field_71439_g.field_70125_A);
            float range = (float)this.assistChance.getInput();
            float[] positions = null;
            switch (this.assistMode.getMode()) {
                case "God Bridge": {
                    positions = this.godbridgePos;
                    break;
                }
                case "Moon Walk": {
                    positions = this.moonwalkPos;
                    break;
                }
                case "Breezily": {
                    positions = this.breezilyPos;
                    break;
                }
                case "Basic": {
                    positions = this.normalPos;
                }
            }
            if (positions != null && positions.length > 0 && positions[0] >= pitch - range && positions[0] <= pitch + range) {
                for (int k = 1; k < positions.length; ++k) {
                    if (!(positions[k] >= yaw - range) || !(positions[k] <= yaw + range)) continue;
                    CombatUtil.instance.aimAt(positions[0], positions[k], BridgeAssist.mc.field_71439_g.field_70177_z, BridgeAssist.mc.field_71439_g.field_70125_A, this.speedAngle.getInput());
                    this.waitingForAim = false;
                    return;
                }
            }
            this.waitingForAim = false;
        }
    }
}

