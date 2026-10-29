package com.otectus.immersivespellcraft.minigame;

/** The physical actions the games are built from. Each game combines a few of them into its own sequence. */
public enum StageKind {
    /** Turn or slide a part until its notch meets the mark, then lock it with the primary action. */
    ALIGN,
    /** Hold to raise a flow or pressure, release to let it fall, and keep it inside a marked band until the work fills. */
    HOLD_BAND,
    /** A marker sweeps a track; press (or release a hold) while it is inside the marked window. */
    TIMED_TRIGGER,
    /** Hold to draw ink along each stroke and lift at the glowing junction that ends it. */
    STROKES;

    private static final StageKind[] VALUES = values();

    public static StageKind byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }
}
