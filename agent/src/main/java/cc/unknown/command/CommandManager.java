/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.command;

import cc.unknown.Haru;
import cc.unknown.command.Command;
import cc.unknown.command.commands.BindCommand;
import cc.unknown.command.commands.CategoryCommand;
import cc.unknown.command.commands.ClearCommand;
import cc.unknown.command.commands.ConfigCommand;
import cc.unknown.command.commands.FriendCommand;
import cc.unknown.command.commands.GameCommand;
import cc.unknown.command.commands.HelpCommand;
import cc.unknown.command.commands.PingCommand;
import cc.unknown.command.commands.SpyCommand;
import cc.unknown.command.commands.ToggleCommand;
import cc.unknown.command.commands.TransactionCommand;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.ChatSendEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.setting.Setting;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.module.setting.impl.ModeValue;
import cc.unknown.module.setting.impl.SliderValue;
import cc.unknown.utils.player.PlayerUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
    private List<Command> commands = new ArrayList<Command>();
    private String prefix = ".";

    public CommandManager() {
        Haru.instance.getEventBus().register(this);
        this.add(new ConfigCommand(), new HelpCommand(), new BindCommand(), new CategoryCommand(), new ToggleCommand(), new FriendCommand(), new TransactionCommand(), new ClearCommand(), new GameCommand(), new PingCommand(), new SpyCommand());
    }

    @EventLink
    public void onChatSend(ChatSendEvent e) {
        try {
            String message = e.getMessage();
            if (message.startsWith(this.prefix)) {
                e.setCancelled(true);
                message = message.substring(1);
                String[] arguments = message.split(" ");
                String cmdName = arguments[0];
                for (Command cmd : this.commands) {
                    if (!cmd.name.equalsIgnoreCase(cmdName) && !cmd.alias.equalsIgnoreCase(cmdName)) continue;
                    String[] args = Arrays.copyOfRange(arguments, 1, arguments.length);
                    cmd.onExecute(args);
                    return;
                }
                for (Module module : Haru.instance.getModuleManager().getModule()) {
                    if (!module.getRegister().name().equalsIgnoreCase(cmdName) || arguments.length <= 1) continue;
                    if (module.getSettingAlternative(arguments[1]) != null) {
                        Setting setting = module.getSettingAlternative(arguments[1]);
                        try {
                            if (setting instanceof BooleanValue) {
                                ((BooleanValue)setting).setEnabled(Boolean.parseBoolean(arguments[2]));
                                continue;
                            }
                            if (setting instanceof SliderValue) {
                                ((SliderValue)setting).setValue(Double.parseDouble(arguments[2]));
                                continue;
                            }
                            if (setting instanceof DoubleSliderValue) {
                                ((DoubleSliderValue)setting).setValueMin(Double.parseDouble(arguments[2]));
                                ((DoubleSliderValue)setting).setValueMax(Double.parseDouble(arguments[3]));
                                continue;
                            }
                            if (!(setting instanceof ModeValue)) continue;
                            ((ModeValue)setting).setMode(arguments[2]);
                            continue;
                        }
                        catch (NumberFormatException ignored) {
                            return;
                        }
                        catch (ArrayIndexOutOfBoundsException ignored) {
                            return;
                        }
                    }
                    PlayerUtil.send("\u00a7c'" + arguments[1] + "' setting doesn't exist", new Object[0]);
                    return;
                }
            }
        }
        catch (NullPointerException nullPointerException) {
            // empty catch block
        }
    }

    public Command getCommand(Class<? extends Command> clazz) {
        return this.commands.stream().filter(command -> command.getClass().equals(clazz)).findFirst().orElse(null);
    }

    private void add(Command ... c) {
        this.commands.addAll(Arrays.asList(c));
    }

    public List<Command> getCommand() {
        return this.commands;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return this.prefix;
    }
}

