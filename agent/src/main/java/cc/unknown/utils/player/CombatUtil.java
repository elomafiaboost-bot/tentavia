/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityArmorStand
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.MathHelper
 *  net.minecraft.util.MovingObjectPosition
 *  net.minecraft.util.MovingObjectPosition$MovingObjectType
 *  net.minecraft.util.Vec3
 */
package cc.unknown.utils.player;

import cc.unknown.utils.Loona;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public enum CombatUtil implements Loona
{
    instance;

    public float yaw;
    public float pitch;

    public boolean canTarget(Entity entity, boolean idk) {
        if (entity != null && entity != CombatUtil.mc.field_71439_g) {
            EntityLivingBase entityLivingBase = null;
            if (entity instanceof EntityLivingBase) {
                entityLivingBase = (EntityLivingBase)entity;
            }
            boolean isTeam = this.isTeam((EntityPlayer)CombatUtil.mc.field_71439_g, entity);
            return !(entity instanceof EntityArmorStand) && (entity instanceof EntityPlayer && !isTeam && !entity.func_82150_aj() && !idk || entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityLivingBase && entityLivingBase.func_70089_S());
        }
        return false;
    }

    public boolean isTeam(EntityPlayer player, Entity entity) {
        if (entity instanceof EntityPlayer && ((EntityPlayer)entity).func_96124_cp() != null && player.func_96124_cp() != null) {
            Character entity_3 = Character.valueOf(entity.func_145748_c_().func_150254_d().charAt(3));
            Character player_3 = Character.valueOf(player.func_145748_c_().func_150254_d().charAt(3));
            Character entity_2 = Character.valueOf(entity.func_145748_c_().func_150254_d().charAt(2));
            Character player_2 = Character.valueOf(player.func_145748_c_().func_150254_d().charAt(2));
            boolean isTeam = false;
            if (entity_3.equals(player_3) && entity_2.equals(player_2)) {
                isTeam = true;
            } else {
                Character entity_1 = Character.valueOf(entity.func_145748_c_().func_150254_d().charAt(1));
                Character player_1 = Character.valueOf(player.func_145748_c_().func_150254_d().charAt(1));
                Character entity_0 = Character.valueOf(entity.func_145748_c_().func_150254_d().charAt(0));
                Character player_0 = Character.valueOf(player.func_145748_c_().func_150254_d().charAt(0));
                if (entity_1.equals(player_1) && Character.isDigit(0) && entity_0.equals(player_0)) {
                    isTeam = true;
                }
            }
            return isTeam;
        }
        return true;
    }

    public float rotsToFloat(float[] rots, int m) {
        if (m == 1) {
            return rots[0];
        }
        if (m == 2) {
            return rots[1] + 4.0f;
        }
        return -1.0f;
    }

    public void aim(Entity en, float offset) {
        float[] rots;
        if (en != null && (rots = this.getTargetRotations(en)) != null) {
            float yaw = this.rotsToFloat(rots, 1);
            float pitch = this.rotsToFloat(rots, 2) + 4.0f + offset;
            CombatUtil.mc.field_71439_g.field_70177_z = yaw;
            CombatUtil.mc.field_71439_g.field_70125_A = pitch;
        }
    }

    public float[] getTargetRotations(Entity en) {
        double diffY;
        if (en == null) {
            return null;
        }
        double diffX = en.field_70165_t - CombatUtil.mc.field_71439_g.field_70165_t;
        if (en instanceof EntityLivingBase) {
            EntityLivingBase x = (EntityLivingBase)en;
            diffY = x.field_70163_u + (double)x.func_70047_e() * 0.9 - (CombatUtil.mc.field_71439_g.field_70163_u + (double)CombatUtil.mc.field_71439_g.func_70047_e());
        } else {
            diffY = (en.func_174813_aQ().field_72338_b + en.func_174813_aQ().field_72337_e) / 2.0 - (CombatUtil.mc.field_71439_g.field_70163_u + (double)CombatUtil.mc.field_71439_g.func_70047_e());
        }
        double diffZ = en.field_70161_v - CombatUtil.mc.field_71439_g.field_70161_v;
        float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(diffY, MathHelper.func_76133_a((double)(diffX * diffX + diffZ * diffZ))) * 180.0 / Math.PI));
        return new float[]{CombatUtil.mc.field_71439_g.field_70177_z + MathHelper.func_76142_g((float)(yaw - CombatUtil.mc.field_71439_g.field_70177_z)), CombatUtil.mc.field_71439_g.field_70125_A + MathHelper.func_76142_g((float)(pitch - CombatUtil.mc.field_71439_g.field_70125_A))};
    }

    public void aimAt(float pitch, float yaw, float fuckedYaw, float fuckedPitch, double speed) {
        float cappedPitch;
        float[] gcd = this.getPatchedRots(new float[]{yaw, pitch + (float)((int)fuckedPitch / 360 * 360)}, new float[]{CombatUtil.mc.field_71439_g.field_70126_B, CombatUtil.mc.field_71439_g.field_70127_C});
        float cappedYaw = this.maxAngleChange(CombatUtil.mc.field_71439_g.field_70126_B, gcd[0], (float)speed);
        CombatUtil.mc.field_71439_g.field_70125_A = cappedPitch = this.maxAngleChange(CombatUtil.mc.field_71439_g.field_70127_C, gcd[1], (float)speed);
        CombatUtil.mc.field_71439_g.field_70177_z = cappedYaw;
    }

    public float[] getPatchedRots(float[] currentRots, float[] prevRots) {
        float yawDif = currentRots[0] - prevRots[0];
        float pitchDif = currentRots[1] - prevRots[1];
        double gcd = this.mouseSens();
        currentRots[0] = currentRots[0] - (float)((double)yawDif % gcd);
        currentRots[1] = currentRots[1] - (float)((double)pitchDif % gcd);
        return currentRots;
    }

    public float maxAngleChange(float prev, float now, float maxTurn) {
        float dif = MathHelper.func_76142_g((float)(now - prev));
        if (dif > maxTurn) {
            dif = maxTurn;
        }
        if (dif < -maxTurn) {
            dif = -maxTurn;
        }
        return prev + dif;
    }

    public double mouseSens() {
        float sens = CombatUtil.mc.field_71474_y.field_74341_c * 0.6f + 0.2f;
        float pow = sens * sens * sens * 8.0f;
        return (double)pow * 0.15;
    }

    public boolean isATeamMate(Entity entity) {
        EntityPlayer teamMate = (EntityPlayer)entity;
        return CombatUtil.mc.field_71439_g.func_142014_c((EntityLivingBase)entity) || CombatUtil.mc.field_71439_g.func_145748_c_().func_150260_c().startsWith(teamMate.func_145748_c_().func_150260_c().substring(0, 2));
    }

    public double getDistanceToEntityBox(Entity entity1) {
        Vec3 eyes = entity1.func_174824_e(1.0f);
        Vec3 pos = this.getNearestPointBB(eyes, entity1.func_174813_aQ());
        double xDist = Math.abs(pos.field_72450_a - eyes.field_72450_a);
        double yDist = Math.abs(pos.field_72448_b - eyes.field_72448_b);
        double zDist = Math.abs(pos.field_72449_c - eyes.field_72449_c);
        return Math.sqrt(Math.pow(xDist, 2.0) + Math.pow(yDist, 2.0) + Math.pow(zDist, 2.0));
    }

    public Vec3 getNearestPointBB(Vec3 eye, AxisAlignedBB box) {
        double[] origin = new double[]{eye.field_72450_a, eye.field_72448_b, eye.field_72449_c};
        double[] destMins = new double[]{box.field_72340_a, box.field_72338_b, box.field_72339_c};
        double[] destMaxs = new double[]{box.field_72336_d, box.field_72337_e, box.field_72334_f};
        for (int i = 0; i < 3; ++i) {
            if (origin[i] > destMaxs[i]) {
                origin[i] = destMaxs[i];
                continue;
            }
            if (!(origin[i] < destMins[i])) continue;
            origin[i] = destMins[i];
        }
        return new Vec3(origin[0], origin[1], origin[2]);
    }

    public int getPing(EntityPlayer e) {
        return mc.func_147114_u().func_175102_a(e.func_110124_au()) != null ? mc.func_147114_u().func_175102_a(e.func_110124_au()).func_178853_c() : 0;
    }

    public double getNearestPointBB(AxisAlignedBB bb) {
        Vec3 eyes = CombatUtil.mc.field_71439_g.func_174824_e(1.0f);
        Vec3 vecRotation3d = null;
        for (double xSearch = 0.0; xSearch <= 1.0; xSearch += 0.05) {
            for (double ySearch = 0.0; ySearch < 1.0; ySearch += 0.05) {
                for (double zSearch = 0.0; zSearch <= 1.0; zSearch += 0.05) {
                    Vec3 vec3 = new Vec3(bb.field_72340_a + (bb.field_72336_d - bb.field_72340_a) * xSearch, bb.field_72338_b + (bb.field_72337_e - bb.field_72338_b) * ySearch, bb.field_72339_c + (bb.field_72334_f - bb.field_72339_c) * zSearch);
                    double vecDist = eyes.func_72436_e(vec3);
                    if (vecRotation3d != null && !(eyes.func_72436_e(vecRotation3d) > vecDist)) continue;
                    vecRotation3d = vec3;
                }
            }
        }
        return vecRotation3d.func_72438_d(eyes);
    }

    public double getLookingTargetRange(EntityPlayerSP thePlayer, AxisAlignedBB bb) {
        return this.getLookingTargetRange(thePlayer, bb, 6.0);
    }

    public double getLookingTargetRange(EntityPlayerSP thePlayer, AxisAlignedBB bb, double range) {
        Vec3 direction;
        Vec3 adjustedDirection;
        Vec3 target;
        Vec3 eyes = thePlayer.func_174824_e(1.0f);
        MovingObjectPosition movingObj = bb.func_72327_a(eyes, target = (adjustedDirection = CombatUtil.multiply(direction = this.getVectorForRotation(), range)).func_178787_e(eyes));
        return movingObj != null ? movingObj.field_72307_f.func_72438_d(eyes) : Double.MAX_VALUE;
    }

    public static Vec3 multiply(Vec3 vec, double value) {
        return new Vec3(vec.field_72450_a * value, vec.field_72448_b * value, vec.field_72449_c * value);
    }

    public Vec3 getVectorForRotation() {
        float f = MathHelper.func_76134_b((float)(-this.yaw * ((float)Math.PI / 180) - (float)Math.PI));
        float f1 = MathHelper.func_76126_a((float)(-this.yaw * ((float)Math.PI / 180) - (float)Math.PI));
        float f2 = -MathHelper.func_76134_b((float)(-this.pitch * ((float)Math.PI / 180)));
        float f3 = MathHelper.func_76126_a((float)(-this.pitch * ((float)Math.PI / 180)));
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }

    public final Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.func_76134_b((float)(-yaw * ((float)Math.PI / 180) - (float)Math.PI));
        float f1 = MathHelper.func_76126_a((float)(-yaw * ((float)Math.PI / 180) - (float)Math.PI));
        float f2 = -MathHelper.func_76134_b((float)(-pitch * ((float)Math.PI / 180)));
        float f3 = MathHelper.func_76126_a((float)(-pitch * ((float)Math.PI / 180)));
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }

    public MovingObjectPosition rayCastedBlock(float yaw, float pitch) {
        Vec3 vec32;
        float range = CombatUtil.mc.field_71442_b.func_78757_d();
        Vec3 vec31 = this.getVectorForRotation(pitch, yaw);
        Vec3 vec3 = CombatUtil.mc.field_71439_g.func_174824_e(1.0f);
        MovingObjectPosition ray = CombatUtil.mc.field_71441_e.func_147447_a(vec3, vec32 = vec3.func_72441_c(vec31.field_72450_a * (double)range, vec31.field_72448_b * (double)range, vec31.field_72449_c * (double)range), false, false, false);
        if (ray != null && ray.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK) {
            return ray;
        }
        return null;
    }
}

