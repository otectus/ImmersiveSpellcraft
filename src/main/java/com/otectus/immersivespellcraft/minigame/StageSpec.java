package com.otectus.immersivespellcraft.minigame;

import java.util.Arrays;

/**
 * Immutable parameters of one stage. Positions and levels are normalized (alignment -1..1, levels and markers 0..1),
 * durations are milliseconds and speeds are units per second. The server generates these and sends them whole, so the
 * client simulates exactly what the server judges.
 */
public sealed interface StageSpec {
    StageKind kind();

    /** The measure this stage contributes to the workmanship score. */
    String measure();

    /** Line the instruction text is keyed by, relative to the game's translation prefix. */
    String label();

    record Align(String label, String measure, double start, double target, double tolerance, double speed) implements StageSpec {
        public Align {
            check(start >= -1 && start <= 1 && target >= -1 && target <= 1, "align positions");
            check(tolerance > 0 && tolerance <= 0.5, "align tolerance");
            check(speed > 0 && speed <= 4, "align speed");
        }
        @Override public StageKind kind() { return StageKind.ALIGN; }
    }

    record HoldBand(String label, String measure, double low, double high, double rise, double fall, int requiredMs,
                    double driftAmplitude, int driftPeriodMs) implements StageSpec {
        public HoldBand {
            check(low >= 0 && high <= 1 && high - low >= 0.04, "band");
            check(rise > 0 && rise <= 5 && fall > 0 && fall <= 5, "band rates");
            check(requiredMs >= 200 && requiredMs <= 30_000, "band duration");
            check(driftAmplitude >= 0 && driftAmplitude <= 0.25 && driftPeriodMs >= 500 && driftPeriodMs <= 20_000, "band drift");
        }
        @Override public StageKind kind() { return StageKind.HOLD_BAND; }
        public double center() { return (low + high) / 2; }
    }

    enum Trigger { PRESS, RELEASE }

    record TimedTrigger(String label, String measure, Trigger trigger, int periodMs, double center, double halfWidth) implements StageSpec {
        public TimedTrigger {
            check(trigger != null, "trigger");
            check(periodMs >= 400 && periodMs <= 10_000, "trigger period");
            check(center > 0 && center < 1 && halfWidth > 0 && halfWidth <= 0.35, "trigger window");
        }
        @Override public StageKind kind() { return StageKind.TIMED_TRIGGER; }
    }

    /**
     * Strokes are measured in inking time: holding draws the quill at constant speed, so a stroke of {@code lengthsMs[i]}
     * ends at its junction after that much ink. Overshooting the junction by more than the half-window bleeds: the
     * stroke's tail ({@code repairMs}) must be inked again, and only that segment.
     */
    record Strokes(String label, String measure, String inkMeasure, int[] lengthsMs, int junctionHalfMs, int repairMs) implements StageSpec {
        public Strokes {
            check(lengthsMs != null && lengthsMs.length >= 1 && lengthsMs.length <= 8, "stroke count");
            for (int l : lengthsMs) check(l >= 300 && l <= 5000, "stroke length");
            check(junctionHalfMs >= 60 && junctionHalfMs <= 600, "junction window");
            check(repairMs >= 100 && repairMs <= 2000, "repair length");
            lengthsMs = lengthsMs.clone();
        }
        @Override public StageKind kind() { return StageKind.STROKES; }
        @Override public int[] lengthsMs() { return lengthsMs.clone(); }
        public int count() { return lengthsMs.length; }
        public int length(int i) { return lengthsMs[i]; }

        @Override public boolean equals(Object o) {
            return o instanceof Strokes s && s.label.equals(label) && s.measure.equals(measure) && s.inkMeasure.equals(inkMeasure)
                    && Arrays.equals(s.lengthsMs, lengthsMs) && s.junctionHalfMs == junctionHalfMs && s.repairMs == repairMs;
        }
        @Override public int hashCode() { return Arrays.hashCode(lengthsMs) * 31 + junctionHalfMs; }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalArgumentException("invalid " + what);
    }
}
