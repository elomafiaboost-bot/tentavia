/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown;

import cc.unknown.command.CommandManager;
import cc.unknown.config.ClientConfig;
import cc.unknown.config.ConfigManager;
import cc.unknown.event.impl.api.EventBus;
import cc.unknown.event.impl.other.GameEvent;
import cc.unknown.module.ModuleManager;
import cc.unknown.ui.clickgui.raven.HaruGui;
import cc.unknown.utils.player.RotationUtils;

public enum Haru {
    instance;

    public RotationUtils rotationUtils;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private ClientConfig clientConfig;
    private ModuleManager moduleManager;
    private HaruGui haruGui;
    private EventBus eventBus = new EventBus();

    public void startClient() {
        this.eventBus.post(new GameEvent.StartEvent());
        this.rotationUtils = new RotationUtils();
        this.commandManager = new CommandManager();
        this.moduleManager = new ModuleManager();
        this.haruGui = new HaruGui();
        this.configManager = new ConfigManager();
        this.clientConfig = new ClientConfig();
        this.clientConfig.applyConfig();
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ClientConfig getClientConfig() {
        return this.clientConfig;
    }

    public ModuleManager getModuleManager() {
        return this.moduleManager;
    }

    public HaruGui getHaruGui() {
        return this.haruGui;
    }

    public EventBus getEventBus() {
        return this.eventBus;
    }
}

