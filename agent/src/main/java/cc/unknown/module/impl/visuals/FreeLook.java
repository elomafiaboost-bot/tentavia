/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.Display
 */
package cc.unknown.module.impl.visuals;

import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import org.lwjgl.opengl.Display;

@Register(name="FreeLook", category=Category.Visuals)
public class FreeLook
extends Module {
    private static boolean perspectiveToggled = false;
    private static float cameraYaw = 0.0f;
    private static float cameraPitch = 0.0f;
    private int previousPerspective = 0;

    @Override
    public void onEnable() {
        perspectiveToggled = !perspectiveToggled;
        cameraYaw = FreeLook.mc.field_71439_g.field_70177_z;
        cameraPitch = FreeLook.mc.field_71439_g.field_70125_A;
        if (perspectiveToggled) {
            this.previousPerspective = FreeLook.mc.field_71474_y.field_74320_O;
            FreeLook.mc.field_71474_y.field_74320_O = 1;
        } else {
            FreeLook.mc.field_71474_y.field_74320_O = this.previousPerspective;
        }
    }

    @Override
    public void onDisable() {
        this.resetPerspective();
    }

    public boolean overrideMouse() {
        if (FreeLook.mc.field_71415_G && Display.isActive()) {
            if (!perspectiveToggled) {
                return true;
            }
            FreeLook.mc.field_71417_B.func_74374_c();
            float f1 = FreeLook.mc.field_71474_y.field_74341_c * 0.6f + 0.2f;
            float f2 = f1 * f1 * f1 * 8.0f;
            float f3 = (float)FreeLook.mc.field_71417_B.field_74377_a * f2;
            float f4 = (float)FreeLook.mc.field_71417_B.field_74375_b * f2;
            cameraYaw += f3 * 0.15f;
            cameraPitch -= f4 * 0.15f;
            if (cameraPitch > 90.0f) {
                cameraPitch = 90.0f;
            }
            if (cameraPitch < -90.0f) {
                cameraPitch = -90.0f;
            }
        }
        return false;
    }

    private void resetPerspective() {
        perspectiveToggled = false;
        FreeLook.mc.field_71474_y.field_74320_O = this.previousPerspective;
    }

    public static boolean isPerspectiveToggled() {
        return perspectiveToggled;
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }
}

