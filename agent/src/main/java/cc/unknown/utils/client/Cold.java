/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.utils.client;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public class Cold {
    private long lastMs;
    private long time;
    private boolean checkedFinish;

    public Cold(long lasts) {
        this.lastMs = lasts;
    }

    public Cold() {
        this.lastMs = System.currentTimeMillis();
    }

    public void start() {
        this.reset();
        this.checkedFinish = false;
    }

    public boolean firstFinish() {
        return this.checkAndSetFinish(() -> System.currentTimeMillis() >= this.time + this.lastMs);
    }

    public void setCooldown(long time) {
        this.lastMs = time;
    }

    public boolean hasFinished() {
        return this.isElapsed(this.time + this.lastMs, System::currentTimeMillis);
    }

    public boolean finished(long delay) {
        return this.isElapsed(this.time, () -> System.currentTimeMillis() - delay);
    }

    public boolean isDelayComplete(long l) {
        return this.isElapsed(this.lastMs, () -> System.currentTimeMillis() - l);
    }

    public boolean reached(long currentTime) {
        return this.isElapsed(this.time, () -> Math.max(0L, System.currentTimeMillis() - currentTime));
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public long getTime() {
        return Math.max(0L, System.currentTimeMillis() - this.time);
    }

    public boolean getCum(long hentai) {
        return System.currentTimeMillis() - this.lastMs >= hentai;
    }

    public boolean hasTimeElapsed(long owo, boolean reset) {
        if (this.getTime() >= owo) {
            if (reset) {
                this.reset();
            }
            return true;
        }
        return false;
    }

    private boolean checkAndSetFinish(BooleanSupplier condition) {
        if (condition.getAsBoolean() && !this.checkedFinish) {
            this.checkedFinish = true;
            return true;
        }
        return false;
    }

    private boolean isElapsed(long targetTime, LongSupplier currentTimeSupplier) {
        return currentTimeSupplier.getAsLong() >= targetTime;
    }
}

