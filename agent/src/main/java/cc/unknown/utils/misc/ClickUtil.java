/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.util.internal.ThreadLocalRandom
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLiquid
 *  net.minecraft.client.gui.inventory.GuiChest
 *  net.minecraft.client.gui.inventory.GuiInventory
 *  net.minecraft.client.settings.KeyBinding
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemBow
 *  net.minecraft.item.ItemBucketMilk
 *  net.minecraft.item.ItemFishingRod
 *  net.minecraft.item.ItemFood
 *  net.minecraft.item.ItemPotion
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.util.BlockPos
 *  net.minecraft.util.MovingObjectPosition$MovingObjectType
 *  org.lwjgl.input.Mouse
 */
package cc.unknown.utils.misc;

import cc.unknown.Haru;
import cc.unknown.module.impl.combat.AutoClick;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.Loona;
import cc.unknown.utils.player.PlayerUtil;
import io.netty.util.internal.ThreadLocalRandom;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

public enum ClickUtil implements Loona
{
    instance;

    private long leftk;
    private long leftl;
    private double leftm;
    private boolean leftn;
    private boolean breakHeld;
    private long lastLeftClick;
    private long leftHold;
    private boolean leftDown;
    private long righti;
    private long rightj;
    private long rightk;
    private long rightl;
    private double rightm;
    private boolean rightn;
    private long lastRightClick;
    private long rightHold;
    private boolean rightDown;
    private long leftDownTime;
    private long leftUpTime;
    private Random rand = null;

