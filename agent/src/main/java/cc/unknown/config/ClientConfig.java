/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.config;

import cc.unknown.Haru;
import cc.unknown.ui.clickgui.raven.impl.CategoryComp;
import cc.unknown.utils.Loona;
import cc.unknown.utils.client.FuckUtil;
import cc.unknown.utils.helpers.MathHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

public class ClientConfig
implements Loona {
    private final File configFile;
    private final File configDir;
    private final String fileName = "config";
    private final String clickGuiPos = "clickgui:pos:";

    public ClientConfig() {
        this.configDir = new File(ClientConfig.mc.field_71412_D, "Haru");
        if (!this.configDir.exists()) {
            this.configDir.mkdir();
        }
        this.configFile = new File(this.configDir, "config");
        if (!this.configFile.exists()) {
            try {
                this.configFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveConfig() {
        ArrayList<String> config = new ArrayList<String>();
        config.add("clickgui:pos:" + this.getClickGuiPos());
        config.add("HUDX:" + FuckUtil.instance.getArrayListX());
        config.add("HUDY:" + FuckUtil.instance.getArrayListY());
        config.add(FuckUtil.instance.WaifuX + FuckUtil.instance.getWaifuX());
        config.add(FuckUtil.instance.WaifuY + FuckUtil.instance.getWaifuY());
        try (PrintWriter writer = new PrintWriter(this.configFile);){
            for (String line : config) {
                writer.println(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void applyConfig() {
        List<String> config = this.parseConfigFile();
        HashMap<String, Action> cfg = new HashMap<String, Action>();
        cfg.put("clickgui:pos:", this::loadClickGuiCoords);
        cfg.put("HUDX:", hudX -> FuckUtil.instance.setArrayListX(Integer.parseInt(hudX)));
        cfg.put("HUDY:", hudY -> FuckUtil.instance.setArrayListY(Integer.parseInt(hudY)));
        cfg.put(FuckUtil.instance.WaifuX, waifuX -> FuckUtil.instance.setWaifuX(Integer.parseInt(waifuX)));
        cfg.put(FuckUtil.instance.WaifuY, waifuY -> FuckUtil.instance.setWaifuY(Integer.parseInt(waifuY)));
        block0: for (String line : config) {
            for (Map.Entry entry : cfg.entrySet()) {
                if (!line.startsWith((String)entry.getKey())) continue;
                ((Action)entry.getValue()).apply(line.replace((CharSequence)entry.getKey(), ""));
                continue block0;
            }
        }
    }

    private List<String> parseConfigFile() {
        ArrayList<String> configFileContents = new ArrayList<String>();
        try (Scanner reader = new Scanner(this.configFile);){
            while (reader.hasNextLine()) {
                configFileContents.add(reader.nextLine());
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return configFileContents;
    }

    private void loadClickGuiCoords(String decryptedString) {
        if (decryptedString == null || decryptedString.isEmpty()) {
            return;
        }
        for (String what : decryptedString.split("/")) {
            for (CategoryComp cat : Haru.instance.getHaruGui().getCategoryList()) {
                if (cat == null || cat.getCategory() == null || !what.startsWith(cat.getCategory().getName())) continue;
                try {
                    List<String> cfg = MathHelper.StringListToList(what.split("~"));
                    if (cfg.size() < 4) continue;
                    cat.setX(Integer.parseInt(cfg.get(1)));
                    cat.setY(Integer.parseInt(cfg.get(2)));
                    cat.setOpened(Boolean.parseBoolean(cfg.get(3)));
                }
                catch (IllegalArgumentException | IndexOutOfBoundsException runtimeException) {}
            }
        }
    }

    private String getClickGuiPos() {
        StringJoiner posConfig = new StringJoiner("/");
        for (CategoryComp cat : Haru.instance.getHaruGui().getCategoryList()) {
            posConfig.add(String.join((CharSequence)"~", cat.getCategory().getName(), String.valueOf(cat.getX()), String.valueOf(cat.getY()), String.valueOf(cat.isOpen())));
        }
        return posConfig.toString();
    }

    @FunctionalInterface
    public static interface Action {
        public void apply(String var1);
    }
}

