/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package cc.unknown.module.setting.impl;

import cc.unknown.module.setting.Setting;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;

public class ModeValue
extends Setting {
    private int index;
    private List<String> list;

    public ModeValue(String name, String t, String ... list) {
        super(name);
        this.list = Arrays.asList(list);
        this.setMode(t);
    }

    public String getMode() {
        if (this.index >= this.list.size() || this.index < 0) {
            this.index = 0;
        }
        return this.list.get(this.index);
    }

    public void setMode(String mode) {
        this.index = this.list.indexOf(mode);
    }

    public boolean is(String mode) {
        if (this.index >= this.list.size() || this.index < 0) {
            this.index = 0;
        }
        return this.list.get(this.index).equals(mode);
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", this.getSettingType());
        data.addProperty("value", this.getMode());
        return data;
    }

    @Override
    public String getSettingType() {
        return "mode";
    }

    public void increment() {
        this.index = this.index < this.list.size() - 1 ? ++this.index : 0;
    }

    public void decrement() {
        this.index = this.index > 0 ? --this.index : this.list.size() - 1;
    }

    public int getIndex() {
        return this.index;
    }

    public List<String> getList() {
        return this.list;
    }

    @Override
    public void resetToDefaults() {
        this.setMode("");
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if (!data.get("type").getAsString().equals(this.getSettingType())) {
            return;
        }
        String value = data.get("value").getAsString();
        this.setMode(value);
    }
}

