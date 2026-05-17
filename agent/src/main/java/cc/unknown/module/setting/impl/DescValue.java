/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package cc.unknown.module.setting.impl;

import cc.unknown.module.setting.Setting;
import com.google.gson.JsonObject;

public class DescValue
extends Setting {
    private String desc;

    public DescValue(String t) {
        super(t);
        this.desc = t;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String t) {
        this.desc = t;
    }

    @Override
    public void resetToDefaults() {
        this.desc = "";
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", this.getSettingType());
        data.addProperty("value", this.getDesc());
        return data;
    }

    @Override
    public String getSettingType() {
        return "desc";
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if (!data.get("type").getAsString().equals(this.getSettingType())) {
            return;
        }
        this.setDesc(data.get("value").getAsString());
    }
}