    public void megumiLeftClick() {
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        double speedLeft1 = 1.0 / ThreadLocalRandom.current().nextGaussian() * clicker.getLeftCPS().getInputMax() + clicker.getLeftCPS().getInputMax();
        double leftHoldLength = speedLeft1 / ThreadLocalRandom.current().nextGaussian() * clicker.getLeftCPS().getInputMin() + clicker.getLeftCPS().getInputMin();
        Mouse.poll();
        if (ClickUtil.mc.field_71462_r != null || !ClickUtil.mc.field_71415_G || this.checkScreen() || this.checkHit()) {
            return;
        }
        if (Mouse.isButtonDown((int)0)) {
            if (this.breakBlockLogic() || clicker.getWeaponOnly().isToggled() && !PlayerUtil.isHoldingWeapon()) {
                return;
            }
            double speedLeft = 1.0 / ThreadLocalRandom.current().nextDouble(clicker.getLeftCPS().getInputMin() - 0.2, clicker.getLeftCPS().getInputMax());
            if ((double)(System.currentTimeMillis() - this.lastLeftClick) > speedLeft * 1000.0) {
                this.lastLeftClick = System.currentTimeMillis();
                if (this.leftHold < this.lastLeftClick) {
                    this.leftHold = this.lastLeftClick;
                }
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i(), (boolean)true);
                KeyBinding.func_74507_a((int)ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i());
            } else if ((double)this.leftHold > leftHoldLength * 1000.0) {
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i(), (boolean)false);
            }
        }
    }

    public void kuruLeftClick() {
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        double speedLeft1 = 1.0 / ThreadLocalRandom.current().nextDouble(clicker.getLeftCPS().getInputMin() - 0.2, clicker.getLeftCPS().getInputMax());
        double leftHoldLength = speedLeft1 / ThreadLocalRandom.current().nextDouble(clicker.getLeftCPS().getInputMin() - 0.02, clicker.getLeftCPS().getInputMax());
        Mouse.poll();
        if (ClickUtil.mc.field_71462_r != null || !ClickUtil.mc.field_71415_G || this.checkScreen() || this.checkHit()) {
            return;
        }
        if (Mouse.isButtonDown((int)0)) {
            if (this.breakBlockLogic() || clicker.getWeaponOnly().isToggled() && !PlayerUtil.isHoldingWeapon()) {
                return;
            }
            double speedLeft = 1.0 / ThreadLocalRandom.current().nextDouble(clicker.getLeftCPS().getInputMin() - 0.2, clicker.getLeftCPS().getInputMax());
            if ((double)(System.currentTimeMillis() - this.lastLeftClick) > speedLeft * 1000.0) {
                this.lastLeftClick = System.currentTimeMillis();
                if (this.leftHold < this.lastLeftClick) {
                    this.leftHold = this.lastLeftClick;
                }
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i(), (boolean)true);
                KeyBinding.func_74507_a((int)ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i());
            } else if ((double)(System.currentTimeMillis() - this.leftHold) > leftHoldLength * 1000.0) {
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i(), (boolean)false);
            }
        }
    }

    public void ravenLeftClick() {
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        if (ClickUtil.mc.field_71462_r != null || !ClickUtil.mc.field_71415_G || this.checkScreen() || this.checkHit()) {
            return;
        }
        Mouse.poll();
        if (!Mouse.isButtonDown((int)0) && !this.leftDown) {
            KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i(), (boolean)false);
        }
        if (Mouse.isButtonDown((int)0) || this.leftDown) {
            if (clicker.getWeaponOnly().isToggled() && !PlayerUtil.isHoldingWeapon()) {
                return;
            }
            this.leftClickExecute(ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i());
        }
    }

    public void leftClickExecute(int key) {
        if (this.breakBlockLogic()) {
            return;
        }
        if (this.leftUpTime > 0L && this.leftDownTime > 0L) {
            if (System.currentTimeMillis() > this.leftUpTime && this.leftDown) {
                KeyBinding.func_74510_a((int)key, (boolean)true);
                KeyBinding.func_74507_a((int)key);
                this.genLeftTimings();
                this.leftDown = false;
            } else if (System.currentTimeMillis() > this.leftDownTime) {
                KeyBinding.func_74510_a((int)key, (boolean)false);
                this.leftDown = true;
            }
        } else {
            this.genLeftTimings();
        }
    }

    public void genLeftTimings() {
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        double clickSpeed = this.ranModuleVal(clicker.getLeftCPS(), this.rand) + 0.4 * this.rand.nextDouble();
        long delay = (int)Math.round(1000.0 / clickSpeed);
        if (System.currentTimeMillis() > this.leftk) {
            if (!this.leftn && this.rand.nextInt(200) >= 85) {
                this.leftn = true;
                this.leftm = 1.1 + this.rand.nextDouble() * 0.15;
            } else {
                this.leftn = false;
            }
            this.leftk = System.currentTimeMillis() + 500L + (long)this.rand.nextInt(1500);
        }
        if (this.leftn) {
            delay = (long)((double)delay * this.leftm);
        }
        if (System.currentTimeMillis() > this.leftl) {
            if (this.rand.nextInt(125) >= 80) {
                delay += 50L + (long)this.rand.nextInt(100);
            }
            this.leftl = System.currentTimeMillis() + 500L + (long)this.rand.nextInt(1500);
        }
        this.leftUpTime = System.currentTimeMillis() + delay;
        this.leftDownTime = System.currentTimeMillis() + delay / 3L - (long)this.rand.nextInt(10);
    }

    public boolean hitSelectLogic() {
        Entity target;
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        if (clicker.getHitSelect().isToggled() && ClickUtil.mc.field_71476_x != null && ClickUtil.mc.field_71476_x.field_72313_a == MovingObjectPosition.MovingObjectType.ENTITY && (target = ClickUtil.mc.field_71476_x.field_72308_g) instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer)target;
            return PlayerUtil.lookingAtPlayer((EntityPlayer)ClickUtil.mc.field_71439_g, targetPlayer, clicker.getHitSelectDistance().getInput());
        }
        return false;
    }

    public boolean breakBlockLogic() {
        BlockPos p;
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        if (clicker.getBreakBlocks().isToggled() && ClickUtil.mc.field_71476_x != null && (p = ClickUtil.mc.field_71476_x.func_178782_a()) != null) {
            Block bl = ClickUtil.mc.field_71441_e.func_180495_p(p).func_177230_c();
            if (bl != Blocks.field_150350_a && !(bl instanceof BlockLiquid)) {
                if (!this.breakHeld) {
                    int e = ClickUtil.mc.field_71474_y.field_74312_F.func_151463_i();
                    KeyBinding.func_74510_a((int)e, (boolean)true);
                    KeyBinding.func_74507_a((int)e);
                    this.breakHeld = true;
                }
                return true;
            }
            if (this.breakHeld) {
                this.breakHeld = false;
            }
        }
        return false;
    }

    public void kuruRightClick() {
        AutoClick right = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        if (ClickUtil.mc.field_71462_r != null || !ClickUtil.mc.field_71415_G) {
            return;
        }
        double speedRight = 1.0 / ThreadLocalRandom.current().nextDouble(right.getRightCPS().getInputMin() - 0.2, right.getRightCPS().getInputMax());
        double rightHoldLength = speedRight / ThreadLocalRandom.current().nextDouble(right.getRightCPS().getInputMin() - 0.02, right.getRightCPS().getInputMax());
        if (!Mouse.isButtonDown((int)1) && !this.rightDown) {
            KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i(), (boolean)false);
        }
        if (Mouse.isButtonDown((int)1) || this.rightDown) {
            if (!this.rightClickAllowed()) {
                return;
            }
            if ((double)(System.currentTimeMillis() - this.lastRightClick) > speedRight * 1000.0) {
                this.lastRightClick = System.currentTimeMillis();
                if (this.rightHold < this.lastRightClick) {
                    this.rightHold = this.lastRightClick;
                }
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i(), (boolean)true);
                KeyBinding.func_74507_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i());
                this.rightDown = false;
            } else if ((double)(System.currentTimeMillis() - this.rightHold) > rightHoldLength * 1000.0) {
                this.rightDown = true;
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i(), (boolean)false);
            }
        }
    }

    public void megumiRightClick() {
        AutoClick right = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        if (ClickUtil.mc.field_71462_r != null || !ClickUtil.mc.field_71415_G) {
            return;
        }
        double speedRight = 1.0 / ThreadLocalRandom.current().nextGaussian() * right.getRightCPS().getInputMin() - 0.2 + right.getRightCPS().getInputMax();
        double rightHoldLength = speedRight / ThreadLocalRandom.current().nextGaussian() * right.getRightCPS().getInputMin() - 0.02 + right.getRightCPS().getInputMax();
        if (!Mouse.isButtonDown((int)1) && !this.rightDown) {
            KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i(), (boolean)false);
        }
        if (Mouse.isButtonDown((int)1) || this.rightDown) {
            if (!this.rightClickAllowed()) {
                return;
            }
            if ((double)(System.currentTimeMillis() - this.lastRightClick) > speedRight * 1000.0) {
                this.lastRightClick = System.currentTimeMillis();
                if (this.rightHold < this.lastRightClick) {
                    this.rightHold = this.lastRightClick;
                }
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i(), (boolean)true);
                KeyBinding.func_74507_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i());
                this.rightDown = false;
            } else if ((double)(System.currentTimeMillis() - this.rightHold) > rightHoldLength * 1000.0) {
                this.rightDown = true;
                KeyBinding.func_74510_a((int)ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i(), (boolean)false);
            }
        }
    }

    public void ravenRightClick() {
        if (ClickUtil.mc.field_71462_r != null || !ClickUtil.mc.field_71415_G) {
            return;
        }
        Mouse.poll();
        if (Mouse.isButtonDown((int)1)) {
            this.rightClickExecute(ClickUtil.mc.field_71474_y.field_74313_G.func_151463_i());
        } else if (!Mouse.isButtonDown((int)1)) {
            this.righti = 0L;
            this.rightj = 0L;
        }
    }

    public boolean rightClickAllowed() {
        AutoClick right = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        ItemStack item = ClickUtil.mc.field_71439_g.func_70694_bm();
        if (item != null) {
            if (item.func_77973_b() instanceof ItemSword) {
                return false;
            }
            if (item.func_77973_b() instanceof ItemFishingRod) {
                return false;
            }
            if (item.func_77973_b() instanceof ItemBow) {
                return false;
            }
            if (right.getAllowEat().isToggled() && (item.func_77973_b() instanceof ItemFood || item.func_77973_b() instanceof ItemPotion || item.func_77973_b() instanceof ItemBucketMilk)) {
                return false;
            }
            if (right.getOnlyBlocks().isToggled() && !(item.func_77973_b() instanceof ItemBlock)) {
                return false;
            }
        }
        return true;
    }

    private void rightClickExecute(int key) {
        if (!this.rightClickAllowed()) {
            return;
        }
        if (this.rightj > 0L && this.righti > 0L) {
            if (System.currentTimeMillis() > this.rightj) {
                KeyBinding.func_74510_a((int)key, (boolean)true);
                KeyBinding.func_74507_a((int)key);
                this.genRightTimings();
            } else if (System.currentTimeMillis() > this.righti) {
                KeyBinding.func_74510_a((int)key, (boolean)false);
            }
        } else {
            this.genRightTimings();
        }
    }

    public void genRightTimings() {
        AutoClick right = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        double clickSpeed = this.ranModuleVal(right.getRightCPS(), this.rand) + 0.4 * this.rand.nextDouble();
        long delay = (int)Math.round(1000.0 / clickSpeed);
        if (System.currentTimeMillis() > this.rightk) {
            if (!this.rightn && this.rand.nextInt(100) >= 85) {
                this.rightn = true;
                this.rightm = 1.1 + this.rand.nextDouble() * 0.15;
            } else {
                this.rightn = false;
            }
            this.rightk = System.currentTimeMillis() + 500L + (long)this.rand.nextInt(1500);
        }
        if (this.rightn) {
            delay = (long)((double)delay * this.rightm);
        }
        if (System.currentTimeMillis() > this.rightl) {
            if (this.rand.nextInt(100) >= 80) {
                delay += 50L + (long)this.rand.nextInt(100);
            }
            this.rightl = System.currentTimeMillis() + 500L + (long)this.rand.nextInt(1500);
        }
        this.rightj = System.currentTimeMillis() + delay;
        this.righti = System.currentTimeMillis() + delay / 2L - (long)this.rand.nextInt(10);
    }

    public boolean isClicking() {
        AutoClick clicker = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        if (clicker != null && clicker.isEnabled()) {
            return clicker.isEnabled() && Mouse.isButtonDown((int)0);
        }
        return false;
    }

    public double ranModuleVal(SliderValue a, SliderValue b, Random r) {
        return a.getInput() == b.getInput() ? a.getInput() : a.getInput() + r.nextDouble() * (b.getInput() - a.getInput());
    }

    public double ranModuleVal(DoubleSliderValue a, Random r) {
        return a.getInputMin() == a.getInputMax() ? a.getInputMin() : a.getInputMin() + r.nextDouble() * (a.getInputMax() - a.getInputMin());
    }

    public void setLeftDownTime(long leftDownTime) {
        this.leftDownTime = leftDownTime;
    }

    public void setLeftUpTime(long leftUpTime) {
        this.leftUpTime = leftUpTime;
    }

    public void setRand(Random rand) {
        this.rand = rand;
    }

    private boolean checkScreen() {
        return ClickUtil.mc.field_71462_r != null || ClickUtil.mc.field_71462_r instanceof GuiInventory || ClickUtil.mc.field_71462_r instanceof GuiChest;
    }

    private boolean checkHit() {
        AutoClick left = (AutoClick)Haru.instance.getModuleManager().getModule(AutoClick.class);
        return left.getHitSelect().isToggled() && !this.hitSelectLogic();
    }
}

