/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package cc.unknown.module.setting.impl;

import cc.unknown.module.setting.Setting;
import com.google.gson.JsonObject;

public class BooleanValue
extends Setting {
    private final String name;
    private boolean isEnabled;

    public BooleanValue(String name, boolean isEnabled) {
        super(name);
        this.name = name;
        this.isEnabled = isEnabled;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void resetToDefaults() {
        this.isEnabled = false;
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", this.getSettingType());
        data.addProperty("value", Boolean.valueOf(this.isToggled()));
        return data;
    }

    @Override
    public String getSettingType() {
        return "bool";
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if (!data.get("type").getAsString().equals(this.getSettingType())) {
            return;
        }
        this.setEnabled(data.get("value").getAsBoolean());
    }

    public boolean isToggled() {
        return this.isEnabled;
    }

    public void toggle() {
        this.isEnabled = !this.isEnabled;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void setEnabled(boolean b) {
        this.isEnabled = b;
    }
}

