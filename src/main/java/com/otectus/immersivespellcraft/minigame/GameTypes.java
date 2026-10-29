package com.otectus.immersivespellcraft.minigame;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The game generators. Each game gives its station a different physical action built from the shared stage kinds:
 *
 * <ul>
 *   <li><b>Sigil Inscription</b> (Scribe's Table): a quill inks 3-6 strokes of a glyph contour; hold to ink, lift at
 *       each junction. Weights: junction timing 0.6, ink control 0.4.</li>
 *   <li><b>Scroll Forging</b> (Scroll Forge): turn the school focus into alignment, feed ink into the sigil inside the
 *       marked flow band, press the seal on its mark. Weights: alignment 0.3, flow 0.4, seal 0.3.</li>
 *   <li><b>Glyph Embossing</b> (Glyph Press): align the die to the plate, hold the pressure band while it engraves,
 *       release on the seal mark. Fits the Press's own processing window. Weights: alignment 0.3, pressure 0.45,
 *       seal 0.25.</li>
 * </ul>
 *
 * Bump {@link #GENERATOR_VERSION} whenever a generator's output changes; clients refuse a spec from another version.
 */
public final class GameTypes {
    public static final int GENERATOR_VERSION = 1;
    public static final String NAMESPACE = "immersive_spellcraft";
    public static final String SIGIL_INSCRIPTION = NAMESPACE + ":sigil_inscription";
    public static final String SCROLL_FORGING = NAMESPACE + ":scroll_forging";
    public static final String GLYPH_EMBOSSING = NAMESPACE + ":glyph_embossing";
    public static final Set<String> ALL = Set.of(SIGIL_INSCRIPTION, SCROLL_FORGING, GLYPH_EMBOSSING);
    public static final int COUNT_IN_MS = 1200;

    public static boolean known(String type) {
        return ALL.contains(type);
    }

    public static GameSpec create(String type, GameParams p) {
        return switch (type) {
            case SIGIL_INSCRIPTION -> sigil(p);
            case SCROLL_FORGING -> forging(p);
            case GLYPH_EMBOSSING -> embossing(p);
            default -> throw new IllegalArgumentException("unknown game " + type);
        };
    }

    /** The translation key prefix for a game's texts, e.g. {@code immersive_spellcraft.game.sigil_inscription}. */
    public static String keyPrefix(String type) {
        int colon = type.indexOf(':');
        return NAMESPACE + ".game." + (colon >= 0 ? type.substring(colon + 1) : type);
    }

    private static GameSpec sigil(GameParams p) {
        SimRandom r = new SimRandom(p.seed());
        double c = p.complexity(), w = p.widening();
        int n = Math.max(p.minActions(), Math.min(p.maxActions(),
                p.minActions() + (int) Math.round((p.maxActions() - p.minActions()) * c)));
        int budget = (int) (p.targetDurationMs() * 0.85) - n * 350;
        int mean = Math.max(600, budget / n);
        int[] lengths = new int[n];
        for (int i = 0; i < n; i++) lengths[i] = clamp((int) (mean * r.range(0.8, 1.2)), 500, 2400);
        int junction = clamp((int) Math.round((230 - 60 * c) * w), 90, 450);
        int repair = clamp(Math.min(500, mean / 2), 150, 900);
        List<StageSpec> stages = List.of(new StageSpec.Strokes("strokes", "junctions", "ink", lengths, junction, repair));
        return spec(SIGIL_INSCRIPTION, p, stages, List.of(new Weight("junctions", 0.6), new Weight("ink", 0.4)));
    }

    private static GameSpec forging(GameParams p) {
        SimRandom r = new SimRandom(p.seed());
        double c = p.complexity(), w = p.widening();
        List<StageSpec> stages = new ArrayList<>();
        double start = (r.nextBoolean() ? 1 : -1) * r.range(0.45, 0.9);
        stages.add(new StageSpec.Align("focus", "alignment", start, 0, clamp((0.12 - 0.03 * c) * w, 0.05, 0.35), 0.8));
        double center = 0.55 + (r.nextDouble() - 0.5) * 0.2;
        double half = clamp((0.24 - 0.07 * c) * w, 0.1, 0.6) / 2;
        int required = clamp((int) (p.targetDurationMs() * 0.38), 1500, 6000);
        stages.add(new StageSpec.HoldBand("ink", "flow", Math.max(0.05, center - half), Math.min(0.97, center + half),
                0.9, 0.7, required, 0.04 + 0.06 * c, 2800));
        stages.add(new StageSpec.TimedTrigger("seal", "seal", StageSpec.Trigger.PRESS, (int) (1700 - 400 * c),
                0.3 + r.nextDouble() * 0.4, clamp((0.10 - 0.02 * c) * w, 0.04, 0.3)));
        return spec(SCROLL_FORGING, p, stages,
                List.of(new Weight("alignment", 0.3), new Weight("flow", 0.4), new Weight("seal", 0.3)));
    }

    private static GameSpec embossing(GameParams p) {
        SimRandom r = new SimRandom(p.seed());
        double c = p.complexity(), w = p.widening();
        int window = p.nativeWindowMs() > 0 ? p.nativeWindowMs() : p.targetDurationMs();
        List<StageSpec> stages = new ArrayList<>();
        double start = (r.nextBoolean() ? 1 : -1) * r.range(0.4, 0.85);
        stages.add(new StageSpec.Align("die", "alignment", start, 0, clamp((0.11 - 0.03 * c) * w, 0.05, 0.35), 0.9));
        double center = 0.62 + (r.nextDouble() - 0.5) * 0.16;
        double half = clamp((0.22 - 0.06 * c) * w, 0.1, 0.6) / 2;
        int required = clamp(window - 2600, 1200, 12_000);
        stages.add(new StageSpec.HoldBand("pressure", "pressure", Math.max(0.05, center - half), Math.min(0.97, center + half),
                1.0, 0.8, required, 0.05 + 0.07 * c, 2400));
        stages.add(new StageSpec.TimedTrigger("seal", "seal", StageSpec.Trigger.RELEASE, (int) (1500 - 300 * c),
                0.5 + (r.nextDouble() - 0.5) * 0.3, clamp((0.10 - 0.02 * c) * w, 0.04, 0.3)));
        return spec(GLYPH_EMBOSSING, p, stages,
                List.of(new Weight("alignment", 0.3), new Weight("pressure", 0.45), new Weight("seal", 0.25)));
    }

    private static GameSpec spec(String type, GameParams p, List<StageSpec> stages, List<Weight> weights) {
        int max = p.untimed() ? 0 : p.maxDurationMs();
        return new GameSpec(type, GENERATOR_VERSION, p.seed(), COUNT_IN_MS, max, p.untimed(),
                Math.min(3, Math.max(1, p.assist())), stages, weights);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private GameTypes() {}
}
