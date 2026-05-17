/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.AbstractClientPlayer
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.ScaledResolution
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.network.play.client.C02PacketUseEntity
 *  net.minecraft.network.play.client.C02PacketUseEntity$Action
 *  net.minecraft.world.World
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.PreUpdateEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import java.awt.Color;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.world.World;

@Register(name="TargetHUD", category=Category.Visuals)
public class TargetHUD
extends Module {
    private final SliderValue posX = new SliderValue("Position X", 100.0, 10.0, 2000.0, 10.0);
    private final SliderValue posY = new SliderValue("Position Y", 0.0, 10.0, 2000.0, 10.0);
    private EntityPlayer player;
    private int ticksSinceAttack;

    public TargetHUD() {
        this.registerSetting(this.posX, this.posY);
    }

    @Override
    public void onEnable() {
        this.player = null;
    }

    @EventLink
    public void onPre(PreUpdateEvent e) {
        ++this.ticksSinceAttack;
        if (this.ticksSinceAttack > 20) {
            this.player = null;
        }
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        C02PacketUseEntity wrapper;
        if (e.isSend() && e.getPacket() instanceof C02PacketUseEntity && (wrapper = (C02PacketUseEntity)e.getPacket()).func_149564_a((World)TargetHUD.mc.field_71441_e) instanceof EntityPlayer && wrapper.func_149565_c() == C02PacketUseEntity.Action.ATTACK) {
            this.ticksSinceAttack = 0;
            this.player = (EntityPlayer)wrapper.func_149564_a((World)TargetHUD.mc.field_71441_e);
        }
    }

    @EventLink
    public void onRender2D(RenderEvent e) {
        if (e.is2D()) {
            ScaledResolution sr = new ScaledResolution(mc);
            int x = sr.func_78326_a() / 2 + this.posX.getInputToInt();
            int y = sr.func_78328_b() / 2 + this.posY.getInputToInt();
            if (this.player == null) {
                return;
            }
            this.drawRect(x, y, 120, 40, new Color(0, 0, 0, 120).getRGB());
            TargetHUD.mc.field_71466_p.func_78276_b(this.player.func_70005_c_(), x + 45, y + 8, -1);
            double offset = -(this.player.field_70737_aN * 20);
            Color color = new Color(255, (int)(255.0 + offset), (int)(255.0 + offset));
            GlStateManager.func_179131_c((float)((float)color.getRed() / 255.0f), (float)((float)color.getGreen() / 255.0f), (float)((float)color.getBlue() / 255.0f), (float)((float)color.getAlpha() / 255.0f));
            mc.func_110434_K().func_110577_a(((AbstractClientPlayer)this.player).func_110306_p());
            Gui.func_152125_a((int)(x + 5), (int)(y + 5), (float)3.0f, (float)3.0f, (int)3, (int)3, (int)30, (int)30, (float)24.0f, (float)24.0f);
            GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            this.drawRect(x + 45, y + 20, 70, 15, new Color(255, 255, 255, 120).getRGB());
            this.drawRect(x + 45, y + 20, (int)(70.0f * (this.player.func_110143_aJ() / this.player.func_110138_aP())), 15, Theme.instance.getMainColor().darker().getRGB());
            String s = (int)(this.player.func_110143_aJ() / this.player.func_110138_aP() * 100.0f) + "%";
            TargetHUD.mc.field_71466_p.func_78276_b(s, x + 45 + 35 - TargetHUD.mc.field_71466_p.func_78256_a(s) / 2, y + 20 + 7 - TargetHUD.mc.field_71466_p.field_78288_b / 2 + 1, -1);
        }
    }

    private void drawRect(int x, int y, int width, int height, int color) {
        Gui.func_73734_a((int)x, (int)y, (int)(x + width), (int)(y + height), (int)color);
    }
}

