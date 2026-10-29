package com.otectus.immersivespellcraft.minigame;

/** SplitMix64: a tiny deterministic generator, identical on client and server for the same seed. */
public final class SimRandom {
    private long state;

    public SimRandom(long seed) {
        state = seed;
    }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform in [0, 1). */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public double range(double min, double max) {
        return min + (max - min) * nextDouble();
    }

    public boolean nextBoolean() {
        return (nextLong() & 1) == 0;
    }
}
