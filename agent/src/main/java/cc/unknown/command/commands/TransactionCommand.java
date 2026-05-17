/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.play.server.S32PacketConfirmTransaction
 */
package cc.unknown.command.commands;

import cc.unknown.Haru;
import cc.unknown.command.Command;
import cc.unknown.command.Flips;
import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.network.PacketEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

@Flips(name="Transaction", alias="s32", desc="Show server transaction IDs", syntax=".transaction")
public class TransactionCommand
extends Command {
    private AtomicBoolean toggle = new AtomicBoolean(false);

    public TransactionCommand() {
        Haru.instance.getEventBus().register(this);
    }

    @Override
    public void onExecute(String[] args) {
        this.toggle.set(!this.toggle.get());
    }

    @EventLink
    public void onPacket(PacketEvent e) {
        if (!this.toggle.get()) {
            return;
        }
        if (e.isReceive() && e.getPacket() instanceof S32PacketConfirmTransaction) {
            this.sendChat(this.getColor("Red") + " [Transaction ID]: " + this.getColor("White") + ((S32PacketConfirmTransaction)e.getPacket()).func_148890_d(), new Object[0]);
        }
    }
}

