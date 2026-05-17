/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.client.gui.GuiChat
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
 *  net.minecraft.network.play.client.C0EPacketClickWindow
 *  net.minecraft.network.play.server.S08PacketPlayerPosLook
 *  net.minecraft.network.play.server.S2DPacketOpenWindow
 *  net.minecraft.network.play.server.S2EPacketCloseWindow
 */
package cc.unknown.command.commands;

import cc.unknown.Haru;
import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.utils.network.PacketUtil;
import cc.unknown.utils.player.PlayerUtil;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;

@Flips(name="Game", alias="join", desc="It automatically enters the selected minigame.", syntax=".game <mini game> <lobby>")
public class GameCommand
extends Command {
    private HashMap<String, Item> hashMap = new HashMap();
    private boolean joining;
    private Item item;
    private int number;
    protected int delay;
    private int stage;
    private boolean foundItem;
    protected boolean foundGame;
    protected boolean foundLobby;

    public GameCommand() {
        this.init();
        Haru.instance.getEventBus().register(this);
    }

    @Override
    public void onExecute(String[] args) {
        AtomicReference<String> message = new AtomicReference<String>("");
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            this.clearChat();
            message.set(this.getList());
        } else {
            if (args.length < 2 || args.length == 0) {
                message.set(this.getColor("Red") + " Syntax Error. Use: " + this.syntax);
                return;
            }
            String gameName = args[0];
            if (!this.hashMap.containsKey(gameName)) {
                message.set(this.getColor("Red") + " Invalid game. Use: .game list");
                return;
            }
            if (!args[1].matches("\\d+")) {
                message.set(this.getColor("Red") + " Invalid number.");
                return;
            }
            int lobby = Integer.parseInt(args[1]);
            if (lobby == 0) {
                message.set(this.getColor("Red") + " Invalid lobby.");
                return;
            }
            this.startJoining(this.hashMap.get(gameName), lobby);
            message.set(this.getColor("Yellow") + " Have a coffee while I try to get you into the mini-game.");
        }
        this.sendChat(message.get(), new Object[0]);
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        if (e.isReceive() && PlayerUtil.inGame()) {
            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                this.joining = false;
            }
            if (this.stage == 2 && e.getPacket() instanceof S2DPacketOpenWindow) {
                this.stage = 3;
            }
            if (this.stage >= 3 && e.getPacket() instanceof S2EPacketCloseWindow) {
                this.stage = 0;
            }
        }
    }

    @EventLink
    public void onTick(TickEvent e) {
        if (PlayerUtil.inGame()) {
            if (GameCommand.mc.field_71462_r instanceof GuiChat || PlayerUtil.isMoving()) {
                this.joining = false;
                return;
            }
            if (!this.joining) {
                return;
            }
            EntityPlayerSP player = GameCommand.mc.field_71439_g;
            block0 : switch (this.stage) {
                case 0: {
                    if (this.foundItem || !player.field_71069_bz.func_75139_a(36).func_75216_d()) break;
                    PacketUtil.send(new C08PacketPlayerBlockPlacement(player.func_70694_bm()));
                    ++this.stage;
                    break;
                }
                case 1: {
                    if (!(GameCommand.mc.field_71462_r instanceof GuiContainer)) break;
                    GuiContainer container = (GuiContainer)GameCommand.mc.field_71462_r;
                    List inventory = container.field_147002_h.func_75138_a();
                    for (int i = 0; i < inventory.size(); ++i) {
                        ItemStack slot = (ItemStack)inventory.get(i);
                        if (slot == null || slot.func_77973_b() != this.item) continue;
                        PacketUtil.send(new C0EPacketClickWindow(container.field_147002_h.field_75152_c, i, 0, 0, slot, 1));
                        ++this.stage;
                        break block0;
                    }
                    break;
                }
                case 3: {
                    if (!(GameCommand.mc.field_71462_r instanceof GuiContainer)) break;
                    GuiContainer container = (GuiContainer)GameCommand.mc.field_71462_r;
                    List inventory = container.field_147002_h.func_75138_a();
                    for (int i = 0; i < inventory.size(); ++i) {
                        ItemStack slot = (ItemStack)inventory.get(i);
                        if (slot == null || slot.field_77994_a != this.number) continue;
                        PacketUtil.send(new C0EPacketClickWindow(container.field_147002_h.field_75152_c, i, 0, 0, slot, 1));
                        ++this.stage;
                        break block0;
                    }
                    break;
                }
                case 4: {
                    if (player.field_70173_aa % 11 != 0) break;
                    this.stage = 3;
                }
            }
        }
    }

    private void init() {
        this.hashMap.put("sw", (Item)Items.field_151031_f);
        this.hashMap.put("tsw", Items.field_151032_g);
        this.hashMap.put("bw", Items.field_151104_aV);
        this.hashMap.put("tnt", Items.field_151016_H);
        this.hashMap.put("pgames", Items.field_151105_aU);
        this.hashMap.put("arena", Items.field_151048_u);
    }

    private String getList() {
        return "\n" + this.getColor("Green") + " - " + this.getColor("White") + "sw" + this.getColor("Gray") + " (Skywars)        \n" + this.getColor("Green") + " - " + this.getColor("White") + "tsw" + this.getColor("Gray") + " (Team Skywars)  \n" + this.getColor("Green") + " - " + this.getColor("White") + "tnt" + this.getColor("Gray") + " (Tnt Tag)       \n" + this.getColor("Green") + " - " + this.getColor("White") + "bw" + this.getColor("Gray") + " (Bedwars)        \n" + this.getColor("Green") + " - " + this.getColor("White") + "pgames" + this.getColor("Gray") + " (Party Games)\n" + this.getColor("Green") + " - " + this.getColor("White") + "arena" + this.getColor("Gray") + " (Arenapvp)    \n";
    }

    private void startJoining(Item name, int lobby) {
        this.joining = true;
        this.item = name;
        this.number = lobby;
        this.delay = 0;
        this.stage = 0;
        this.foundLobby = false;
        this.foundGame = false;
        this.foundItem = false;
    }
}

