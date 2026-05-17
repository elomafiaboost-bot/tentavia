/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 */
package cc.unknown.command.commands;

import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.utils.player.FriendUtil;
import java.util.ArrayList;
import net.minecraft.entity.Entity;

@Flips(name="Friend", alias="fr", desc="It allows you to save a friend", syntax=".friend add <name>")
public class FriendCommand
extends Command {
    @Override
    public void onExecute(String[] args) {
        if (args.length == 0 || args.length == 1 && args[0].equalsIgnoreCase("list")) {
            this.listFriends();
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            Entity friendEntity = this.findEntity(args[1]);
            if (friendEntity != null) {
                if (args[0].equalsIgnoreCase("add")) {
                    this.addFriend(friendEntity);
                } else {
                    this.removeFriend(friendEntity);
                }
            } else {
                this.sendChat(this.getColor("Red") + " Player not found.", new Object[0]);
            }
        } else {
            this.sendChat(this.getColor("Red") + " Syntax Error.", new Object[0]);
        }
    }

    private void listFriends() {
        ArrayList<Entity> friends = FriendUtil.instance.getFriends();
        if (friends.isEmpty()) {
            this.sendChat(this.getColor("Gray") + " You have no friends. :(", new Object[0]);
        } else {
            this.sendChat(this.getColor("Gray") + " Your friends are:", new Object[0]);
            friends.stream().map(Entity::func_70005_c_).forEach(name -> this.sendChat(this.getColor("Gray") + name, new Object[0]));
        }
    }

    private void addFriend(Entity friendEntity) {
        FriendUtil.instance.addFriend(friendEntity);
        this.sendChat(this.getColor("Gray") + " New friend " + friendEntity.func_70005_c_() + " :)", new Object[0]);
    }

    private void removeFriend(Entity friendEntity) {
        boolean removed = FriendUtil.instance.removeFriend(friendEntity);
        if (removed) {
            this.sendChat(this.getColor("Gray") + " Successfully removed " + friendEntity.func_70005_c_() + " from your friends list!", new Object[0]);
        }
    }

    private Entity findEntity(String name) {
        return FriendCommand.mc.field_71441_e.func_72910_y().stream().filter(entity -> entity.func_70005_c_().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}

