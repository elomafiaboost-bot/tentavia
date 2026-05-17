/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.EnumChatFormatting
 */
package cc.unknown.command;

import cc.unknown.command.Flips;
import cc.unknown.utils.Loona;
import cc.unknown.utils.player.PlayerUtil;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.EnumChatFormatting;

public abstract class Command
implements Loona {
    private static final Map<String, EnumChatFormatting> colorMap = new HashMap<String, EnumChatFormatting>();
    final Flips flips = this.getClass().getAnnotation(Flips.class);
    public final String name = this.flips.name();
    public final String desc = this.flips.desc();
    public final String alias = this.flips.alias();
    public final String syntax = this.flips.alias();

    public abstract void onExecute(String[] var1);

    public String getColor(String colorName) {
        EnumChatFormatting color = colorMap.getOrDefault(colorName, EnumChatFormatting.RESET);
        return color.toString();
    }

    public void sendChat(Object text, Object ... text2) {
        String format = String.format(text.toString(), text2);
        PlayerUtil.send(format, new Object[0]);
    }

    public void clearChat() {
        Command.mc.field_71456_v.func_146158_b().func_146231_a();
    }

    static {
        colorMap.put("DarkAqua", EnumChatFormatting.DARK_AQUA);
        colorMap.put("Green", EnumChatFormatting.GREEN);
        colorMap.put("White", EnumChatFormatting.WHITE);
        colorMap.put("Red", EnumChatFormatting.RED);
        colorMap.put("Gold", EnumChatFormatting.GOLD);
        colorMap.put("Gray", EnumChatFormatting.GRAY);
        colorMap.put("Yellow", EnumChatFormatting.YELLOW);
        colorMap.put("Blue", EnumChatFormatting.BLUE);
    }
}

