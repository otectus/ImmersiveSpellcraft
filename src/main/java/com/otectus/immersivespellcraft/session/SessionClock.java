package com.otectus.immersivespellcraft.session;

import net.minecraft.Util;

import java.util.function.LongSupplier;

/**
 * Milliseconds for session timing: real time, because the client's performance clock is real time, minus any time
 * the integrated server spent paused. Automated tests switch to game time, since a GameTest server runs ticks faster
 * than real time.
 */
public final class SessionClock {
    private static volatile LongSupplier source = Util::getMillis;
    private static long pausedTotal;
    private static long pausedSince = -1;

    public static synchronized long now() {
        long raw = source.getAsLong();
        return raw - pausedTotal - (pausedSince >= 0 ? raw - pausedSince : 0);
    }

    /** From the integrated server's own pause flag; a dedicated server never pauses. */
    public static synchronized void paused(boolean paused) {
        long raw = source.getAsLong();
        if (paused && pausedSince < 0) pausedSince = raw;
        else if (!paused && pausedSince >= 0) {
            pausedTotal += raw - pausedSince;
            pausedSince = -1;
        }
    }

    /** Automated tests only. {@code null} restores real time. */
    public static synchronized void useSource(LongSupplier clock) {
        source = clock == null ? Util::getMillis : clock;
        pausedTotal = 0;
        pausedSince = -1;
    }

    private SessionClock() {}
}
