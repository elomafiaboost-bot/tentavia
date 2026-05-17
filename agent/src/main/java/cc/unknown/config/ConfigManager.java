/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonSyntaxException
 */
package cc.unknown.config;

import cc.unknown.Haru;
import cc.unknown.config.Config;
import cc.unknown.module.impl.Module;
import cc.unknown.utils.Loona;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Objects;

public class ConfigManager
implements Loona {
    public final File configDirectory;
    private Config config;
    private final ArrayList<Config> configs;

    public ConfigManager() {
        this.configDirectory = new File(ConfigManager.mc.field_71412_D + File.separator + "Haru" + File.separator + "configs");
        this.configs = new ArrayList();
        if (!this.configDirectory.isDirectory()) {
            this.configDirectory.mkdirs();
        }
        this.discoverConfigs();
        File defaultFile = new File(this.configDirectory, "default.haru");
        this.config = new Config(defaultFile);
        if (!defaultFile.exists()) {
            this.save();
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private boolean isOutdated(File file) {
        JsonParser jsonParser = new JsonParser();
        try (FileReader reader = new FileReader(file);){
            JsonElement jsonElement = jsonParser.parse((Reader)reader);
            boolean bl = !jsonElement.isJsonObject();
            return bl;
        }
        catch (JsonSyntaxException | IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void discoverConfigs() {
        this.configs.clear();
        if (this.configDirectory.listFiles() == null || Objects.requireNonNull(this.configDirectory.listFiles()).length <= 0) {
            return;
        }
        for (File file : Objects.requireNonNull(this.configDirectory.listFiles())) {
            if (!file.getName().endsWith(".haru") || this.isOutdated(file)) continue;
            this.configs.add(new Config(new File(file.getPath())));
        }
    }

    public Config getConfig() {
        return this.config;
    }

    public void save() {
        JsonObject data = new JsonObject();
        JsonObject modules = new JsonObject();
        for (Module module : Haru.instance.getModuleManager().getModule()) {
            modules.add(module.getRegister().name(), (JsonElement)module.getConfigAsJson());
        }
        data.add("modules", (JsonElement)modules);
        this.config.save(data);
    }

    public void setConfig(Config config) {
        this.config = config;
        JsonObject data = Objects.requireNonNull(config.getData()).get("modules").getAsJsonObject();
        ArrayList<Module> knownModules = new ArrayList<Module>(Haru.instance.getModuleManager().getModule());
        for (Module module : knownModules) {
            if (data.has(module.getRegister().name())) {
                module.applyConfigFromJson(data.get(module.getRegister().name()).getAsJsonObject());
                continue;
            }
            module.resetToDefaults();
        }
    }

    public void loadConfigByName(String replace) {
        this.discoverConfigs();
        for (Config config : this.configs) {
            if (!config.getName().equals(replace)) continue;
            this.setConfig(config);
        }
    }

    public ArrayList<Config> getConfigs() {
        this.discoverConfigs();
        return this.configs;
    }

    public void copyConfig(Config config, String s) {
        File file = new File(this.configDirectory, s);
        Config newConfig = new Config(file);
        newConfig.save(config.getData());
    }

    public void resetConfig() {
        for (Module module : Haru.instance.getModuleManager().getModule()) {
            module.resetToDefaults();
        }
        this.save();
    }

    public void deleteConfig(Config config) {
        config.file.delete();
        if (config.getName().equals(this.config.getName())) {
            this.discoverConfigs();
            if (this.configs.size() < 2) {
                this.resetConfig();
                File defaultFile = new File(this.configDirectory, "default.haru");
                this.config = new Config(defaultFile);
                this.save();
            } else {
                this.config = this.configs.get(0);
            }
            this.save();
        }
    }
}

