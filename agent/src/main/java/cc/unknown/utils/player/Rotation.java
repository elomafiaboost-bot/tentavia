/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.util.MathHelper
 */
package cc.unknown.utils.player;

import cc.unknown.event.impl.player.StrafeEvent;
import cc.unknown.utils.player.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;

public class Rotation {
    public static Rotation instance;
    public float yaw;
    public float pitch;

    public Rotation() {
        instance = this;
    }

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float f) {
        this.pitch = f;
    }

    public Rotation fixedSensitivity(float sensitivity) {
        float gcd = this.getFixedAngleDelta(sensitivity);
        this.yaw = this.getFixedSensitivityAngle(this.yaw, RotationUtils.serverRotation.getYaw(), gcd);
        this.pitch = MathHelper.func_76131_a((float)this.getFixedSensitivityAngle(this.pitch, RotationUtils.serverRotation.getPitch(), gcd), (float)-90.0f, (float)90.0f);
        return this;
    }

    private float getFixedAngleDelta(float sensitivity) {
        return (float)Math.pow(sensitivity * 0.6f + 0.2f, 3.0) * 1.2f;
    }

    private float getFixedSensitivityAngle(float targetAngle, float startAngle, float gcd) {
        return startAngle + (float)Math.round((targetAngle - startAngle) / gcd) * gcd;
    }

    public void applyStrafeToPlayer(StrafeEvent event, boolean strict) {
        float d;
        float calcStrafe;
        float calcForward;
        Minecraft mc = Minecraft.func_71410_x();
        EntityPlayerSP player = mc.field_71439_g;
        float yawDifference = MathHelper.func_76142_g((float)(player.field_70177_z - this.yaw - 23.5f - 135.0f)) + 180.0f;
        int diff = (int)(yawDifference / 45.0f);
        float strafe = event.getStrafe();
        float forward = event.getForward();
        float friction = event.getFriction();
        if (!strict) {
            switch (diff) {
                case 0: {
                    calcForward = forward;
                    calcStrafe = strafe;
                    break;
                }
                case 1: {
                    calcForward = forward + strafe;
                    calcStrafe = strafe - forward;
                    break;
                }
                case 2: {
                    calcForward = strafe;
                    calcStrafe = -forward;
                    break;
                }
                case 3: {
                    calcForward = forward - strafe;
                    calcStrafe = -forward - strafe;
                    break;
                }
                case 4: {
                    calcForward = -forward;
                    calcStrafe = -strafe;
                    break;
                }
                case 5: {
                    calcForward = -forward - strafe;
                    calcStrafe = strafe - forward;
                    break;
                }
                case 6: {
                    calcForward = -strafe;
                    calcStrafe = forward;
                    break;
                }
                case 7: {
                    calcForward = forward + strafe;
                    calcStrafe = forward + strafe;
                    break;
                }
                default: {
                    calcForward = forward;
                    calcStrafe = strafe;
                }
            }
            if (Math.abs(calcForward) > 1.0f || Math.abs(calcForward) > 0.3f && Math.abs(calcForward) < 0.9f) {
                calcForward *= 0.5f;
            }
            if (Math.abs(calcStrafe) > 1.0f || Math.abs(calcStrafe) > 0.3f && Math.abs(calcStrafe) < 0.9f) {
                calcStrafe *= 0.5f;
            }
        } else {
            calcForward = event.getForward();
            calcStrafe = event.getStrafe();
        }
        if ((d = calcStrafe * calcStrafe + calcForward * calcForward) >= 1.0E-4f) {
            d = friction / MathHelper.func_76129_c((float)d);
            float yawRad = (float)Math.toRadians(this.yaw);
            float yawSin = MathHelper.func_76126_a((float)yawRad);
            float yawCos = MathHelper.func_76134_b((float)yawRad);
            player.field_70159_w += (double)(calcStrafe * yawCos - calcForward * yawSin);
            player.field_70179_y += (double)(calcForward * yawCos + calcStrafe * yawSin);
        }
    }
}

