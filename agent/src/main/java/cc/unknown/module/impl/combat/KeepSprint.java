/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.module.impl.combat;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.HitSlowDownEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.SliderValue;

@Register(name="KeepSprint", category=Category.Combat)
public class KeepSprint
extends Module {
    private final SliderValue deffensive = new SliderValue("Defensive Motion", 0.6, 0.0, 1.0, 0.05);
    private final SliderValue offensive = new SliderValue("Offensive Motion", 1.0, 0.0, 1.0, 0.05);

    public KeepSprint() {
        this.registerSetting(this.deffensive, this.offensive);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.deffensive.getInput() + ", " + this.offensive.getInput() + "]");
    }

    @EventLink
    public void onHitSlowDown(HitSlowDownEvent e) {
        if (KeepSprint.mc.field_71439_g.field_70737_aN > 0) {
            e.setSlowDown(this.deffensive.getInput());
            e.setSprint(false);
        } else {
            e.setSlowDown(this.offensive.getInput());
            e.setSprint(true);
        }
    }
}

