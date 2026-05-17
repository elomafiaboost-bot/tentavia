/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.C03PacketPlayer
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.MathHelper
 *  net.minecraft.util.Vec3
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package cc.unknown.utils.player;

import cc.unknown.Haru;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.utils.Loona;
import cc.unknown.utils.player.Rotation;
import java.util.Random;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(value=Side.CLIENT)
public class RotationUtils
implements Loona {
    private static Random random = new Random();
    private static int keepLength;
    private static int revTick;
    public static Rotation targetRotation;
    public static Rotation serverRotation;
    public static float[] clientRotation;
    public static boolean keepCurrentRotation;
    public double x = random.nextDouble();
    public double y = random.nextDouble();
    public double z = random.nextDouble();

    public RotationUtils() {
        Haru.instance.getEventBus().register(this);
    }

    public static Rotation getRotationsEntity(EntityLivingBase entity) {
        return RotationUtils.getRotations(entity.field_70165_t, entity.field_70163_u + (double)entity.func_70047_e() - 0.4, entity.field_70161_v);
    }

    public static Rotation toRotation(Vec3 vec, boolean predict) {
        Vec3 eyesPos = new Vec3(RotationUtils.mc.field_71439_g.field_70165_t, RotationUtils.mc.field_71439_g.func_174813_aQ().field_72338_b + (double)RotationUtils.mc.field_71439_g.func_70047_e(), RotationUtils.mc.field_71439_g.field_70161_v);
        if (predict) {
            if (RotationUtils.mc.field_71439_g.field_70122_E) {
                eyesPos.func_72441_c(RotationUtils.mc.field_71439_g.field_70159_w, 0.0, RotationUtils.mc.field_71439_g.field_70179_y);
            } else {
                eyesPos.func_72441_c(RotationUtils.mc.field_71439_g.field_70159_w, RotationUtils.mc.field_71439_g.field_70181_x, RotationUtils.mc.field_71439_g.field_70179_y);
            }
        }
        double diffX = vec.field_72450_a - eyesPos.field_72450_a;
        double diffY = vec.field_72448_b - eyesPos.field_72448_b;
        double diffZ = vec.field_72449_c - eyesPos.field_72449_c;
        return new Rotation(MathHelper.func_76142_g((float)((float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f)), MathHelper.func_76142_g((float)((float)(-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)))))));
    }

    public static Vec3 getCenter(AxisAlignedBB bb) {
        return new Vec3(bb.field_72340_a + (bb.field_72336_d - bb.field_72340_a) * 0.5, bb.field_72338_b + (bb.field_72337_e - bb.field_72338_b) * 0.5, bb.field_72339_c + (bb.field_72334_f - bb.field_72339_c) * 0.5);
    }

    public static double getRotationDifference(Entity entity) {
        Rotation rotation = RotationUtils.toRotation(RotationUtils.getCenter(entity.func_174813_aQ()), true);
        return RotationUtils.getRotationDifference(rotation, new Rotation(RotationUtils.mc.field_71439_g.field_70177_z, RotationUtils.mc.field_71439_g.field_70125_A));
    }

    public static double getRotationDifference(Rotation rotation) {
        return serverRotation == null ? 0.0 : RotationUtils.getRotationDifference(rotation, serverRotation);
    }

    public static double getRotationDifference(Rotation a, Rotation b) {
        return Math.hypot(RotationUtils.getAngleDifference(a.getYaw(), b.getYaw()), a.getPitch() - b.getPitch());
    }

    public static Rotation limitAngleChange(Rotation currentRotation, Rotation targetRotation, float turnSpeed) {
        float yawDifference = RotationUtils.getAngleDifference(targetRotation.getYaw(), currentRotation.getYaw());
        float pitchDifference = RotationUtils.getAngleDifference(targetRotation.getPitch(), currentRotation.getPitch());
        return new Rotation(currentRotation.getYaw() + (yawDifference > turnSpeed ? turnSpeed : Math.max(yawDifference, -turnSpeed)), currentRotation.getPitch() + (pitchDifference > turnSpeed ? turnSpeed : Math.max(pitchDifference, -turnSpeed)));
    }

    public static float getAngleDifference(float a, float b) {
        return ((a - b) % 360.0f + 540.0f) % 360.0f - 180.0f;
    }

    @EventLink
    public void onTick(TickEvent event) {
        if (targetRotation != null && --keepLength <= 0) {
            if (revTick > 0) {
                --revTick;
                RotationUtils.reset();
            } else {
                RotationUtils.reset();
            }
        }
        if (random.nextGaussian() > 0.8) {
            this.x = Math.random();
        }
        if (random.nextGaussian() > 0.8) {
            this.y = Math.random();
        }
        if (random.nextGaussian() > 0.8) {
            this.z = Math.random();
        }
    }

    @EventLink
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (event.isSend() && packet instanceof C03PacketPlayer) {
            C03PacketPlayer packetPlayer = (C03PacketPlayer)packet;
            if (!(targetRotation == null || keepCurrentRotation || targetRotation.getYaw() == serverRotation.getYaw() && targetRotation.getPitch() == serverRotation.getPitch())) {
                packetPlayer.field_149476_e = targetRotation.getYaw();
                packetPlayer.field_149473_f = targetRotation.getPitch();
                packetPlayer.field_149481_i = true;
            }
            if (packetPlayer.field_149481_i) {
                serverRotation = new Rotation(packetPlayer.field_149476_e, packetPlayer.field_149473_f);
            }
        }
    }

    public static void setTargetRotation(Rotation rotation) {
        RotationUtils.setTargetRotation(rotation, 0);
    }

    public static void setTargetRotation(Rotation rotation, int c) {
        if (Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch()) || rotation.getPitch() > 90.0f || rotation.getPitch() < -90.0f) {
            return;
        }
        rotation.fixedSensitivity(RotationUtils.mc.field_71474_y.field_74341_c);
        targetRotation = rotation;
        keepLength = c;
        revTick = 0;
    }

    public static void reset() {
        keepLength = 0;
        targetRotation = revTick > 0 ? new Rotation(targetRotation.getYaw() - RotationUtils.getAngleDifference(targetRotation.getYaw(), RotationUtils.mc.field_71439_g.field_70177_z) / (float)revTick, targetRotation.getPitch() - RotationUtils.getAngleDifference(targetRotation.getPitch(), RotationUtils.mc.field_71439_g.field_70125_A) / (float)revTick) : null;
    }

    public static Rotation getRotations(Entity ent) {
        double x = ent.field_70165_t;
        double z = ent.field_70161_v;
        double y = ent.field_70163_u + (double)(ent.func_70047_e() / 2.0f);
        return RotationUtils.getRotationFromPosition(x, z, y);
    }

    public static Rotation getRotations(double posX, double posY, double posZ) {
        EntityPlayerSP player = RotationUtils.mc.field_71439_g;
        double x = posX - player.field_70165_t;
        double y = posY - (player.field_70163_u + (double)player.func_70047_e());
        double z = posZ - player.field_70161_v;
        double dist = MathHelper.func_76133_a((double)(x * x + z * z));
        float yaw = (float)(Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(y, dist) * 180.0 / Math.PI));
        return new Rotation(yaw, pitch);
    }

    public static Rotation getRotationFromPosition(double x, double z, double y) {
        double xDiff = x - RotationUtils.mc.field_71439_g.field_70165_t;
        double zDiff = z - RotationUtils.mc.field_71439_g.field_70161_v;
        double yDiff = y - RotationUtils.mc.field_71439_g.field_70163_u - 1.2;
        double dist = MathHelper.func_76133_a((double)(xDiff * xDiff + zDiff * zDiff));
        float yaw = (float)(Math.atan2(zDiff, xDiff) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-Math.atan2(yDiff, dist) * 180.0 / Math.PI);
        return new Rotation(yaw, pitch);
    }

    public static Rotation getTargetRotation() {
        return targetRotation;
    }

    public static Rotation getServerRotation() {
        return serverRotation;
    }

    static {
        serverRotation = new Rotation(0.0f, 0.0f);
        clientRotation = new float[]{0.0f, 0.0f};
        keepCurrentRotation = false;
    }
}

