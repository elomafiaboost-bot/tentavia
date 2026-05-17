/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.ui.clickgui.raven.impl.api;

import cc.unknown.Haru;
import cc.unknown.module.impl.visuals.ClickGuiModule;
import cc.unknown.utils.client.ColorUtil;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum Theme {
    instance;

    private Supplier<Color> backColorSupplier = () -> new Color(0, 0, 0, 100);
    private final Map<String, Supplier<Color>> colorMap = new HashMap<String, Supplier<Color>>();

    private Theme() {
        this.colorMap.put("Lilith", () -> ColorUtil.reverseGradientDraw(new Color(76, 56, 108), new Color(255, 51, 51), new Color(76, 56, 108), 5));
        this.colorMap.put("Rainbow", () -> Color.getHSBColor((float)(System.currentTimeMillis() % 5000L) / 5000.0f, 1.0f, 1.0f));
        this.colorMap.put("Pastel", () -> ColorUtil.reverseGradientDraw(new Color(255, 190, 190), new Color(255, 190, 255), 2));
        this.colorMap.put("Memories", () -> ColorUtil.reverseGradientDraw(new Color(255, 0, 255), new Color(255, 255, 0), new Color(255, 0, 158), 2));
        this.colorMap.put("Cantina", () -> ColorUtil.gradientDraw(new Color(255, 0, 0), new Color(0, 0, 255), 7));
    }

    public Color getMainColor() {
        ClickGuiModule clickgui = (ClickGuiModule)Haru.instance.getModuleManager().getModule(ClickGuiModule.class);
        return this.colorMap.getOrDefault(clickgui.clientTheme.getMode(), () -> Color.getHSBColor(clickgui.clickGuiColor.getInputToFloat() % 360.0f / 360.0f, clickgui.saturation.getInputToFloat(), clickgui.brightness.getInputToFloat())).get();
    }

    public Color getBackColor() {
        return this.backColorSupplier.get();
    }
}

