/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.network.NetworkPlayerInfo
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 */
package cc.unknown.utils.player;

import cc.unknown.utils.Loona;
import java.util.ArrayList;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public enum FriendUtil implements Loona
{
    instance;

    public ArrayList<Entity> friends = new ArrayList();

    public void addFriend(Entity en) {
        this.friends.add(en);
    }

    public boolean removeFriend(Entity en) {
        return this.friends.remove(en);
    }

    public ArrayList<Entity> getFriends() {
        return this.friends;
    }

    public boolean addFriend(String name) {
        boolean found = false;
        for (Entity entity : FriendUtil.mc.field_71441_e.func_72910_y()) {
            if (!entity.func_70005_c_().equalsIgnoreCase(name) && !entity.func_95999_t().equalsIgnoreCase(name) || this.isAFriend(entity)) continue;
            this.addFriend(entity);
            found = true;
        }
        return found;
    }

    public boolean removeFriend(String name) {
        boolean removed = false;
        boolean found = false;
        for (NetworkPlayerInfo networkPlayerInfo : new ArrayList<NetworkPlayerInfo>(mc.func_147114_u().func_175106_d())) {
            String playerName;
            EntityPlayer entity;
            if (networkPlayerInfo.func_178854_k() == null || (entity = FriendUtil.mc.field_71441_e.func_72924_a(playerName = networkPlayerInfo.func_178854_k().func_150260_c())) == null || !entity.func_70005_c_().equalsIgnoreCase(name) && !entity.func_95999_t().equalsIgnoreCase(name)) continue;
            removed = this.removeFriend((Entity)entity);
            found = true;
        }
        return found && removed;
    }

    public boolean isAFriend(Entity entity) {
        if (entity == FriendUtil.mc.field_71439_g) {
            return true;
        }
        for (Entity en : this.friends) {
            if (!en.equals((Object)entity)) continue;
            return true;
        }
        EntityPlayer e = (EntityPlayer)entity;
        return FriendUtil.mc.field_71439_g.func_142014_c((EntityLivingBase)((EntityPlayer)entity)) || FriendUtil.mc.field_71439_g.func_145748_c_().func_150260_c().startsWith(e.func_145748_c_().func_150260_c().substring(0, 2));
    }
}

