/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraftforge.fml.relauncher.ReflectionHelper
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 */
package cc.unknown.module.impl.combat;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.misc.ClickUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.gui.GuiScreen;
import java.lang.reflect.Method;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Register(name="AutoClick", category=Category.Combat)
public class AutoClick
extends Module {
    private ModeValue clickMode = new ModeValue("Click Mode", "Left", "Left", "Right", "Both");
    private final DoubleSliderValue leftCPS = new DoubleSliderValue("Left Click Speed", 16.0, 19.0, 1.0, 80.0, 0.05);
    private final BooleanValue weaponOnly = new BooleanValue("Only Use Weapons", false);
    private final BooleanValue breakBlocks = new BooleanValue("Break Blocks", false);
    private final BooleanValue hitSelect = new BooleanValue("Precise Hit Selection", false);
    private final SliderValue hitSelectDistance = new SliderValue("Hit Range", 10.0, 1.0, 20.0, 5.0);
    private BooleanValue invClicker = new BooleanValue("Auto-Click in Inventory", false);
    private ModeValue invMode = new ModeValue("Inventory Click Mode", "Pre", "Pre", "Post");
    private SliderValue invDelay = new SliderValue("Click Tick Delay", 5.0, 0.0, 10.0, 1.0);
    private final DoubleSliderValue rightCPS = new DoubleSliderValue("Right Click Speed", 12.0, 16.0, 1.0, 80.0, 0.05);
    private final BooleanValue onlyBlocks = new BooleanValue("Only Use Blocks", false);
    private final BooleanValue allowEat = new BooleanValue("Allow Eating & Drinking", true);
    private final BooleanValue allowBow = new BooleanValue("Allow Using Bow", true);
    private ModeValue clickEvent = new ModeValue("Click Event", "Render", "Render", "Render 2", "Tick");
    private ModeValue clickStyle = new ModeValue("Click Style", "Raven", "Raven", "Kuru", "Megumi");
    private int invClick;

    public AutoClick() {
        this.registerSetting(this.clickMode, this.leftCPS, this.weaponOnly, this.breakBlocks, this.hitSelect, this.hitSelectDistance, this.invClicker, this.invMode, this.invDelay, this.rightCPS, this.onlyBlocks, this.allowEat, this.allowBow, this.clickEvent, this.clickStyle);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        AtomicReference<String> suffixRef = new AtomicReference<String>();
        if (this.clickMode.is("Left")) {
            suffixRef.set("- [" + this.leftCPS.getInputMinToInt() + ", " + this.leftCPS.getInputMaxToInt() + "]");
        } else if (this.clickMode.is("Right")) {
            suffixRef.set("- [" + this.rightCPS.getInputMinToInt() + ", " + this.rightCPS.getInputMaxToInt() + "]");
        }
        this.setSuffix((String)suffixRef.get());
    }

    @Override
    public void onEnable() {
        ClickUtil.instance.setRand(new Random());
    }

    @Override
    public void onDisable() {
        ClickUtil.instance.setLeftDownTime(0L);
        ClickUtil.instance.setLeftUpTime(0L);
    }

    @EventLink
    public void onMotion(MotionEvent e) {
        if (this.invClicker.isToggled() && (e.isPre() && this.invMode.is("Pre") || e.isPost() && this.invMode.is("Post")) && Mouse.isButtonDown((int)0) && (Keyboard.isKeyDown((int)54) || Keyboard.isKeyDown((int)42))) {
            ++this.invClick;
            this.inInvClick(AutoClick.mc.field_71462_r);
            return;
        }
    }

    @EventLink
    public void onRender(RenderEvent e) {
        if (this.clickEvent.is("Render 2") && e.is2D() || this.clickEvent.is("Render") && e.is3D()) {
            this.onClick();
        }
    }

    @EventLink
    public void onTick(TickEvent e) {
        if (this.clickEvent.is("Tick")) {
            this.onClick();
        }
    }

    private void onClick() {
        if (this.clickMode.is("Both")) {
            switch (this.clickStyle.getMode()) {
                case "Raven": {
                    ClickUtil.instance.ravenLeftClick();
                    ClickUtil.instance.ravenRightClick();
                    break;
                }
                case "Kuru": {
                    ClickUtil.instance.kuruLeftClick();
                    ClickUtil.instance.kuruRightClick();
                    break;
                }
                case "Megumi": {
                    ClickUtil.instance.megumiLeftClick();
                    ClickUtil.instance.megumiRightClick();
                }
            }
        } else if (this.clickMode.is("Left")) {
            switch (this.clickStyle.getMode()) {
                case "Raven": {
                    ClickUtil.instance.ravenLeftClick();
                    break;
                }
                case "Kuru": {
                    ClickUtil.instance.kuruLeftClick();
                    break;
                }
                case "Megumi": {
                    ClickUtil.instance.megumiLeftClick();
                }
            }
        } else if (this.clickMode.is("Right")) {
            switch (this.clickStyle.getMode()) {
                case "Raven": {
                    ClickUtil.instance.ravenRightClick();
                    break;
                }
                case "Kuru": {
                    ClickUtil.instance.kuruRightClick();
                    break;
                }
                case "Megumi": {
                    ClickUtil.instance.megumiRightClick();
                }
            }
        }
    }

    private void inInvClick(GuiScreen gui) {
        int x = Mouse.getX() * gui.field_146294_l / AutoClick.mc.field_71443_c;
        int y = gui.field_146295_m - Mouse.getY() * gui.field_146295_m / AutoClick.mc.field_71440_d - 1;
        try {
            if ((double)this.invClick >= this.invDelay.getInput()) {
                Method m = null;
                for (String name : new String[]{"func_73864_a", "mouseClicked"}) {
                    try { m = GuiScreen.class.getDeclaredMethod(name, int.class, int.class, int.class); break; }
                    catch (NoSuchMethodException ignored) {}
                }
                if (m != null) { m.setAccessible(true); m.invoke(gui, x, y, 0); }
                this.invClick = 0;
            }
        }
        catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
            // empty catch block
        }
    }

    public DoubleSliderValue getLeftCPS() {
        return this.leftCPS;
    }

    public DoubleSliderValue getRightCPS() {
        return this.rightCPS;
    }

    public BooleanValue getBreakBlocks() {
        return this.breakBlocks;
    }

    public BooleanValue getHitSelect() {
        return this.hitSelect;
    }

    public SliderValue getHitSelectDistance() {
        return this.hitSelectDistance;
    }

    public BooleanValue getAllowEat() {
        return this.allowEat;
    }

    public BooleanValue getAllowBow() {
        return this.allowBow;
    }

    public BooleanValue getWeaponOnly() {
        return this.weaponOnly;
    }

    public BooleanValue getOnlyBlocks() {
        return this.onlyBlocks;
    }
}

