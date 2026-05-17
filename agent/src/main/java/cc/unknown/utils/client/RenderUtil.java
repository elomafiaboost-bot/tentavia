/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.RenderGlobal
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.WorldRenderer
 *  net.minecraft.client.renderer.entity.RenderManager
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.client.renderer.texture.ITextureObject
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.scoreboard.ScorePlayerTeam
 *  net.minecraft.scoreboard.Scoreboard
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.BlockPos
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.Timer
 *  net.minecraft.util.Vec3
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.utils.client;

import cc.unknown.utils.Loona;
import cc.unknown.utils.client.ColorUtil;
import java.awt.Color;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class RenderUtil
implements Loona {
    public static void drawMenu(int mouseX, int mouseY) {
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }
        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }
        float f3 = (float)(color >> 24 & 0xFF) / 255.0f;
        float f = (float)(color >> 16 & 0xFF) / 255.0f;
        float f1 = (float)(color >> 8 & 0xFF) / 255.0f;
        float f2 = (float)(color & 0xFF) / 255.0f;
        Tessellator t = Tessellator.func_178181_a();
        WorldRenderer w = t.func_178180_c();
        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a((int)770, (int)771, (int)1, (int)0);
        GlStateManager.func_179131_c((float)f, (float)f1, (float)f2, (float)f3);
        w.func_181668_a(7, DefaultVertexFormats.field_181705_e);
        w.func_181662_b(left, bottom, 0.0).func_181675_d();
        w.func_181662_b(right, bottom, 0.0).func_181675_d();
        w.func_181662_b(right, top, 0.0).func_181675_d();
        w.func_181662_b(left, top, 0.0).func_181675_d();
        t.func_78381_a();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
    }

    public static void drawBorderedRoundedRect(float x, float y, float x1, float y1, float radius, float borderSize, int borderC, int insideC) {
        RenderUtil.drawRoundedRect(x, y, x1, y1, radius, insideC);
        RenderUtil.drawRoundedOutline(x, y, x1, y1, radius, borderSize, borderC);
    }

    public static void drawRoundedRect(float x, float y, float x1, float y1, float radius, int color) {
        RenderUtil.drawRoundedRect(x, y, x1, y1, radius, color, new boolean[]{true, true, true, true});
    }

    public static void drawRoundedRect(float x, float y, float x1, float y1, float radius, int color, boolean[] round) {
        GL11.glPushAttrib((int)0);
        GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
        x = (float)((double)x * 2.0);
        y = (float)((double)y * 2.0);
        x1 = (float)((double)x1 * 2.0);
        y1 = (float)((double)y1 * 2.0);
        GL11.glEnable((int)3042);
        GL11.glDisable((int)3553);
        GL11.glEnable((int)2848);
        ColorUtil.setColor(color);
        GL11.glEnable((int)2848);
        GL11.glBegin((int)9);
        RenderUtil.round(x, y, x1, y1, radius, round);
        GL11.glEnd();
        GL11.glEnable((int)3553);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)2848);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)2848);
        GL11.glScaled((double)2.0, (double)2.0, (double)2.0);
        GL11.glEnable((int)3042);
        GL11.glPopAttrib();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    public static void roundHelper(float x, float y, float radius, int pn, int pn2, int originalRotation, int finalRotation) {
        for (int i = originalRotation; i <= finalRotation; i += 3) {
            GL11.glVertex2d((double)((double)(x + radius * (float)(-pn)) + Math.sin((double)i * Math.PI / 180.0) * (double)radius * (double)pn), (double)((double)(y + radius * (float)pn2) + Math.cos((double)i * Math.PI / 180.0) * (double)radius * (double)pn));
        }
    }

    public static void drawRoundedOutline(float x, float y, float x1, float y1, float radius, float borderSize, int color) {
        RenderUtil.drawRoundedOutline(x, y, x1, y1, radius, borderSize, color, new boolean[]{true, true, true, true});
    }

    public static void round(float x, float y, float x1, float y1, float radius, boolean[] round) {
        if (round[0]) {
            RenderUtil.roundHelper(x, y, radius, -1, 1, 0, 90);
        } else {
            GL11.glVertex2d((double)x, (double)y);
        }
        if (round[1]) {
            RenderUtil.roundHelper(x, y1, radius, -1, -1, 90, 180);
        } else {
            GL11.glVertex2d((double)x, (double)y1);
        }
        if (round[2]) {
            RenderUtil.roundHelper(x1, y1, radius, 1, -1, 0, 90);
        } else {
            GL11.glVertex2d((double)x1, (double)y1);
        }
        if (round[3]) {
            RenderUtil.roundHelper(x1, y, radius, 1, 1, 90, 180);
        } else {
            GL11.glVertex2d((double)x1, (double)y);
        }
    }

    public static void drawRoundedOutline(float x, float y, float x1, float y1, float radius, float borderSize, int color, boolean[] drawCorner) {
        GL11.glPushAttrib((int)0);
        GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
        x = (float)((double)x * 2.0);
        y = (float)((double)y * 2.0);
        x1 = (float)((double)x1 * 2.0);
        y1 = (float)((double)y1 * 2.0);
        GL11.glEnable((int)3042);
        GL11.glDisable((int)3553);
        ColorUtil.setColor(color);
        GL11.glEnable((int)2848);
        GL11.glLineWidth((float)borderSize);
        GL11.glBegin((int)2);
        RenderUtil.round(x, y, x1, y1, radius, drawCorner);
        GL11.glEnd();
        GL11.glEnable((int)3553);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)2848);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)3553);
        GL11.glScaled((double)2.0, (double)2.0, (double)2.0);
        GL11.glPopAttrib();
        GL11.glLineWidth((float)1.0f);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    public static void rect(float x1, float y1, float x2, float y2, int fill) {
        GlStateManager.func_179124_c((float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glColor4f((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        float f = (float)(fill >> 24 & 0xFF) / 255.0f;
        float f1 = (float)(fill >> 16 & 0xFF) / 255.0f;
        float f2 = (float)(fill >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(fill & 0xFF) / 255.0f;
        GL11.glEnable((int)3042);
        GL11.glDisable((int)3553);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glEnable((int)2848);
        GL11.glPushMatrix();
        GL11.glColor4f((float)f1, (float)f2, (float)f3, (float)f);
        GL11.glBegin((int)7);
        GL11.glVertex2d((double)x2, (double)y1);
        GL11.glVertex2d((double)x1, (double)y1);
        GL11.glVertex2d((double)x1, (double)y2);
        GL11.glVertex2d((double)x2, (double)y2);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable((int)3553);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)2848);
    }

    public static void drawBorderedRect(float f, float f1, float f2, float f3, float f4, int i, int j) {
        RenderUtil.drawRect(f, f1, f2, f3, j);
        float f5 = (float)(i >> 24 & 0xFF) / 255.0f;
        float f6 = (float)(i >> 16 & 0xFF) / 255.0f;
        float f7 = (float)(i >> 8 & 0xFF) / 255.0f;
        float f8 = (float)(i & 0xFF) / 255.0f;
        GL11.glEnable((int)3042);
        GL11.glDisable((int)3553);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glEnable((int)2848);
        GL11.glPushMatrix();
        GL11.glColor4f((float)f6, (float)f7, (float)f8, (float)f5);
        GL11.glLineWidth((float)f4);
        GL11.glBegin((int)1);
        GL11.glVertex2d((double)f, (double)f1);
        GL11.glVertex2d((double)f, (double)f3);
        GL11.glVertex2d((double)f2, (double)f3);
        GL11.glVertex2d((double)f2, (double)f1);
        GL11.glVertex2d((double)f, (double)f1);
        GL11.glVertex2d((double)f2, (double)f1);
        GL11.glVertex2d((double)f, (double)f3);
        GL11.glVertex2d((double)f2, (double)f3);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable((int)3553);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)2848);
    }

    public static void startDrawing() {
        GL11.glEnable((int)3042);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glEnable((int)2848);
        GL11.glDisable((int)3553);
        GL11.glDisable((int)2929);
        RenderUtil.mc.field_71460_t.func_78479_a(RenderUtil.mc.field_71428_T.field_74281_c, 0);
    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height) {
        GL11.glDisable((int)2929);
        GL11.glEnable((int)3042);
        GL11.glDepthMask((boolean)false);
        OpenGlHelper.func_148821_a((int)770, (int)771, (int)1, (int)0);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        mc.func_110434_K().func_110577_a(resourceLocation);
        Gui.func_146110_a((int)((int)x), (int)((int)y), (float)0.0f, (float)0.0f, (int)((int)width), (int)((int)height), (float)width, (float)height);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)2929);
    }

    public static void drawImage(DynamicTexture image, float x, float y, float width, float height, ResourceLocation id) {
        mc.func_110434_K().func_110579_a(id, (ITextureObject)image);
        RenderUtil.drawImage(id, x, y, width, height);
    }

    public static void drawChestBox(BlockPos bp, int color, boolean shade) {
        if (bp != null) {
            double x = (double)bp.func_177958_n() - RenderUtil.mc.func_175598_ae().field_78730_l;
            double y = (double)bp.func_177956_o() - RenderUtil.mc.func_175598_ae().field_78731_m;
            double z = (double)bp.func_177952_p() - RenderUtil.mc.func_175598_ae().field_78728_n;
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glEnable((int)3042);
            GL11.glLineWidth((float)2.0f);
            GL11.glDisable((int)3553);
            GL11.glDisable((int)2929);
            GL11.glDepthMask((boolean)false);
            float a = (float)(color >> 24 & 0xFF) / 255.0f;
            float r = (float)(color >> 16 & 0xFF) / 255.0f;
            float g = (float)(color >> 8 & 0xFF) / 255.0f;
            float b = (float)(color & 0xFF) / 255.0f;
            GL11.glColor4d((double)r, (double)g, (double)b, (double)a);
            RenderGlobal.func_181561_a((AxisAlignedBB)new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0));
            if (shade) {
                RenderUtil.dbb(new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0), r, g, b);
            }
            GL11.glEnable((int)3553);
            GL11.glEnable((int)2929);
            GL11.glDepthMask((boolean)true);
            GL11.glDisable((int)3042);
        }
    }

    public static void drawBoxAroundEntity(Entity e, int type, double expand, double shift, int color, boolean damage) {
        if (e instanceof EntityLivingBase) {
            double x = e.field_70142_S + (e.field_70165_t - e.field_70142_S) * (double)RenderUtil.mc.field_71428_T.field_74281_c - RenderUtil.mc.func_175598_ae().field_78730_l;
            double y = e.field_70137_T + (e.field_70163_u - e.field_70137_T) * (double)RenderUtil.mc.field_71428_T.field_74281_c - RenderUtil.mc.func_175598_ae().field_78731_m;
            double z = e.field_70136_U + (e.field_70161_v - e.field_70136_U) * (double)RenderUtil.mc.field_71428_T.field_74281_c - RenderUtil.mc.func_175598_ae().field_78728_n;
            float d = (float)expand / 40.0f;
            if (e instanceof EntityPlayer && damage && ((EntityPlayer)e).field_70737_aN != 0) {
                color = Color.RED.getRGB();
            }
            GlStateManager.func_179094_E();
            int teamColor = RenderUtil.getTeamColor((EntityPlayer)e);
            if (type == 3) {
                GL11.glTranslated((double)x, (double)(y - 0.2), (double)z);
                GL11.glRotated((double)(-RenderUtil.mc.func_175598_ae().field_78735_i), (double)0.0, (double)1.0, (double)0.0);
                GlStateManager.func_179097_i();
                GL11.glScalef((float)(0.03f + d), (float)(0.03f + d), (float)(0.03f + d));
                int outline = Color.black.getRGB();
                Gui.func_73734_a((int)-20, (int)-1, (int)-26, (int)75, (int)outline);
                Gui.func_73734_a((int)20, (int)-1, (int)26, (int)75, (int)outline);
                Gui.func_73734_a((int)-20, (int)-1, (int)21, (int)5, (int)outline);
                Gui.func_73734_a((int)-20, (int)70, (int)21, (int)75, (int)outline);
                if (color != 0) {
                    Gui.func_73734_a((int)-21, (int)0, (int)-25, (int)74, (int)color);
                    Gui.func_73734_a((int)21, (int)0, (int)25, (int)74, (int)color);
                    Gui.func_73734_a((int)-21, (int)0, (int)24, (int)4, (int)color);
                    Gui.func_73734_a((int)-21, (int)71, (int)25, (int)74, (int)color);
                } else {
                    int st = ColorUtil.rainbowDraw(2L, 0L);
                    int en = ColorUtil.rainbowDraw(2L, 1000L);
                    RenderUtil.dGR(-21, 0, -25, 74, st, en);
                    RenderUtil.dGR(21, 0, 25, 74, st, en);
                    Gui.func_73734_a((int)-21, (int)0, (int)21, (int)4, (int)en);
                    Gui.func_73734_a((int)-21, (int)71, (int)21, (int)74, (int)st);
                }
                GlStateManager.func_179126_j();
            } else if (type == 4) {
                EntityLivingBase en = (EntityLivingBase)e;
                double r = en.func_110143_aJ() / en.func_110138_aP();
                int b = (int)(74.0 * r);
                int hc = r < 0.3 ? Color.red.getRGB() : (r < 0.5 ? Color.orange.getRGB() : (r < 0.7 ? Color.yellow.getRGB() : Color.green.getRGB()));
                GL11.glTranslated((double)x, (double)(y - 0.2), (double)z);
                GL11.glRotated((double)(-RenderUtil.mc.func_175598_ae().field_78735_i), (double)0.0, (double)1.0, (double)0.0);
                GlStateManager.func_179097_i();
                GL11.glScalef((float)(0.03f + d), (float)(0.03f + d), (float)(0.03f + d));
                int i = (int)(21.0 + shift * 2.0);
                Gui.func_73734_a((int)i, (int)-1, (int)(i + 5), (int)75, (int)Color.black.getRGB());
                Gui.func_73734_a((int)(i + 1), (int)b, (int)(i + 4), (int)74, (int)Color.darkGray.getRGB());
                Gui.func_73734_a((int)(i + 1), (int)0, (int)(i + 4), (int)b, (int)hc);
                GlStateManager.func_179126_j();
            } else if (type == 6) {
                RenderUtil.d3p(x, y, z, 0.7f, 45, 1.5f, color, color == 0);
            } else {
                if (color == 0) {
                    color = ColorUtil.rainbowDraw(2L, 0L);
                }
                float a = (float)(color >> 24 & 0xFF) / 255.0f;
                float r = (float)(color >> 16 & 0xFF) / 255.0f;
                float g = (float)(color >> 8 & 0xFF) / 255.0f;
                float b = (float)(color & 0xFF) / 255.0f;
                if (type == 5) {
                    int i;
                    GL11.glTranslated((double)x, (double)(y - 0.2), (double)z);
                    GL11.glRotated((double)(-RenderUtil.mc.func_175598_ae().field_78735_i), (double)0.0, (double)1.0, (double)0.0);
                    GlStateManager.func_179097_i();
                    GL11.glScalef((float)(0.03f + d), (float)0.03f, (float)(0.03f + d));
                    RenderUtil.d2p(0.0, 95.0, 10, 3, Color.black.getRGB());
                    for (i = 0; i < 6; ++i) {
                        RenderUtil.d2p(0.0, 95 + (10 - i), 3, 4, Color.black.getRGB());
                    }
                    for (i = 0; i < 7; ++i) {
                        RenderUtil.d2p(0.0, 95 + (10 - i), 2, 4, color);
                    }
                    RenderUtil.d2p(0.0, 95.0, 8, 3, color);
                    GlStateManager.func_179126_j();
                } else {
                    AxisAlignedBB bbox = e.func_174813_aQ().func_72314_b(0.1 + expand, 0.1 + expand, 0.1 + expand);
                    AxisAlignedBB axis = new AxisAlignedBB(bbox.field_72340_a - e.field_70165_t + x, bbox.field_72338_b - e.field_70163_u + y, bbox.field_72339_c - e.field_70161_v + z, bbox.field_72336_d - e.field_70165_t + x, bbox.field_72337_e - e.field_70163_u + y, bbox.field_72334_f - e.field_70161_v + z);
                    GL11.glBlendFunc((int)770, (int)771);
                    GL11.glEnable((int)3042);
                    GL11.glDisable((int)3553);
                    GL11.glDisable((int)2929);
                    GL11.glDepthMask((boolean)false);
                    GL11.glLineWidth((float)2.0f);
                    GL11.glColor4f((float)r, (float)g, (float)b, (float)a);
                    if (type == 1) {
                        RenderGlobal.func_181561_a((AxisAlignedBB)axis);
                    } else if (type == 2) {
                        RenderUtil.dbb(axis, r, g, b);
                    } else if (type == 7) {
                        RenderUtil.dsbbt(axis, teamColor);
                    }
                    GL11.glEnable((int)3553);
                    GL11.glEnable((int)2929);
                    GL11.glDepthMask((boolean)true);
                    GL11.glDisable((int)3042);
                }
            }
            GlStateManager.func_179121_F();
        }
    }

    private static void d2p(double x, double y, int radius, int sides, int color) {
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a((int)770, (int)771, (int)1, (int)0);
        GlStateManager.func_179131_c((float)r, (float)g, (float)b, (float)a);
        worldrenderer.func_181668_a(6, DefaultVertexFormats.field_181705_e);
        for (int i = 0; i < sides; ++i) {
            double angle = Math.PI * 2 * (double)i / (double)sides + Math.toRadians(180.0);
            worldrenderer.func_181662_b(x + Math.sin(angle) * (double)radius, y + Math.cos(angle) * (double)radius, 0.0).func_181675_d();
        }
        tessellator.func_78381_a();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
    }

    private static void dbb(AxisAlignedBB abb, float r, float g, float b) {
        float a = 0.25f;
        Tessellator ts = Tessellator.func_178181_a();
        WorldRenderer vb = ts.func_178180_c();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f).func_181666_a(r, g, b, a).func_181675_d();
        ts.func_78381_a();
    }

    private static void d3p(double x, double y, double z, double radius, int sides, float lineWidth, int color, boolean chroma) {
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        RenderUtil.mc.field_71460_t.func_175072_h();
        GL11.glDisable((int)3553);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)2929);
        GL11.glEnable((int)2848);
        GL11.glDepthMask((boolean)false);
        GL11.glLineWidth((float)lineWidth);
        if (!chroma) {
            GL11.glColor4f((float)r, (float)g, (float)b, (float)a);
        }
        GL11.glBegin((int)1);
        long d = 0L;
        long ed = 15000L / (long)sides;
        long hed = ed / 2L;
        for (int i = 0; i < sides * 2; ++i) {
            if (chroma) {
                if (i % 2 != 0) {
                    if (i == 47) {
                        d = hed;
                    }
                    d += ed;
                }
                int c = ColorUtil.rainbowDraw(2L, d);
                float r2 = (float)(c >> 16 & 0xFF) / 255.0f;
                float g2 = (float)(c >> 8 & 0xFF) / 255.0f;
                float b2 = (float)(c & 0xFF) / 255.0f;
                GL11.glColor3f((float)r2, (float)g2, (float)b2);
            }
            double angle = Math.PI * 2 * (double)i / (double)sides + Math.toRadians(180.0);
            GL11.glVertex3d((double)(x + Math.cos(angle) * radius), (double)y, (double)(z + Math.sin(angle) * radius));
        }
        GL11.glEnd();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)2848);
        GL11.glEnable((int)2929);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)3553);
        RenderUtil.mc.field_71460_t.func_180436_i();
    }

    private static void dsbbt(AxisAlignedBB var0, int teamColor) {
        Tessellator var1 = Tessellator.func_178181_a();
        WorldRenderer var2 = var1.func_178180_c();
        float red = (float)(teamColor >> 16 & 0xFF) / 255.0f;
        float green = (float)(teamColor >> 8 & 0xFF) / 255.0f;
        float blue = (float)(teamColor & 0xFF) / 255.0f;
        GlStateManager.func_179131_c((float)red, (float)green, (float)blue, (float)1.0f);
        var2.func_181668_a(3, DefaultVertexFormats.field_181705_e);
        var2.func_181662_b(var0.field_72340_a, var0.field_72338_b, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72338_b, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72338_b, var0.field_72334_f).func_181675_d();
        var2.func_181662_b(var0.field_72340_a, var0.field_72338_b, var0.field_72334_f).func_181675_d();
        var2.func_181662_b(var0.field_72340_a, var0.field_72338_b, var0.field_72339_c).func_181675_d();
        var1.func_78381_a();
        var2.func_181668_a(3, DefaultVertexFormats.field_181705_e);
        var2.func_181662_b(var0.field_72340_a, var0.field_72337_e, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72337_e, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72337_e, var0.field_72334_f).func_181675_d();
        var2.func_181662_b(var0.field_72340_a, var0.field_72337_e, var0.field_72334_f).func_181675_d();
        var2.func_181662_b(var0.field_72340_a, var0.field_72337_e, var0.field_72339_c).func_181675_d();
        var1.func_78381_a();
        var2.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        var2.func_181662_b(var0.field_72340_a, var0.field_72338_b, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72340_a, var0.field_72337_e, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72338_b, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72337_e, var0.field_72339_c).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72338_b, var0.field_72334_f).func_181675_d();
        var2.func_181662_b(var0.field_72336_d, var0.field_72337_e, var0.field_72334_f).func_181675_d();
        var2.func_181662_b(var0.field_72340_a, var0.field_72338_b, var0.field_72334_f).func_181675_d();
        var2.func_181662_b(var0.field_72340_a, var0.field_72337_e, var0.field_72334_f).func_181675_d();
        var1.func_78381_a();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private static void dGR(int left, int top, int right, int bottom, int startColor, int endColor) {
        int j;
        if (left < right) {
            j = left;
            left = right;
            right = j;
        }
        if (top < bottom) {
            j = top;
            top = bottom;
            bottom = j;
        }
        float f = (float)(startColor >> 24 & 0xFF) / 255.0f;
        float f1 = (float)(startColor >> 16 & 0xFF) / 255.0f;
        float f2 = (float)(startColor >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(startColor & 0xFF) / 255.0f;
        float f4 = (float)(endColor >> 24 & 0xFF) / 255.0f;
        float f5 = (float)(endColor >> 16 & 0xFF) / 255.0f;
        float f6 = (float)(endColor >> 8 & 0xFF) / 255.0f;
        float f7 = (float)(endColor & 0xFF) / 255.0f;
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179118_c();
        GlStateManager.func_179120_a((int)770, (int)771, (int)1, (int)0);
        GlStateManager.func_179103_j((int)7425);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        worldrenderer.func_181662_b((double)right, (double)top, 0.0).func_181666_a(f1, f2, f3, f).func_181675_d();
        worldrenderer.func_181662_b((double)left, (double)top, 0.0).func_181666_a(f1, f2, f3, f).func_181675_d();
        worldrenderer.func_181662_b((double)left, (double)bottom, 0.0).func_181666_a(f5, f6, f7, f4).func_181675_d();
        worldrenderer.func_181662_b((double)right, (double)bottom, 0.0).func_181666_a(f5, f6, f7, f4).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179103_j((int)7424);
        GlStateManager.func_179084_k();
        GlStateManager.func_179141_d();
        GlStateManager.func_179098_w();
    }

    private static int getTeamColor(EntityPlayer player) {
        Scoreboard scoreboard = player.func_96123_co();
        ScorePlayerTeam playerTeam = scoreboard.func_96509_i(player.func_70005_c_());
        if (playerTeam != null) {
            String color = playerTeam.func_96668_e();
            if (color.length() < 2) {
                return Color.WHITE.getRGB();
            }
            char colorChar = color.charAt(1);
            if (colorChar == '4' || colorChar == 'c') {
                return Color.RED.getRGB();
            }
            if (colorChar == '6' || colorChar == 'e') {
                return Color.YELLOW.getRGB();
            }
            if (colorChar == '2' || colorChar == 'a') {
                return Color.GREEN.getRGB();
            }
            if (colorChar == 'b' || colorChar == '3') {
                return Color.CYAN.getRGB();
            }
            if (colorChar == '9' || colorChar == '1') {
                return Color.BLUE.getRGB();
            }
            if (colorChar == 'd' || colorChar == '5') {
                return Color.MAGENTA.getRGB();
            }
            if (colorChar == 'f' || colorChar == '7') {
                return Color.WHITE.getRGB();
            }
            if (colorChar == '8' || colorChar == '0') {
                return Color.BLACK.getRGB();
            }
        }
        return Color.WHITE.getRGB();
    }

    public static void drawBox(Entity entity, Vec3 realPos, Vec3 lastPos, Color color) {
        RenderManager renderManager = mc.func_175598_ae();
        Timer timer = RenderUtil.mc.field_71428_T;
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glEnable((int)3042);
        GL11.glDisable((int)3553);
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        double x = lastPos.field_72450_a + (realPos.field_72450_a - lastPos.field_72450_a) * (double)timer.field_74281_c - renderManager.field_78725_b;
        double y = lastPos.field_72448_b + (realPos.field_72448_b - lastPos.field_72448_b) * (double)timer.field_74281_c - renderManager.field_78726_c;
        double z = lastPos.field_72449_c + (realPos.field_72449_c - lastPos.field_72449_c) * (double)timer.field_74281_c - renderManager.field_78723_d;
        AxisAlignedBB entityBox = entity.func_174813_aQ();
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(entityBox.field_72340_a - entity.field_70165_t + x - 0.05, entityBox.field_72338_b - entity.field_70163_u + y, entityBox.field_72339_c - entity.field_70161_v + z - 0.05, entityBox.field_72336_d - entity.field_70165_t + x + 0.05, entityBox.field_72337_e - entity.field_70163_u + y + 0.15, entityBox.field_72334_f - entity.field_70161_v + z + 0.05);
        RenderUtil.glColor(color.getRed(), color.getGreen(), color.getBlue(), 35);
        RenderUtil.drawFilledBox(axisAlignedBB);
        GlStateManager.func_179117_G();
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)3553);
        GL11.glEnable((int)2929);
        GL11.glDepthMask((boolean)true);
    }

    public static void glColor(int red, int green, int blue, int alpha) {
        GlStateManager.func_179131_c((float)((float)red / 255.0f), (float)((float)green / 255.0f), (float)((float)blue / 255.0f), (float)((float)alpha / 255.0f));
    }

    public static void drawFilledBox(AxisAlignedBB axisAlignedBB) {
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldRenderer = tessellator.func_178180_c();
        worldRenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f).func_181675_d();
        worldRenderer.func_181662_b(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f).func_181675_d();
        tessellator.func_78381_a();
    }
}

