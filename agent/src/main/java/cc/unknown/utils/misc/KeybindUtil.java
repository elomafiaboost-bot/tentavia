/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.utils.misc;

import cc.unknown.module.impl.Module;
import java.util.HashMap;
import java.util.Map;

public enum KeybindUtil {
    instance;

    private final Map<String, Integer> keyMap = new HashMap<String, Integer>();

    public void bind(Module mod, int bind) {
        mod.setKey(bind);
    }

    public void unbind(Module mod) {
        mod.setKey(0);
    }

    public int toInt(String keyCode) {
        return this.keyMap.getOrDefault(keyCode.toLowerCase(), 0);
    }

    private KeybindUtil() {
        this.keyMap.put("a", 30);
        this.keyMap.put("b", 48);
        this.keyMap.put("c", 46);
        this.keyMap.put("d", 32);
        this.keyMap.put("e", 18);
        this.keyMap.put("f", 33);
        this.keyMap.put("g", 34);
        this.keyMap.put("h", 35);
        this.keyMap.put("i", 23);
        this.keyMap.put("j", 36);
        this.keyMap.put("k", 37);
        this.keyMap.put("l", 38);
        this.keyMap.put("m", 50);
        this.keyMap.put("n", 49);
        this.keyMap.put("o", 24);
        this.keyMap.put("p", 25);
        this.keyMap.put("q", 16);
        this.keyMap.put("r", 19);
        this.keyMap.put("s", 31);
        this.keyMap.put("t", 20);
        this.keyMap.put("u", 22);
        this.keyMap.put("v", 47);
        this.keyMap.put("w", 17);
        this.keyMap.put("x", 45);
        this.keyMap.put("y", 21);
        this.keyMap.put("z", 44);
        this.keyMap.put("0", 11);
        this.keyMap.put("1", 2);
        this.keyMap.put("2", 3);
        this.keyMap.put("3", 4);
        this.keyMap.put("4", 5);
        this.keyMap.put("5", 6);
        this.keyMap.put("6", 7);
        this.keyMap.put("7", 8);
        this.keyMap.put("8", 9);
        this.keyMap.put("9", 10);
        this.keyMap.put("numpad0", 82);
        this.keyMap.put("numpad1", 79);
        this.keyMap.put("numpad2", 80);
        this.keyMap.put("numpad3", 81);
        this.keyMap.put("numpad4", 75);
        this.keyMap.put("numpad5", 76);
        this.keyMap.put("numpad6", 77);
        this.keyMap.put("numpad7", 71);
        this.keyMap.put("numpad8", 72);
        this.keyMap.put("numpad9", 73);
        this.keyMap.put("rshift", 54);
        this.keyMap.put("lshift", 42);
        this.keyMap.put("lcontrol", 29);
        this.keyMap.put("tab", 15);
        this.keyMap.put("strg", 29);
        this.keyMap.put("alt", 56);
    }
}

