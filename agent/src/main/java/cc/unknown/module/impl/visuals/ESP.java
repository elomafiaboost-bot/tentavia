/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityChest
 *  net.minecraft.tileentity.TileEntityEnderChest
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
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import cc.unknown.utils.client.RenderUtil;
import cc.unknown.utils.player.PlayerUtil;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;

@Register(name="ESP", category=Category.Visuals)
public class ESP
extends Module {
    private ModeValue boxMode = new ModeValue("Box Mode", "2D", "2D", "3D", "Health");
    private ModeValue renderMode = new ModeValue("Render Mode", "Player", "Player", "Chest", "Both");
    private BooleanValue enablePlayerColor = new BooleanValue("Enable Player Color", false);
    private SliderValue playerColorHSB = new SliderValue("Player Color [H/S/B]", 0.0, 0.0, 350.0, 10.0);
    private BooleanValue enableChestColor = new BooleanValue("Enable Chest Color", false);
    private SliderValue chestColorHSB = new SliderValue("Chest Color [H/S/B]", 0.0, 0.0, 350.0, 10.0);
    private BooleanValue checkInvisibility = new BooleanValue("Check Invisibility", true);
    private BooleanValue checkTeams = new BooleanValue("Check Teams", true);
    private BooleanValue disableIfChestOpened = new BooleanValue("Disable if Chest Opened", false);

    public ESP() {
        this.registerSetting(this.boxMode, this.renderMode, this.enablePlayerColor, this.playerColorHSB, this.enableChestColor, this.chestColorHSB, this.checkInvisibility, this.checkTeams, this.disableIfChestOpened);
    }

    @EventLink
    public void onRender(RenderEvent e) {
        if (PlayerUtil.inGame() && e.is3D()) {
            int chestColorRGB;
            int playerColorRGB = this.enablePlayerColor.isToggled() ? Color.getHSBColor(this.playerColorHSB.getInputToFloat() % 360.0f / 360.0f, 1.0f, 1.0f).getRGB() : Theme.instance.getMainColor().getRGB();
            int n = chestColorRGB = this.enableChestColor.isToggled() ? Color.getHSBColor(this.chestColorHSB.getInputToFloat() % 360.0f / 360.0f, 1.0f, 1.0f).getRGB() : Theme.instance.getMainColor().getRGB();
            if (this.renderMode.is("Player") || this.renderMode.is("Both")) {
                for (EntityPlayer player : ESP.mc.field_71441_e.field_73010_i) {
                    if (player == ESP.mc.field_71439_g || player.field_70725_aQ != 0 || !this.checkInvisibility.isToggled() && player.func_82150_aj()) continue;
                    if (this.checkTeams.isToggled() && this.getColor(player.func_82169_q(2)) > 0) {
                        int teamColor = new Color(this.getColor(player.func_82169_q(2))).getRGB();
                        this.renderPlayer((Entity)player, teamColor);
                        continue;
                    }
                    this.renderPlayer((Entity)player, playerColorRGB);
                }
            }
            if (this.renderMode.is("Chest") || this.renderMode.is("Both")) {
                for (TileEntity te : ESP.mc.field_71441_e.field_147482_g) {
                    if (!(te instanceof TileEntityChest) && !(te instanceof TileEntityEnderChest) || this.disableIfChestOpened.isToggled() && ((TileEntityChest)te).field_145989_m > 0.0f) continue;
                    RenderUtil.drawChestBox(te.func_174877_v(), chestColorRGB, true);
                }
            }
        }
    }

    private int getColor(ItemStack stack) {
        NBTTagCompound nbt;
        if (stack == null) {
            return -1;
        }
        NBTTagCompound tag = stack.func_77978_p();
        if (tag != null && (nbt = tag.func_74775_l("display")) != null && nbt.func_150297_b("color", 3)) {
            return nbt.func_74762_e("color");
        }
        return -2;
    }

    private void renderPlayer(Entity target, int rgb) {
        switch (this.boxMode.getMode()) {
            case "3D": {
                RenderUtil.drawBoxAroundEntity(target, 1, 0.0, 0.0, rgb, false);
                break;
            }
            case "2D": {
                RenderUtil.drawBoxAroundEntity(target, 3, 0.0, 0.0, rgb, false);
                break;
            }
            case "Health": {
                RenderUtil.drawBoxAroundEntity(target, 4, 0.0, 0.0, rgb, false);
            }
        }
    }
}

