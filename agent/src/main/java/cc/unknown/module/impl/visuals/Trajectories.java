/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.WorldRenderer
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.item.ItemBow
 *  net.minecraft.item.ItemEgg
 *  net.minecraft.item.ItemEnderPearl
 *  net.minecraft.item.ItemSnowball
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.MathHelper
 *  net.minecraft.util.MovingObjectPosition
 *  net.minecraft.util.Vec3
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.SliderValue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

@Register(name="Trajectories", category=Category.Visuals)
public class Trajectories
extends Module {
    private SliderValue color = new SliderValue("Color [H/S/B]", 0.0, 0.0, 350.0, 10.0);
    private final ArrayList<Vec3> positions = new ArrayList();

    public Trajectories() {
        this.registerSetting(this.color);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.positions.clear();
    }

    @EventLink
    public void onRender3D(RenderEvent e) {
        if (e.is3D()) {
            this.positions.clear();
            ItemStack itemStack = Trajectories.mc.field_71439_g.func_71045_bC();
            MovingObjectPosition m = null;
            if (itemStack != null && (itemStack.func_77973_b() instanceof ItemSnowball || itemStack.func_77973_b() instanceof ItemEgg || itemStack.func_77973_b() instanceof ItemBow || itemStack.func_77973_b() instanceof ItemEnderPearl)) {
                float prevRotationPitch;
                float prevRotationYaw;
                EntityPlayerSP thrower = Trajectories.mc.field_71439_g;
                float rotationYaw = thrower.field_70126_B + (thrower.field_70177_z - thrower.field_70126_B) * Trajectories.mc.field_71428_T.field_74281_c;
                float rotationPitch = thrower.field_70127_C + (thrower.field_70125_A - thrower.field_70127_C) * Trajectories.mc.field_71428_T.field_74281_c;
                double posX = thrower.field_70142_S + (thrower.field_70165_t - thrower.field_70142_S) * (double)Trajectories.mc.field_71428_T.field_74281_c;
                double posY = thrower.field_70137_T + (double)thrower.func_70047_e() + (thrower.field_70163_u - thrower.field_70137_T) * (double)Trajectories.mc.field_71428_T.field_74281_c;
                double posZ = thrower.field_70136_U + (thrower.field_70161_v - thrower.field_70136_U) * (double)Trajectories.mc.field_71428_T.field_74281_c;
                posX -= (double)(MathHelper.func_76134_b((float)(rotationYaw / 180.0f * (float)Math.PI)) * 0.16f);
                posY -= (double)0.1f;
                posZ -= (double)(MathHelper.func_76126_a((float)(rotationYaw / 180.0f * (float)Math.PI)) * 0.16f);
                float multipicator = 0.4f;
                if (itemStack.func_77973_b() instanceof ItemBow) {
                    multipicator = 1.0f;
                }
                double motionX = -MathHelper.func_76126_a((float)(rotationYaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(rotationPitch / 180.0f * (float)Math.PI)) * multipicator;
                double motionZ = MathHelper.func_76134_b((float)(rotationYaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(rotationPitch / 180.0f * (float)Math.PI)) * multipicator;
                double motionY = -MathHelper.func_76126_a((float)(rotationPitch / 180.0f * (float)Math.PI)) * multipicator;
                double x = motionX;
                double y = motionY;
                double z = motionZ;
                float inaccuracy = 0.0f;
                float velocity = 1.5f;
                if (itemStack.func_77973_b() instanceof ItemBow) {
                    int i = Trajectories.mc.field_71439_g.func_71057_bx() - Trajectories.mc.field_71439_g.func_71052_bv();
                    float f = (float)i / 20.0f;
                    if ((double)(f = (f * f + f * 2.0f) / 3.0f) < 0.1) {
                        return;
                    }
                    if (f > 1.0f) {
                        f = 1.0f;
                    }
                    velocity = f * 2.0f * 1.5f;
                }
                Random rand = new Random();
                float ff = MathHelper.func_76133_a((double)(x * x + y * y + z * z));
                x /= (double)ff;
                y /= (double)ff;
                z /= (double)ff;
                x += rand.nextGaussian() * (double)0.0075f * 0.0;
                y += rand.nextGaussian() * (double)0.0075f * 0.0;
                z += rand.nextGaussian() * (double)0.0075f * 0.0;
                motionX = x *= (double)velocity;
                motionY = y *= (double)velocity;
                motionZ = z *= (double)velocity;
                rotationYaw = prevRotationYaw = (float)(MathHelper.func_181159_b((double)x, (double)z) * 180.0 / Math.PI);
                rotationPitch = prevRotationPitch = (float)(MathHelper.func_181159_b((double)y, (double)MathHelper.func_76133_a((double)(x * x + z * z))) * 180.0 / Math.PI);
                boolean b = true;
                int ticksInAir = 0;
                while (b) {
                    if (ticksInAir > 300) {
                        b = false;
                    }
                    ++ticksInAir;
                    Vec3 vec3 = new Vec3(posX, posY, posZ);
                    Vec3 vec4 = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
                    MovingObjectPosition movingobjectposition = Trajectories.mc.field_71441_e.func_72933_a(vec3, vec4);
                    vec3 = new Vec3(posX, posY, posZ);
                    vec4 = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
                    if (movingobjectposition != null) {
                        vec4 = new Vec3(movingobjectposition.field_72307_f.field_72450_a, movingobjectposition.field_72307_f.field_72448_b, movingobjectposition.field_72307_f.field_72449_c);
                    }
                    for (Entity entity : Trajectories.mc.field_71441_e.field_72996_f) {
                        if (entity == Trajectories.mc.field_71439_g || !(entity instanceof EntityLivingBase)) continue;
                        float f2 = 0.3f;
                        AxisAlignedBB localAxisAlignedBB = entity.func_174813_aQ().func_72314_b((double)0.3f, (double)0.3f, (double)0.3f);
                        MovingObjectPosition localMovingObjectPosition = localAxisAlignedBB.func_72327_a(vec3, vec4);
                        if (localMovingObjectPosition == null) continue;
                        movingobjectposition = localMovingObjectPosition;
                        break;
                    }
                    if (movingobjectposition != null) {
                        b = false;
                    }
                    m = movingobjectposition;
                    posX += motionX;
                    posY += motionY;
                    posZ += motionZ;
                    float f3 = MathHelper.func_76133_a((double)(motionX * motionX + motionZ * motionZ));
                    rotationYaw = (float)(MathHelper.func_181159_b((double)motionX, (double)motionZ) * 180.0 / Math.PI);
                    rotationPitch = (float)(MathHelper.func_181159_b((double)motionY, (double)f3) * 180.0 / Math.PI);
                    while (rotationPitch - prevRotationPitch < -180.0f) {
                        prevRotationPitch -= 360.0f;
                    }
                    while (rotationPitch - prevRotationPitch >= 180.0f) {
                        prevRotationPitch += 360.0f;
                    }
                    while (rotationYaw - prevRotationYaw < -180.0f) {
                        prevRotationYaw -= 360.0f;
                    }
                    while (rotationYaw - prevRotationYaw >= 180.0f) {
                        prevRotationYaw += 360.0f;
                    }
                    float f4 = 0.99f;
                    float f5 = 0.03f;
                    if (itemStack.func_77973_b() instanceof ItemBow) {
                        f5 = 0.05f;
                    }
                    motionX *= (double)0.99f;
                    motionY *= (double)0.99f;
                    motionZ *= (double)0.99f;
                    motionY -= (double)f5;
                    this.positions.add(new Vec3(posX, posY, posZ));
                }
                if (this.positions.size() > 1) {
                    Color col = Color.getHSBColor(this.color.getInputToFloat() % 360.0f / 360.0f, 1.0f, 1.0f);
                    GL11.glEnable((int)3042);
                    GL11.glBlendFunc((int)770, (int)771);
                    GL11.glEnable((int)2848);
                    GL11.glDisable((int)3553);
                    GlStateManager.func_179129_p();
                    GL11.glDepthMask((boolean)false);
                    GL11.glColor4f((float)((float)col.getRed() / 255.0f), (float)((float)col.getGreen() / 255.0f), (float)((float)col.getBlue() / 255.0f), (float)0.7f);
                    GL11.glLineWidth((float)3.0f);
                    Tessellator tessellator = Tessellator.func_178181_a();
                    WorldRenderer worldrenderer = tessellator.func_178180_c();
                    GlStateManager.func_179120_a((int)770, (int)771, (int)1, (int)0);
                    worldrenderer.func_181668_a(3, DefaultVertexFormats.field_181705_e);
                    for (Vec3 vec5 : this.positions) {
                        worldrenderer.func_181662_b((double)((float)vec5.field_72450_a) - Trajectories.mc.func_175598_ae().field_78725_b, (double)((float)vec5.field_72448_b) - Trajectories.mc.func_175598_ae().field_78726_c, (double)((float)vec5.field_72449_c) - Trajectories.mc.func_175598_ae().field_78723_d).func_181675_d();
                    }
                    tessellator.func_78381_a();
                    if (m != null) {
                        GL11.glColor4f((float)((float)col.getRed() / 255.0f), (float)((float)col.getGreen() / 255.0f), (float)((float)col.getBlue() / 255.0f), (float)0.3f);
                        Vec3 hitVec = m.field_72307_f;
                        EnumFacing enumFacing1 = m.field_178784_b;
                        float minX = (float)(hitVec.field_72450_a - Trajectories.mc.func_175598_ae().field_78725_b);
                        float maxX = (float)(hitVec.field_72450_a - Trajectories.mc.func_175598_ae().field_78725_b);
                        float minY = (float)(hitVec.field_72448_b - Trajectories.mc.func_175598_ae().field_78726_c);
                        float maxY = (float)(hitVec.field_72448_b - Trajectories.mc.func_175598_ae().field_78726_c);
                        float minZ = (float)(hitVec.field_72449_c - Trajectories.mc.func_175598_ae().field_78723_d);
                        float maxZ = (float)(hitVec.field_72449_c - Trajectories.mc.func_175598_ae().field_78723_d);
                        if (enumFacing1 == EnumFacing.SOUTH) {
                            minX -= 0.4f;
                            maxX += 0.4f;
                            minY -= 0.4f;
                            maxY += 0.4f;
                            maxZ += 0.02f;
                            minZ += 0.05f;
                        } else if (enumFacing1 == EnumFacing.NORTH) {
                            minX -= 0.4f;
                            maxX += 0.4f;
                            minY -= 0.4f;
                            maxY += 0.4f;
                            maxZ -= 0.02f;
                            minZ -= 0.05f;
                        } else if (enumFacing1 == EnumFacing.EAST) {
                            maxX += 0.02f;
                            minX += 0.05f;
                            minY -= 0.4f;
                            maxY += 0.4f;
                            minZ -= 0.4f;
                            maxZ += 0.4f;
                        } else if (enumFacing1 == EnumFacing.WEST) {
                            maxX -= 0.02f;
                            minX -= 0.05f;
                            minY -= 0.4f;
                            maxY += 0.4f;
                            minZ -= 0.4f;
                            maxZ += 0.4f;
                        } else if (enumFacing1 == EnumFacing.UP) {
                            minX -= 0.4f;
                            maxX += 0.4f;
                            maxY += 0.02f;
                            minY += 0.05f;
                            minZ -= 0.4f;
                            maxZ += 0.4f;
                        } else if (enumFacing1 == EnumFacing.DOWN) {
                            minX -= 0.4f;
                            maxX += 0.4f;
                            maxY -= 0.02f;
                            minY -= 0.05f;
                            minZ -= 0.4f;
                            maxZ += 0.4f;
                        }
                        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181675_d();
                        tessellator.func_78381_a();
                        GL11.glLineWidth((float)2.0f);
                        worldrenderer.func_181668_a(3, DefaultVertexFormats.field_181705_e);
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)minY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)maxZ).func_181675_d();
                        worldrenderer.func_181662_b((double)maxX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181662_b((double)minX, (double)maxY, (double)minZ).func_181675_d();
                        worldrenderer.func_181675_d();
                        tessellator.func_78381_a();
                    }
                    GL11.glLineWidth((float)1.0f);
                    GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                    GL11.glDepthMask((boolean)true);
                    GlStateManager.func_179089_o();
                    GL11.glEnable((int)3553);
                    GL11.glEnable((int)2929);
                    GL11.glDisable((int)3042);
                    GL11.glBlendFunc((int)770, (int)771);
                    GL11.glDisable((int)2848);
                }
            }
        }
    }
}

