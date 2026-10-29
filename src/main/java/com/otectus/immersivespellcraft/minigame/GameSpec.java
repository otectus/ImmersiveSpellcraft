package com.otectus.immersivespellcraft.minigame;

import java.util.List;

/**
 * One generated performance: the game type, the generator version that produced it, its stages and score weights.
 * {@code maxDurationMs} 0 means untimed (no expiry); {@code assist} is the window widening the server granted.
 */
public record GameSpec(String type, int generatorVersion, long seed, int countInMs, int maxDurationMs, boolean untimed,
                       double assist, List<StageSpec> stages, List<Weight> weights) {
    public static final int MAX_STAGES = 8;
    public static final int MAX_WEIGHTS = 8;

    public GameSpec {
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("type");
        if (countInMs < 0 || countInMs > 5000) throw new IllegalArgumentException("count-in");
        if (maxDurationMs < 0 || maxDurationMs > 120_000) throw new IllegalArgumentException("max duration");
        if (!(assist >= 1 && assist <= 3)) throw new IllegalArgumentException("assist");
        if (stages == null || stages.isEmpty() || stages.size() > MAX_STAGES) throw new IllegalArgumentException("stages");
        if (weights == null || weights.isEmpty() || weights.size() > MAX_WEIGHTS) throw new IllegalArgumentException("weights");
        double total = 0;
        for (Weight w : weights) total += w.weight();
        if (Math.abs(total - 1) > 1e-6) throw new IllegalArgumentException("weights must total 1, not " + total);
        stages = List.copyOf(stages);
        weights = List.copyOf(weights);
    }

    public int stageCount() {
        return stages.size();
    }

    public StageSpec stage(int index) {
        return stages.get(index);
    }
}
