package com.otectus.immersivespellcraft.minigame;

/**
 * Inputs to a game generator, all decided by the server: the operation's native complexity (0..1), the profile's
 * duration and action bounds, the seed, and the assistance the server allows this player. {@code nativeWindowMs} is
 * the length of an existing native processing window the game should fit inside, or 0.
 */
public record GameParams(double complexity, int targetDurationMs, int maxDurationMs, int minActions, int maxActions,
                         long seed, double assist, boolean untimed, double windowMultiplier, int nativeWindowMs) {
    public GameParams {
        if (!(complexity >= 0 && complexity <= 1)) throw new IllegalArgumentException("complexity");
        if (targetDurationMs < 1000 || targetDurationMs > 60_000) throw new IllegalArgumentException("target duration");
        if (maxDurationMs < targetDurationMs || maxDurationMs > 120_000) throw new IllegalArgumentException("max duration");
        if (minActions < 1 || maxActions < minActions || maxActions > 8) throw new IllegalArgumentException("actions");
        if (!(assist >= 1 && assist <= 3)) throw new IllegalArgumentException("assist");
        if (!(windowMultiplier >= 0.25 && windowMultiplier <= 4)) throw new IllegalArgumentException("window multiplier");
        if (nativeWindowMs < 0 || nativeWindowMs > 120_000) throw new IllegalArgumentException("native window");
    }

    /** The combined widening of windows, bands and tolerances. */
    public double widening() {
        return Math.max(0.5, Math.min(3.0, assist * windowMultiplier));
    }
}
