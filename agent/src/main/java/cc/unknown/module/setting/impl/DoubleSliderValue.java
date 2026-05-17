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

public class DoubleSliderValue
extends Setting {
    private final String name;
    private double valMax;
    private double valMin;
    private double max;
    private double min;
    private double interval;

    public DoubleSliderValue(String name, double valMin, double valMax, double min, double max, double intervals) {
        super(name);
        this.name = name;
        this.valMin = valMin;
        this.valMax = valMax;
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
        this.setValueMin(0.0);
        this.setValueMax(0.0);
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", this.getSettingType());
        data.addProperty("valueMin", (Number)this.getInputMin());
        data.addProperty("valueMax", (Number)this.getInputMax());
        return data;
    }

    @Override
    public String getSettingType() {
        return "doubleslider";
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if (!data.get("type").getAsString().equals(this.getSettingType())) {
            return;
        }
        this.setValueMax(data.get("valueMax").getAsDouble());
        this.setValueMin(data.get("valueMin").getAsDouble());
    }

    public double getInputMin() {
        return DoubleSliderValue.round(this.valMin, 2);
    }

    public double getInputMax() {
        return DoubleSliderValue.round(this.valMax, 2);
    }

    public long getInputMinToLong() {
        return (long)DoubleSliderValue.round(this.valMin, 2);
    }

    public long getInputMaxToLong() {
        return (long)DoubleSliderValue.round(this.valMax, 2);
    }

    public int getInputMinToInt() {
        return (int)DoubleSliderValue.round(this.valMin, 2);
    }

    public int getInputMaxToInt() {
        return (int)DoubleSliderValue.round(this.valMax, 2);
    }

    public float getInputMinToFloat() {
        return (float)DoubleSliderValue.round(this.valMin, 2);
    }

    public float getInputMaxToFloat() {
        return (float)DoubleSliderValue.round(this.valMax, 2);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public void setValueMin(double n) {
        n = DoubleSliderValue.correct(n, this.min, this.valMax);
        this.valMin = n = (double)Math.round(n * (1.0 / this.interval)) / (1.0 / this.interval);
    }

    public void setValueMax(double n) {
        n = DoubleSliderValue.correct(n, this.valMin, this.max);
        this.valMax = n = (double)Math.round(n * (1.0 / this.interval)) / (1.0 / this.interval);
    }

    public static double correct(double val, double min, double max) {
        val = Math.max(min, val);
        val = Math.min(max, val);
        return val;
    }

    public static double round(double val, int p) {
        if (p < 0) {
            return 0.0;
        }
        BigDecimal bd = new BigDecimal(val);
        bd = bd.setScale(p, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

