/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.event.impl.network;

import cc.unknown.event.Event;

public class ChatSendEvent
extends Event {
    private String message;

    public ChatSendEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

