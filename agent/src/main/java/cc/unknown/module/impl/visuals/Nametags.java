/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.entity.RenderManager
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemBow
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.item.ItemTool
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.client.ColorUtil;
import cc.unknown.utils.client.RenderUtil;
import cc.unknown.utils.player.CombatUtil;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import org.lwjgl.opengl.GL11;

@Register(name="Nametags", category=Category.Visuals)
public class Nametags
extends Module {
    private ModeValue mode = new ModeValue("Mode", "Health", "Health", "Percentage");
    private SliderValue range = new SliderValue("Range", 0.0, 0.0, 512.0, 1.0);
    private SliderValue scale = new SliderValue("Scale", 4.5, 0.1, 10.0, 0.1);
    private SliderValue opacity = new SliderValue("Opacity", 185.0, 5.0, 200.0, 5.0);
    private BooleanValue armor = new BooleanValue("Armor", true);
    private BooleanValue durability = new BooleanValue("Durability", false);
    private BooleanValue distance = new BooleanValue("Distance", false);
    private BooleanValue showInvis = new BooleanValue("Show invis", true);
    private float _x = 0.0f;
    private float _y = 0.0f;
    private float _z = 0.0f;

    public Nametags() {
        this.registerSetting(this.mode, this.range, this.scale, this.opacity, this.armor, this.durability, this.distance, this.showInvis);
    }

    @EventLink
    public void onRender(RenderEvent e) {
        if (e.isLabel()) {
            String playerName = e.getTarget().func_145748_c_().func_150254_d();
            if (playerName == null || playerName.isEmpty()) {
                return;
            }
            if (!CombatUtil.instance.canTarget(e.getTarget(), true)) {
                return;
            }
            double playerDistance = Nametags.mc.field_71439_g.func_70032_d(e.getTarget());
            if (!(playerDistance <= this.range.getInput()) && this.range.getInput() != 0.0) {
                return;
            }
            e.setCancelled(true);
        }
        if (e.is3D()) {
            ArrayList<net.minecraft.entity.player.EntityPlayer> players = new ArrayList<net.minecraft.entity.player.EntityPlayer>();
            Nametags.mc.field_71441_e.field_73010_i.forEach(entity -> {
                double distance = Nametags.mc.field_71439_g.func_70032_d((Entity)entity);
                if (this.range.getInput() != 0.0 && distance > this.range.getInput() || entity.func_70005_c_().matches(".*[-/|<>\\u0e22\\u0e07].*") || entity.func_70005_c_().isEmpty() || !this.showInvis.isToggled() && entity.func_82150_aj()) {
                    return;
                }
                players.add(entity);
                if (players.size() >= 100) {
                    return;
                }
            });
            RenderManager renderManager = mc.func_175598_ae();
            players.stream().filter(player -> CombatUtil.instance.canTarget((Entity)player, true)).forEach(player -> {
                player.func_174805_g(false);
                this._x = (float)(player.field_70142_S + (player.field_70165_t - player.field_70142_S) * (double)Nametags.mc.field_71428_T.field_74281_c - renderManager.field_78730_l);
                this._y = (float)(player.field_70137_T + (player.field_70163_u - player.field_70137_T) * (double)Nametags.mc.field_71428_T.field_74281_c - renderManager.field_78731_m);
                this._z = (float)(player.field_70136_U + (player.field_70161_v - player.field_70136_U) * (double)Nametags.mc.field_71428_T.field_74281_c - renderManager.field_78728_n);
                this.renderNametag((EntityPlayer)player, this._x, this._y, this._z);
            });
        }
    }

    private String getHealth(EntityPlayer player) {
        DecimalFormat decimalFormat = new DecimalFormat("0.#");
        return this.mode.is("Percentage") ? decimalFormat.format(player.func_110143_aJ() * 5.0f + player.func_110139_bj() * 5.0f) : decimalFormat.format(player.func_110143_aJ() / 2.0f + player.func_110139_bj() / 2.0f);
    }

    private void drawNames(EntityPlayer player) {
        float i;
        float e = (float)this.getWidth(this.getPlayerName(player)) / 2.0f + 2.2f;
        e = i = (float)((double)e + (double)(this.getWidth(" " + this.getHealth(player)) / 2) + 2.5);
        float x = -e - 2.2f;
        float z = this.getWidth(this.getPlayerName(player)) + 4;
        if (this.mode.is("Percentage")) {
            RenderUtil.drawBorderedRect(x, -3.0f, e, 10.0f, 1.0f, new Color(20, 20, 20, this.opacity.getInputToInt()).getRGB(), new Color(10, 10, 10, this.opacity.getInputToInt()).getRGB());
        } else {
            RenderUtil.drawBorderedRect(x + 5.0f, -3.0f, e, 10.0f, 1.0f, new Color(20, 20, 20, this.opacity.getInputToInt()).getRGB(), new Color(10, 10, 10, this.opacity.getInputToInt()).getRGB());
        }
        GlStateManager.func_179097_i();
        z = this.mode.is("Percentage") ? (z += (float)(this.getWidth(this.getHealth(player)) + this.getWidth(" %") - 1)) : (z += (float)(this.getWidth(this.getHealth(player)) + this.getWidth(" ") - 1));
        this.drawString(this.getPlayerName(player), i - z, 0.0f, 0xFFFFFF);
        int blendColor = player.func_110143_aJ() > 10.0f ? ColorUtil.blend(new Color(-16711936), new Color(-256), 1.0f / player.func_110143_aJ() / 2.0f * (player.func_110143_aJ() - 10.0f)).getRGB() : ColorUtil.blend(new Color(-256), new Color(-65536), 0.1f * player.func_110143_aJ()).getRGB();
        if (this.mode.is("Percentage")) {
            this.drawString(this.getHealth(player) + "%", i - (float)this.getWidth(this.getHealth(player) + " %"), 0.0f, blendColor);
        } else {
            this.drawString(this.getHealth(player), i - (float)this.getWidth(this.getHealth(player) + " "), 0.0f, blendColor);
        }
        GlStateManager.func_179126_j();
    }

    private void drawString(String string, float x, float y, int z) {
        Nametags.mc.field_71466_p.func_175063_a(string, x, y, z);
    }

    private int getWidth(String string) {
        return Nametags.mc.field_71466_p.func_78256_a(string);
    }

    private void startDrawing(float x, float y, float z, EntityPlayer player) {
        float rotateX = Nametags.mc.field_71474_y.field_74320_O == 2 ? -1.0f : 1.0f;
        double scaleRatio = (double)(this.getSize(player) / 10.0f) * this.scale.getInput() * 1.5;
        GL11.glPushMatrix();
        RenderUtil.startDrawing();
        GL11.glTranslatef((float)x, (float)y, (float)z);
        GL11.glNormal3f((float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)(-Nametags.mc.func_175598_ae().field_78735_i), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)Nametags.mc.func_175598_ae().field_78732_j, (float)rotateX, (float)0.0f, (float)0.0f);
        GL11.glScaled((double)(-0.01666666753590107 * scaleRatio), (double)(-0.01666666753590107 * scaleRatio), (double)(0.01666666753590107 * scaleRatio));
    }

    private void stopDrawing() {
        GL11.glDisable((int)3042);
        GL11.glEnable((int)3553);
        GL11.glDisable((int)2848);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)2929);
        GlStateManager.func_179124_c((float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179121_F();
    }

    private void renderNametag(EntityPlayer player, float x, float y, float z) {
        this.startDrawing(x, y += (float)(1.55 + (player.func_70093_af() ? 0.5 : 0.7)), z, player);
        this.drawNames(player);
        GL11.glColor4d((double)1.0, (double)1.0, (double)1.0, (double)1.0);
        if (this.armor.isToggled()) {
            this.renderArmor(player);
        }
        this.stopDrawing();
    }

    private void renderArmor(EntityPlayer player) {
        ItemStack[] armor = player.field_71071_by.field_70460_b;
        int pos = 0;
        for (ItemStack is : armor) {
            if (is == null) continue;
            pos -= 8;
        }
        if (player.func_70694_bm() != null) {
            pos -= 8;
            ItemStack var10 = player.func_70694_bm().func_77946_l();
            if (var10.func_77962_s() && (var10.func_77973_b() instanceof ItemTool || var10.func_77973_b() instanceof ItemArmor)) {
                var10.field_77994_a = 1;
            }
            this.renderItemStack(var10, pos, -20);
            pos += 16;
        }
        armor = player.field_71071_by.field_70460_b;
        for (int i = 3; i >= 0; --i) {
            ItemStack var11 = armor[i];
            if (var11 == null) continue;
            this.renderItemStack(var11, pos, -20);
            pos += 16;
        }
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private String getPlayerName(EntityPlayer player) {
        boolean isDistanceSettingToggled = this.distance.isToggled();
        return (isDistanceSettingToggled ? new DecimalFormat("#.##").format(Nametags.mc.field_71439_g.func_70032_d((Entity)player)) + "m " : "") + player.func_145748_c_().func_150254_d();
    }

    private float getSize(EntityPlayer player) {
        return Math.max(Nametags.mc.field_71439_g.func_70032_d((Entity)player) / 4.0f, 2.0f);
    }

    private void renderItemStack(ItemStack is, int xPos, int yPos) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179132_a((boolean)true);
        GlStateManager.func_179086_m((int)256);
        RenderHelper.func_74519_b();
        Nametags.mc.func_175599_af().field_77023_b = -150.0f;
        GlStateManager.func_179097_i();
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179141_d();
        GlStateManager.func_179098_w();
        GlStateManager.func_179145_e();
        GlStateManager.func_179126_j();
        mc.func_175599_af().func_180450_b(is, xPos, yPos);
        mc.func_175599_af().func_175030_a(Nametags.mc.field_71466_p, is, xPos, yPos);
        Nametags.mc.func_175599_af().field_77023_b = 0.0f;
        RenderHelper.func_74518_a();
        GlStateManager.func_179129_p();
        GlStateManager.func_179141_d();
        GlStateManager.func_179084_k();
        GlStateManager.func_179140_f();
        GlStateManager.func_179139_a((double)0.5, (double)0.5, (double)0.5);
        GlStateManager.func_179097_i();
        this.renderEnchantText(is, xPos, yPos);
        GlStateManager.func_179126_j();
        GlStateManager.func_179152_a((float)2.0f, (float)2.0f, (float)2.0f);
        GlStateManager.func_179121_F();
    }

    private void renderEnchantText(ItemStack is, int xPos, int yPos) {
        int newYPos = yPos - 24;
        if (is.func_77986_q() != null && is.func_77986_q().func_74745_c() >= 6) {
            Nametags.mc.field_71466_p.func_175063_a("god", (float)(xPos * 2), (float)newYPos, 0xFF0000);
        } else {
            int unbreakingLvl;
            if (is.func_77973_b() instanceof ItemArmor) {
                int protection = EnchantmentHelper.func_77506_a((int)Enchantment.field_180310_c.field_77352_x, (ItemStack)is);
                int projectileProtection = EnchantmentHelper.func_77506_a((int)Enchantment.field_180308_g.field_77352_x, (ItemStack)is);
                int blastProtectionLvL = EnchantmentHelper.func_77506_a((int)Enchantment.field_77327_f.field_77352_x, (ItemStack)is);
                int fireProtection = EnchantmentHelper.func_77506_a((int)Enchantment.field_77329_d.field_77352_x, (ItemStack)is);
                int thornsLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_92091_k.field_77352_x, (ItemStack)is);
                int unbreakingLvl2 = EnchantmentHelper.func_77506_a((int)Enchantment.field_77347_r.field_77352_x, (ItemStack)is);
                int remainingDurability = is.func_77958_k() - is.func_77952_i();
                if (this.durability.isToggled()) {
                    Nametags.mc.field_71466_p.func_175063_a(String.valueOf(remainingDurability), (float)(xPos * 2), (float)yPos, 0xFFFFFF);
                }
                if (protection > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("prot" + protection, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (projectileProtection > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("proj" + projectileProtection, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (blastProtectionLvL > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("bp" + blastProtectionLvL, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (fireProtection > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("frp" + fireProtection, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (thornsLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("th" + thornsLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (unbreakingLvl2 > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("unb" + unbreakingLvl2, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
            }
            if (is.func_77973_b() instanceof ItemBow) {
                int powerLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77345_t.field_77352_x, (ItemStack)is);
                int punchLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77344_u.field_77352_x, (ItemStack)is);
                int flameLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77343_v.field_77352_x, (ItemStack)is);
                unbreakingLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77347_r.field_77352_x, (ItemStack)is);
                if (powerLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("pow" + powerLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (punchLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("pun" + punchLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (flameLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("flame" + flameLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (unbreakingLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("unb" + unbreakingLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
            }
            if (is.func_77973_b() instanceof ItemSword) {
                int sharpnessLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_180314_l.field_77352_x, (ItemStack)is);
                int knockbackLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_180313_o.field_77352_x, (ItemStack)is);
                int fireAspectLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77334_n.field_77352_x, (ItemStack)is);
                unbreakingLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77347_r.field_77352_x, (ItemStack)is);
                if (sharpnessLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("sh" + sharpnessLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (knockbackLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("kb" + knockbackLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (fireAspectLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("fire" + fireAspectLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (unbreakingLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("unb" + unbreakingLvl, (float)(xPos * 2), (float)newYPos, -1);
                }
            }
            if (is.func_77973_b() instanceof ItemTool) {
                int unbreakingLvl3 = EnchantmentHelper.func_77506_a((int)Enchantment.field_77347_r.field_77352_x, (ItemStack)is);
                int efficiencyLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77349_p.field_77352_x, (ItemStack)is);
                int fortuneLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77346_s.field_77352_x, (ItemStack)is);
                int silkTouchLvl = EnchantmentHelper.func_77506_a((int)Enchantment.field_77348_q.field_77352_x, (ItemStack)is);
                if (efficiencyLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("eff" + efficiencyLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (fortuneLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("fo" + fortuneLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (silkTouchLvl > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("silk" + silkTouchLvl, (float)(xPos * 2), (float)newYPos, -1);
                    newYPos += 8;
                }
                if (unbreakingLvl3 > 0) {
                    Nametags.mc.field_71466_p.func_175063_a("ub" + unbreakingLvl3, (float)(xPos * 2), (float)newYPos, -1);
                }
            }
            if (is.func_77973_b() == Items.field_151153_ao && is.func_77962_s()) {
                Nametags.mc.field_71466_p.func_175063_a("god", (float)(xPos * 2), (float)newYPos, -1);
            }
        }
    }
}

