/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringUtils
 */
package cc.unknown.utils.helpers;

import cc.unknown.utils.Loona;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.StringUtils;

public class MathHelper
implements Loona {
    private static final Random rand = new Random();

    public static float wrapAngleTo90_float(float var) {
        if ((var %= 90.0f) >= 90.0f) {
            var -= 90.0f;
        }
        if (var < -90.0f) {
            var += 90.0f;
        }
        return var;
    }

    public static int simpleRandom(int min, int max) {
        int x = min;
        int y = max;
        if (min == max) {
            return min;
        }
        if (min > max) {
            x = max;
            y = min;
        }
        return ThreadLocalRandom.current().nextInt(x, y);
    }

    public static double simpleRandom(double min, double max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            double d = min;
            min = max;
            max = d;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static long nextLong(long origin, long bound) {
        return origin == bound ? origin : ThreadLocalRandom.current().nextLong(origin, bound);
    }

    public static Random rand() {
        return rand;
    }

    public static ArrayList<String> toArrayList(String[] x) {
        return new ArrayList<String>(Arrays.asList(x));
    }

    public static List<String> StringListToList(String[] whytho) {
        ArrayList<String> f = new ArrayList<String>();
        Collections.addAll(f, whytho);
        return f;
    }

    public static double round(double n, int d) {
        if (d == 0) {
            return Math.round(n);
        }
        double p = Math.pow(10.0, d);
        return (double)Math.round(n * p) / p;
    }

    public static float randomFloat(float x, float v) {
        return (float)(Math.random() * (double)(x - v) + (double)v);
    }

    public static int randomInt(double x, double v) {
        return (int)(Math.random() * (x - v) + v);
    }

    public static double randomDouble(double x, double v) {
        return Math.random() * (x - v) + v;
    }

    public static String str(String s) {
        char[] n = StringUtils.func_76338_a((String)s).toCharArray();
        StringBuilder v = new StringBuilder();
        for (char c : n) {
            if (c >= '\u007f' || c <= '\u0014') continue;
            v.append(c);
        }
        return v.toString();
    }
}

