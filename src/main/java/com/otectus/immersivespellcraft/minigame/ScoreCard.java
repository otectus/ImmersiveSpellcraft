package com.otectus.immersivespellcraft.minigame;

import java.util.Map;

/**
 * The workmanship of one performance: {@code Q = round(100 x weighted mean of the measures)} and its grade, the
 * normalized measures themselves, and how many corrections the player made. {@code complete} is the separate,
 * boolean essential-work requirement: a high average never stands in for unfinished stages.
 */
public record ScoreCard(boolean complete, int quality, Grade grade, Map<String, Double> measures, int corrections,
                        int elapsedMs, boolean assisted) {
    public ScoreCard {
        measures = Map.copyOf(measures);
    }
}
