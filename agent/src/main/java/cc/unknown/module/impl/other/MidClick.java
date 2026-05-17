/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemEnderPearl
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumChatFormatting
 */
package cc.unknown.module.impl.other;

import cc.unknown.Haru;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.other.MouseEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.utils.player.FriendUtil;
import cc.unknown.utils.player.PlayerUtil;
import java.awt.AWTException;
import java.awt.Robot;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

@Register(name="Midclick", category=Category.Other)
public class MidClick
extends Module {
    private AtomicBoolean x = new AtomicBoolean(false);
    private AtomicInteger prevSlot = new AtomicInteger(0);
    private Robot bot;
    private AtomicInteger pearlEvent = new AtomicInteger(4);
    private ModeValue mode = new ModeValue("Mode", "Add/Remove friend", "Add/Remove friend", "Throw pearl");
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public MidClick() {
        this.registerSetting(this.mode);
    }

    @Override
    public void onEnable() {
        try {
            this.bot = new Robot();
        }
        catch (AWTException x) {
            this.disable();
        }
    }

    @EventLink
    public void onMouse(MouseEvent e) {
        if (MidClick.mc.field_71462_r != null) {
            return;
        }
        if (this.pearlEvent.get() < 4) {
            if (this.pearlEvent.get() == 3) {
                MidClick.mc.field_71439_g.field_71071_by.field_70461_c = this.prevSlot.get();
            }
            this.pearlEvent.incrementAndGet();
        }
        if (!this.x.get() && e.getButton() == 2) {
            if (this.mode.is("Add/Remove friend") && MidClick.mc.field_71476_x.field_72308_g instanceof EntityPlayer) {
                EntityPlayer playerHit = (EntityPlayer)MidClick.mc.field_71476_x.field_72308_g;
                if (!FriendUtil.instance.isAFriend((Entity)playerHit)) {
                    FriendUtil.instance.addFriend((Entity)playerHit);
                    if (Haru.instance.getClientConfig() != null) {
                        Haru.instance.getClientConfig().saveConfig();
                    }
                    PlayerUtil.send(EnumChatFormatting.GRAY + playerHit.func_70005_c_() + " was added to your friends.", new Object[0]);
                } else {
                    FriendUtil.instance.removeFriend((Entity)playerHit);
                    if (Haru.instance.getClientConfig() != null) {
                        Haru.instance.getClientConfig().saveConfig();
                    }
                    PlayerUtil.send(EnumChatFormatting.GRAY + playerHit.func_70005_c_() + " was removed from your friends.", new Object[0]);
                }
            }
            if (this.mode.is("Throw pearl")) {
                for (int s = 0; s <= 8; ++s) {
                    ItemStack item = MidClick.mc.field_71439_g.field_71071_by.func_70301_a(s);
                    if (item == null || !(item.func_77973_b() instanceof ItemEnderPearl)) continue;
                    this.prevSlot.set(MidClick.mc.field_71439_g.field_71071_by.field_70461_c);
                    MidClick.mc.field_71439_g.field_71071_by.field_70461_c = s;
                    this.executorService.execute(() -> {
                        this.bot.mousePress(4);
                        this.bot.mouseRelease(4);
                    });
                    this.pearlEvent.set(0);
                    this.x.set(true);
                    return;
                }
            }
        }
        this.x.set(e.getButton() == 2);
    }
}

