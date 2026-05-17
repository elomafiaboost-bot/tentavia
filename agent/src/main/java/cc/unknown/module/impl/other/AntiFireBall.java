/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.settings.KeyBinding
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.projectile.EntityFireball
 *  org.apache.commons.lang3.RandomUtils
 */
package cc.unknown.module.impl.other;

import cc.unknown.event.Event;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.LivingEvent;
import cc.unknown.event.impl.player.JumpEvent;
import cc.unknown.event.impl.player.StrafeEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.player.RotationUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import org.apache.commons.lang3.RandomUtils;

@Register(name="AntiFireBall", category=Category.Other)
public class AntiFireBall
extends Module {
    private DoubleSliderValue speed = new DoubleSliderValue("Rotation Speed", 98.0, 98.0, 1.0, 180.0, 1.0);
    private SliderValue range = new SliderValue("Range", 6.0, 1.0, 6.0, 0.01);
    private final BooleanValue moveFix = new BooleanValue("Move Fix", false);

    public AntiFireBall() {
        this.registerSetting(this.speed, this.range, this.moveFix);
    }

    @EventLink
    public void onFireball(Event event) {
        if (!(event instanceof StrafeEvent) && !(event instanceof JumpEvent)) {
            return;
        }
        for (Entity entity : AntiFireBall.mc.field_71441_e.field_72996_f) {
            EntityFireball fireball;
            if (!(entity instanceof EntityFireball) || (fireball = (EntityFireball)entity) == null || !this.moveFix.isToggled()) continue;
            if (event instanceof StrafeEvent) {
                ((StrafeEvent)event).setYaw(AntiFireBall.mc.field_71439_g.field_70177_z);
                continue;
            }
            if (!(event instanceof JumpEvent)) continue;
            ((JumpEvent)event).setYaw(AntiFireBall.mc.field_71439_g.field_70177_z);
        }
    }

    @EventLink
    public void onUpdate(LivingEvent e) {
        for (Entity entity : AntiFireBall.mc.field_71441_e.field_72996_f) {
            EntityFireball fire;
            if (!(entity instanceof EntityFireball) || !((double)AntiFireBall.mc.field_71439_g.func_70032_d((Entity)(fire = (EntityFireball)entity)) < this.range.getInput())) continue;
            RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.getServerRotation(), RotationUtils.getRotations((Entity)fire), RandomUtils.nextFloat((float)this.speed.getInputMinToFloat(), (float)this.speed.getInputMaxToFloat())));
            KeyBinding.func_74507_a((int)AntiFireBall.mc.field_71474_y.field_74312_F.func_151463_i());
        }
    }
}

