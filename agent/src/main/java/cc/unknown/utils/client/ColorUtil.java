/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 */
package cc.unknown.utils.client;

import java.awt.Color;
import org.lwjgl.opengl.GL11;

public class ColorUtil {
    public static Color blend(Color color, Color color1, double d0) {
        float f = (float)d0;
        float f1 = 1.0f - f;
        float[] afloat = new float[3];
        float[] afloat1 = new float[3];
        color.getColorComponents(afloat);
        color1.getColorComponents(afloat1);
        return new Color(afloat[0] * f + afloat1[0] * f1, afloat[1] * f + afloat1[1] * f1, afloat[2] * f + afloat1[2] * f1);
    }

    public static Color gradientDraw(Color color1, Color color2, int yLocation) {
        double angle = (double)System.currentTimeMillis() / 600.0 - (double)yLocation * 0.06;
        double normalizedSin = Math.cos(angle) * 0.5 + 0.5;
        int red = ColorUtil.interpolate(color1.getRed(), color2.getRed(), normalizedSin);
        int green = ColorUtil.interpolate(color2.getGreen(), color1.getGreen(), normalizedSin);
        int blue = ColorUtil.interpolate(color1.getBlue(), color2.getBlue(), normalizedSin);
        return new Color(red, green, blue);
    }

    private static int interpolate(int start, int end, double percent) {
        return (int)((double)start + (double)(end - start) * percent);
    }

    public static Color reverseGradientDraw(Color color1, Color color2, int yLocation) {
        double percent = Math.sin((double)System.currentTimeMillis() / 600.0 - (double)yLocation * 0.06) * 0.5 + 0.5;
        return new Color((int)((double)color1.getRed() + (double)(color2.getRed() - color1.getRed()) * percent), (int)((double)color1.getGreen() + (double)(color2.getGreen() - color1.getGreen()) * percent), (int)((double)color1.getBlue() + (double)(color2.getBlue() - color1.getBlue()) * percent));
    }

    public static Color reverseGradientDraw(Color color1, Color color2, Color color3, int yLocation) {
        double percent = Math.sin((double)System.currentTimeMillis() / 600.0 - (double)yLocation * 0.06) * 0.5 + 0.5;
        return new Color((int)((double)color1.getRed() + (double)(color2.getRed() - color1.getRed()) * percent), (int)((double)color1.getGreen() + (double)(color2.getGreen() - color1.getGreen()) * percent), (int)((double)color3.getBlue() + (double)(color3.getBlue() - color3.getBlue()) * percent));
    }

    public static void setColor(int color) {
        float alpha = 0.8f;
        float red = (float)(color >> 16 & 0xFF) / 255.0f;
        float green = (float)(color >> 8 & 0xFF) / 255.0f;
        float blue = (float)(color & 0xFF) / 255.0f;
        GL11.glColor4f((float)red, (float)green, (float)blue, (float)0.8f);
    }

    public static int rainbowDraw(long speed, long ... delay) {
        long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
        return Color.getHSBColor((float)(time % (15000L / speed)) / (15000.0f / (float)speed), 1.0f, 1.0f).getRGB();
    }
}

