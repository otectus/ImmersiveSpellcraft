package com.otectus.immersivespellcraft.minigame;

/** Family-style descriptive workmanship grades. They describe the physical work only; the native output never changes. */
public enum Grade {
    CRUDE(0, 34), STANDARD(35, 69), FINE(70, 89), MASTERWORK(90, 100);

    public final int min, max;

    Grade(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static Grade of(int quality) {
        int q = Math.max(0, Math.min(100, quality));
        for (Grade g : values()) if (q >= g.min && q <= g.max) return g;
        return CRUDE;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
