/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonSyntaxException
 */
package cc.unknown.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Config {
    public final File file;

    public Config(File pathToFile) {
        this.file = pathToFile;
        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getName() {
        return this.file.getName().replace(".haru", "");
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public JsonObject getData() {
        JsonParser jsonParser = new JsonParser();
        try (FileReader reader = new FileReader(this.file);){
            JsonElement obj = jsonParser.parse((Reader)reader);
            JsonObject jsonObject = obj.isJsonNull() ? null : obj.getAsJsonObject();
            return jsonObject;
        }
        catch (JsonSyntaxException | IOException | ClassCastException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void save(JsonObject data) {
        data.addProperty("creationTime", this.getDate());
        try (PrintWriter out = new PrintWriter(new FileWriter(this.file));){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String formattedJson = gson.toJson((JsonElement)data);
            out.write(formattedJson);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date());
    }
}

