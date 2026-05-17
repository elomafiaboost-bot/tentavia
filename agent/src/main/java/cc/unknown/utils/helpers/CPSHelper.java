/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.utils.helpers;

import java.util.concurrent.atomic.AtomicLongArray;

public class CPSHelper {
    private static final int MAX_CPS = 500;
    private static final AtomicLongArray[] TIMESTAMP_BUFFERS = new AtomicLongArray[MouseButton.values().length];

    public static void registerClick(MouseButton button) {
        int index = button.getIndex();
        int slot = (int)(System.currentTimeMillis() % 500L);
        TIMESTAMP_BUFFERS[index].set(slot, System.currentTimeMillis());
    }

    public static int getCPS(MouseButton button) {
        int index = button.getIndex();
        long currentTime = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < 500; ++i) {
            long timestamp = TIMESTAMP_BUFFERS[index].get(i);
            if (timestamp <= currentTime - 1000L) continue;
            ++count;
        }
        return count;
    }

    static {
        for (int i = 0; i < TIMESTAMP_BUFFERS.length; ++i) {
            CPSHelper.TIMESTAMP_BUFFERS[i] = new AtomicLongArray(500);
        }
    }

    public static enum MouseButton {
        LEFT(0),
        MIDDLE(1),
        RIGHT(2);

        private int index;

        private MouseButton(int index) {
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }
    }
}

