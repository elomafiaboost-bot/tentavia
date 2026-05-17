/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.module.impl.other;

import cc.unknown.Haru;
import cc.unknown.command.CommandManager;
import cc.unknown.module.ModuleManager;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.impl.visuals.ClickGuiModule;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.utils.Loona;
import java.io.File;
import java.util.Arrays;

@Register(name="SelfDestruct", category=Category.Other)
public class SelfDestruct
extends Module {
    private final File logsDirectory;
    private BooleanValue deleteLogs;
    private BooleanValue removePrefix;
    private BooleanValue removeClickgui;
    private BooleanValue removeBind;
    private BooleanValue hiddenModules;

    public SelfDestruct() {
        this.logsDirectory = new File(Loona.mc.field_71412_D + File.separator + "logs" + File.separator);
        this.deleteLogs = new BooleanValue("Delete logs", true);
        this.removePrefix = new BooleanValue("Remove Prefix", true);
        this.removeClickgui = new BooleanValue("Remove ClickGui", true);
        this.removeBind = new BooleanValue("Remove Binds", false);
        this.hiddenModules = new BooleanValue("Hidden Modules", true);
        this.registerSetting(this.deleteLogs, this.removePrefix, this.removeClickgui, this.removeBind, this.hiddenModules);
    }

    @Override
    public void onEnable() {
        if (this.deleteLogs.isToggled()) {
            this.deleteLogs();
        }
        CommandManager commandManager = Haru.instance.getCommandManager();
        ModuleManager moduleManager = Haru.instance.getModuleManager();
        ClickGuiModule clickGuiModule = (ClickGuiModule)moduleManager.getModule(ClickGuiModule.class);
        if (this.removePrefix.isToggled()) {
            commandManager.setPrefix(" ");
        }
        if (this.removeClickgui.isToggled() && clickGuiModule != null) {
            clickGuiModule.setKey(0);
        }
        if (this.removeBind.isToggled()) {
            moduleManager.getModule().forEach(m -> m.setKey(0));
        }
        if (this.hiddenModules.isToggled()) {
            moduleManager.getModule().forEach(m -> m.setToggled(false));
        }
        if (mc != null) {
            mc.func_147108_a(null);
        }
    }

    private void deleteLogs() {
        File[] files;
        if (this.logsDirectory.exists() && (files = this.logsDirectory.listFiles()) != null) {
            Arrays.stream(files).filter(file -> file.getName().endsWith("log.gz")).forEach(File::delete);
        }
    }
}

