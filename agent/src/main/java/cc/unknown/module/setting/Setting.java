/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package cc.unknown.module.setting;

import com.google.gson.JsonObject;

public abstract class Setting {
    public String settingName;

    public Setting(String name) {
        this.settingName = name;
    }

    public String getName() {
        return this.settingName;
    }

    public abstract void resetToDefaults();

    public abstract JsonObject getConfigAsJson();

    public abstract String getSettingType();

    public abstract void applyConfigFromJson(JsonObject var1);
}

