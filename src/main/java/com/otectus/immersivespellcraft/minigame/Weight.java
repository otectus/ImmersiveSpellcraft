package com.otectus.immersivespellcraft.minigame;

/** A documented, nonnegative weight of one measure in the workmanship score. A game's weights total 1. */
public record Weight(String measure, double weight) {
    public Weight {
        if (measure == null || measure.isEmpty()) throw new IllegalArgumentException("measure");
        if (!(weight >= 0 && weight <= 1)) throw new IllegalArgumentException("weight " + weight);
    }
}
