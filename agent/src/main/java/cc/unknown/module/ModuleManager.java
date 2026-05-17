/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.FontRenderer
 */
package cc.unknown.module;

import cc.unknown.Haru;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.combat.AimAssist;
import cc.unknown.module.impl.combat.AutoClick;
import cc.unknown.module.impl.combat.AutoRefill;
import cc.unknown.module.impl.combat.Criticals;
import cc.unknown.module.impl.combat.JumpReset;
import cc.unknown.module.impl.combat.KeepSprint;
import cc.unknown.module.impl.combat.Reach;
import cc.unknown.module.impl.combat.Velocity;
import cc.unknown.module.impl.combat.WTap;
import cc.unknown.module.impl.exploit.BackTrack;
import cc.unknown.module.impl.exploit.ChatBypass;
import cc.unknown.module.impl.exploit.FakeLag;
import cc.unknown.module.impl.exploit.LagRange;
import cc.unknown.module.impl.exploit.PingSpoof;
import cc.unknown.module.impl.exploit.Timer;
import cc.unknown.module.impl.exploit.TimerRange;
import cc.unknown.module.impl.other.AntiFireBall;
import cc.unknown.module.impl.other.AutoLeave;
import cc.unknown.module.impl.other.AutoTool;
import cc.unknown.module.impl.other.Autoplay;
import cc.unknown.module.impl.other.Inventory;
import cc.unknown.module.impl.other.MidClick;
import cc.unknown.module.impl.other.SelfDestruct;
import cc.unknown.module.impl.player.AntiVoid;
import cc.unknown.module.impl.player.Blink;
import cc.unknown.module.impl.player.BridgeAssist;
import cc.unknown.module.impl.player.FastPlace;
import cc.unknown.module.impl.player.InvManager;
import cc.unknown.module.impl.player.LegitScaffold;
import cc.unknown.module.impl.player.NoFall;
import cc.unknown.module.impl.player.NoSlow;
import cc.unknown.module.impl.player.Sprint;
import cc.unknown.module.impl.player.Stealer;
import cc.unknown.module.impl.settings.Tweaks;
import cc.unknown.module.impl.visuals.Ambience;
import cc.unknown.module.impl.visuals.ClickGuiModule;
import cc.unknown.module.impl.visuals.CpsDisplay;
import cc.unknown.module.impl.visuals.ESP;
import cc.unknown.module.impl.visuals.FreeLook;
import cc.unknown.module.impl.visuals.Fullbright;
import cc.unknown.module.impl.visuals.HUD;
import cc.unknown.module.impl.visuals.Keystrokes;
import cc.unknown.module.impl.visuals.Nametags;
import cc.unknown.module.impl.visuals.TargetHUD;
import cc.unknown.module.impl.visuals.Trajectories;
import cc.unknown.utils.Loona;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.gui.FontRenderer;

public class ModuleManager
implements Loona {
    private final List<Module> modules = new ArrayList<Module>();
    private boolean initialized = false;

    public ModuleManager() {
        if (this.initialized) {
            return;
        }
        this.addModule(new AutoClick(), new AimAssist(), new AutoRefill(), new JumpReset(), new KeepSprint(), new Criticals(), new Reach(), new WTap(), new Velocity(), new ChatBypass(), new PingSpoof(), new FakeLag(), new LagRange(), new TimerRange(), new Timer(), new BackTrack(), new AntiFireBall(), new Autoplay(), new AutoLeave(), new AutoTool(), new Tweaks(), new SelfDestruct(), new MidClick(), new Inventory(), new AntiVoid(), new InvManager(), new Stealer(), new FastPlace(), new LegitScaffold(), new BridgeAssist(), new Sprint(), new Blink(), new NoSlow(), new NoFall(), new Ambience(), new Fullbright(), new FreeLook(), new Keystrokes(), new ClickGuiModule(), new HUD(), new CpsDisplay(), new TargetHUD(), new Trajectories(), new Nametags(), new ESP());
        this.initialized = true;
    }

    public void addModule(Module ... s) {
        this.modules.addAll(Arrays.asList(s));
    }

    public Module getModule(String name) {
        return this.initialized ? (Module)this.modules.stream().filter(module -> module.getRegister().name().equalsIgnoreCase(name)).findFirst().orElse(null) : null;
    }

    public Module getModule(Class<? extends Module> clazz) {
        return this.initialized ? (Module)this.modules.stream().filter(module -> module.getClass().equals(clazz)).findFirst().orElse(null) : null;
    }

    public List<Module> getModule() {
        return this.modules;
    }

    public List<Module> getModule(Class<?>[] classes) {
        return this.initialized ? this.modules.stream().filter(module -> Arrays.stream(classes).anyMatch(clazz -> module.getClass().equals(clazz))).collect(Collectors.toList()) : Collections.emptyList();
    }

    public List<Module> getCategory(Category category) {
        return this.initialized ? this.modules.stream().filter(module -> module.getRegister().category().equals((Object)category)).collect(Collectors.toList()) : Collections.emptyList();
    }

    public void sort() {
        HUD hud = (HUD)Haru.instance.getModuleManager().getModule(HUD.class);
        this.modules.sort((o1, o2) -> ModuleManager.mc.field_71466_p.func_78256_a(o2.getRegister().name() + (hud.suffix.isToggled() ? o2.getSuffix() : "")) - ModuleManager.mc.field_71466_p.func_78256_a(o1.getRegister().name() + (hud.suffix.isToggled() ? o1.getSuffix() : "")));
    }

    public int getLongestActiveModule(FontRenderer fontRenderer) {
        return this.initialized ? this.modules.stream().filter(Module::isEnabled).mapToInt(module -> fontRenderer.func_78256_a(module.getRegister().name())).max().orElse(0) : 0;
    }

    public int getBoxHeight(FontRenderer fontRenderer, int margin) {
        return this.initialized ? this.modules.stream().filter(Module::isEnabled).mapToInt(module -> fontRenderer.field_78288_b + margin).sum() : 0;
    }

    public int numberOfModules() {
        return this.modules.size();
    }
}

