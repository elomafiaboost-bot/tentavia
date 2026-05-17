/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package cc.unknown.module.setting.impl;

import cc.unknown.module.setting.Setting;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderValue
extends Setting {
    private final String name;
    private double value;
    private double max;
    private double min;
    private double interval;

    public SliderValue(String name, double value, double min, double max, double intervals) {
        super(name);
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.interval = intervals;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void resetToDefaults() {
        this.value = 0.0;
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", this.getSettingType());
        data.addProperty("value", (Number)this.getInput());
        return data;
    }

    @Override
    public String getSettingType() {
        return "slider";
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if (!data.get("type").getAsString().equals(this.getSettingType())) {
            return;
        }
        this.setValue(data.get("value").getAsDouble());
    }

    public double getInput() {
        return SliderValue.r(this.value, 2);
    }

    public float getInputToFloat() {
        return (float)this.getInput();
    }

    public int getInputToInt() {
        return (int)this.getInput();
    }

    public long getInputToLong() {
        return (long)this.getInput();
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public void setValue(double n) {
        n = SliderValue.check(n, this.min, this.max);
        this.value = n = (double)Math.round(n * (1.0 / this.interval)) / (1.0 / this.interval);
    }

    public static double check(double v, double i, double a) {
        v = Math.max(i, v);
        v = Math.min(a, v);
        return v;
    }

    public static double r(double v, int p) {
        if (p < 0) {
            return 0.0;
        }
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(p, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

