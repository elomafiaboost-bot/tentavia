/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  org.lwjgl.input.Keyboard
 */
package cc.unknown.module.impl;

import cc.unknown.Haru;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.Setting;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.utils.Loona;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.lwjgl.input.Keyboard;

public class Module
implements Loona {
    private List<Setting> settings = new ArrayList<Setting>();
    private String suffix = "";
    private boolean isToggled = false;
    private boolean enabled = false;
    private boolean hidden = true;
    private int key = 0;
    private Register register;

    public Module() {
        if (!this.getClass().isAnnotationPresent(Register.class)) {
            throw new RuntimeException("@Register not found" + this.getClass().getSimpleName());
        }
        this.register = this.getClass().getAnnotation(Register.class);
        this.key = this.getRegister().key();
        this.enabled = this.getRegister().enable();
    }

    public JsonObject getConfigAsJson() {
        JsonObject settings = new JsonObject();
        for (Setting setting : this.settings) {
            JsonObject settingData = setting.getConfigAsJson();
            settings.add(setting.getName(), (JsonElement)settingData);
        }
        JsonObject data = new JsonObject();
        data.addProperty("enabled", Boolean.valueOf(this.enabled));
        data.addProperty("keycode", (Number)this.key);
        data.add("settings", (JsonElement)settings);
        return data;
    }

    public void applyConfigFromJson(JsonObject data) {
        try {
            this.key = data.get("keycode").getAsInt();
            this.setToggled(data.get("enabled").getAsBoolean());
            JsonObject settingsData = data.get("settings").getAsJsonObject();
            for (Setting setting : this.getSettings()) {
                if (!settingsData.has(setting.getName())) continue;
                setting.applyConfigFromJson(settingsData.get(setting.getName()).getAsJsonObject());
            }
        }
        catch (NullPointerException nullPointerException) {
            // empty catch block
        }
    }

    public void keybind() {
        if (this.key != 0 && this.canBeEnabled()) {
            if (!this.isToggled && Keyboard.isKeyDown((int)this.key)) {
                this.toggle();
                this.isToggled = true;
            } else if (!Keyboard.isKeyDown((int)this.key)) {
                this.isToggled = false;
            }
        }
    }

    public Setting getSettingAlternative(String name) {
        for (Setting setting : this.settings) {
            String comparingName = setting.getName().replaceAll(" ", "");
            if (!comparingName.equalsIgnoreCase(name)) continue;
            return setting;
        }
        return null;
    }

    public boolean canBeEnabled() {
        return true;
    }

    public void enable() {
        this.enabled = true;
        this.onEnable();
        Haru.instance.getEventBus().register(this);
    }

    public void disable() {
        this.enabled = false;
        this.onDisable();
        Haru.instance.getEventBus().unregister(this);
    }

    public void setToggled(boolean enabled) {
        if (enabled) {
            this.enable();
        } else {
            this.disable();
        }
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public void registerSetting(Setting ... s) {
        this.settings.addAll(Arrays.asList(s));
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void guiButtonToggled(BooleanValue b) {
    }

    public void toggle() {
        if (this.enabled) {
            this.disable();
        } else {
            this.enable();
        }
    }

    public void resetToDefaults() {
        this.key = 0;
        this.setToggled(this.enabled);
        for (Setting setting : this.settings) {
            setting.resetToDefaults();
        }
    }

    public String getBindAsString() {
        return this.key == 0 ? "None" : Keyboard.getKeyName((int)this.key);
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Register getRegister() {
        return this.register;
    }
}

