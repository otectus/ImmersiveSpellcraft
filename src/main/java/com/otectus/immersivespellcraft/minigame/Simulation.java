package com.otectus.immersivespellcraft.minigame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic fixed-step simulation of one performance. The server runs it from the inputs it accepted; the client
 * runs the same code from the same inputs to predict what it draws. Time advances in {@link #STEP_MS} steps and only
 * the inputs move it forward: there is no hidden randomness after generation and no wall clock.
 *
 * <p>Nothing here knows about Minecraft. The adapters decide what completing the work means.
 */
public final class Simulation {
    public static final int STEP_MS = 10;
    private static final int CUE_LIMIT = 64;

    private final GameSpec spec;
    private final boolean[] held = new boolean[Control.values().length];
    private final Stage[] stages;
    private final List<Cue> cues = new ArrayList<>();
    private int now;
    private int stage;
    private int stageStartedAt;
    private int pointer;
    private boolean pointerActive;
    private boolean complete;
    private boolean expired;
    private int completedAt = -1;
    private int cueSerial;

    public Simulation(GameSpec spec) {
        this.spec = spec;
        stages = new Stage[spec.stageCount()];
        for (int i = 0; i < stages.length; i++) stages[i] = create(spec.stage(i));
    }

    // ---- Driving ---------------------------------------------------------------------------------------------------

    /** Advances to {@code timeMs} (no-op when already there). Returns false when the time lies in the past. */
    public boolean advanceTo(int timeMs) {
        if (timeMs < now) return false;
        while (now + STEP_MS <= timeMs && !finished()) {
            step();
            now += STEP_MS;
            checkExpiry();
        }
        return true;
    }

    /** Applies one control change at its own time. Returns false when it is in the past or the run is finished. */
    public boolean apply(InputEvent event) {
        if (event.timeMs() < now || finished()) return false;
        advanceTo(event.timeMs());
        if (finished()) return false;
        Control c = event.control();
        if (c == Control.POINTER) {
            pointer = event.value();
            pointerActive = true;
            return true;
        }
        boolean down = event.value() == 1;
        boolean was = held[c.ordinal()];
        held[c.ordinal()] = down;
        if (c == Control.PRIMARY && was != down) {
            if (down) stages[stage].press();
            else stages[stage].release();
            advanceIfDone();
        }
        return true;
    }

    private void step() {
        stages[stage].step(STEP_MS);
        advanceIfDone();
    }

    private void checkExpiry() {
        if (!complete && !expired && !spec.untimed() && spec.maxDurationMs() > 0 && now >= spec.maxDurationMs()) {
            expired = true;
            cue(Cue.Kind.EXPIRED);
        }
    }

    private void advanceIfDone() {
        while (!complete && stages[stage].done()) {
            cue(Cue.Kind.STAGE_COMPLETE);
            if (stage + 1 >= stages.length) {
                complete = true;
                completedAt = now;
                cue(Cue.Kind.COMPLETE);
                return;
            }
            stage++;
            stageStartedAt = now;
            stages[stage].begin();
        }
    }

    // ---- Reading ---------------------------------------------------------------------------------------------------

    public GameSpec spec() { return spec; }
    public int now() { return now; }
    public int stageIndex() { return stage; }
    public int stageElapsed() { return now - stageStartedAt; }
    public boolean complete() { return complete; }
    public boolean expired() { return expired; }
    public boolean finished() { return complete || expired; }
    public int completedAt() { return completedAt; }
    public boolean held(Control control) { return held[control.ordinal()]; }
    public StageSpec currentSpec() { return spec.stage(stage); }

    /** Read-only view of a stage for presentation. */
    public StageView view(int index) { return stages[index]; }
    public StageView current() { return stages[stage]; }

    /** Feedback cues with a serial above {@code after}; the serial of a cue is its position in the full sequence. */
    public List<Cue> cuesAfter(int after) {
        int first = cueSerial - cues.size();
        int from = Math.max(0, after - first);
        return from >= cues.size() ? List.of() : List.copyOf(cues.subList(from, cues.size()));
    }

    public int cueSerial() { return cueSerial; }

    public int corrections() {
        int total = 0;
        for (Stage s : stages) total += s.corrections();
        return total;
    }

    public ScoreCard score() {
        Map<String, double[]> sums = new LinkedHashMap<>();
        for (Stage s : stages) s.measures((id, v) -> {
            double[] a = sums.computeIfAbsent(id, k -> new double[2]);
            a[0] += v;
            a[1]++;
        });
        Map<String, Double> measures = new LinkedHashMap<>();
        sums.forEach((id, a) -> measures.put(id, clamp01(a[0] / a[1])));
        double total = 0, weightSum = 0;
        for (Weight w : spec.weights()) {
            Double m = measures.get(w.measure());
            if (m == null) continue;
            total += w.weight() * m;
            weightSum += w.weight();
        }
        int quality = weightSum <= 0 ? 0 : (int) Math.round(100 * total / weightSum);
        boolean assisted = spec.untimed() || spec.assist() > 1.0001;
        return new ScoreCard(complete, quality, Grade.of(quality), measures, corrections(),
                complete ? completedAt : now, assisted);
    }

    private void cue(Cue.Kind kind) {
        cues.add(new Cue(now, stage, kind));
        cueSerial++;
        if (cues.size() > CUE_LIMIT) cues.remove(0);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    private Stage create(StageSpec s) {
        return switch (s.kind()) {
            case ALIGN -> new AlignStage((StageSpec.Align) s);
            case HOLD_BAND -> new HoldBandStage((StageSpec.HoldBand) s);
            case TIMED_TRIGGER -> new TriggerStage((StageSpec.TimedTrigger) s);
            case STROKES -> new StrokesStage((StageSpec.Strokes) s);
        };
    }

    // ---- Stages ----------------------------------------------------------------------------------------------------

    /** What the presentation may read about a stage. All values are normalized like the spec. */
    public interface StageView {
        StageSpec spec();
        boolean done();
        int corrections();
        /** 0..1 progress through the stage's essential work. */
        double progress();
        /** Alignment position, flow/pressure level, marker position or inked fraction of the current stroke. */
        double value();
        /** The currently valid band or window, after drift: {low, high}. */
        double[] window();
        /** Strokes: index of the stroke being inked; other stages 0. */
        default int index() { return 0; }
        /** Strokes: whether the current stroke's tail is bleeding and waiting for repair. */
        default boolean needsRepair() { return false; }
        /** Strokes: the quill must be lifted (primary released) before inking can continue. */
        default boolean waitingForLift() { return false; }
    }

    private interface MeasureSink { void accept(String id, double value); }

    private abstract class Stage implements StageView {
        boolean done;
        int corrections;
        void begin() {}
        void press() {}
        void release() {}
        abstract void step(int dt);
        abstract void measures(MeasureSink sink);
        @Override public boolean done() { return done; }
        @Override public int corrections() { return corrections; }
    }

    private final class AlignStage extends Stage {
        final StageSpec.Align s;
        double pos, accuracy;

        AlignStage(StageSpec.Align s) { this.s = s; pos = s.start(); }

        @Override void step(int dt) {
            if (done) return;
            int dir = (held[Control.RIGHT.ordinal()] ? 1 : 0) - (held[Control.LEFT.ordinal()] ? 1 : 0);
            double move = s.speed() * dt / 1000.0;
            if (dir != 0) pos += dir * move;
            else if (pointerActive) {
                double target = pointer / (double) InputEvent.POINTER_RANGE;
                double d = target - pos;
                double max = move * 1.5;
                pos += Math.abs(d) <= max ? d : Math.signum(d) * max;
            }
            pos = Math.max(-1, Math.min(1, pos));
        }

        @Override void press() {
            if (done) return;
            double off = Math.abs(pos - s.target());
            if (off <= s.tolerance()) {
                accuracy = 1 - 0.5 * off / s.tolerance();
                done = true;
                cue(Cue.Kind.ALIGNED);
            } else {
                corrections++;
                cue(Cue.Kind.MISALIGNED);
            }
        }

        @Override void measures(MeasureSink sink) { sink.accept(s.measure(), done ? accuracy - 0.1 * corrections : 0); }
        @Override public StageSpec spec() { return s; }
        @Override public double progress() { return done ? 1 : Math.max(0, 1 - Math.abs(pos - s.target()) / 2); }
        @Override public double value() { return pos; }
        @Override public double[] window() { return new double[]{s.target() - s.tolerance(), s.target() + s.tolerance()}; }
    }

    private final class HoldBandStage extends Stage {
        final StageSpec.HoldBand s;
        double level, progress;
        int inBand, outBand, overEpisodes;
        boolean entered;
        int zone; // -1 below, 0 inside, 1 above

        HoldBandStage(StageSpec.HoldBand s) { this.s = s; }

        double drift() {
            if (s.driftAmplitude() <= 0) return 0;
            return s.driftAmplitude() * StrictMath.sin(2 * Math.PI * stageElapsed() / s.driftPeriodMs());
        }

        @Override void step(int dt) {
            if (done) return;
            double d = drift();
            double lo = s.low() + d, hi = s.high() + d;
            boolean pressing = held[Control.PRIMARY.ordinal()];
            if (spec.untimed()) {
                if (pressing) level = (lo + hi) / 2;
            } else {
                level += (pressing ? s.rise() : -s.fall()) * dt / 1000.0;
                level = Math.max(0, Math.min(1, level));
            }
            int z = level < lo ? -1 : level > hi ? 1 : 0;
            if (z == 0 && (!spec.untimed() || pressing)) {
                progress += dt / (double) s.requiredMs();
                inBand += dt;
                entered = true;
            } else if (entered && !spec.untimed()) {
                outBand += dt;
            }
            if (z != zone) {
                if (z == 0) cue(Cue.Kind.BAND_ENTERED);
                else if (entered) {
                    // Too much pressure is the correction that counts; dipping low only costs control time.
                    if (z > 0) overEpisodes++;
                    cue(z > 0 ? Cue.Kind.BAND_HIGH : Cue.Kind.BAND_LOW);
                }
                zone = z;
            }
            if (progress >= 1) {
                progress = 1;
                done = true;
            }
        }

        @Override void measures(MeasureSink sink) {
            int total = inBand + outBand;
            sink.accept(s.measure(), !done ? 0 : total == 0 ? 1 : inBand / (double) total);
        }

        @Override public int corrections() { return overEpisodes; }
        @Override public StageSpec spec() { return s; }
        @Override public double progress() { return progress; }
        @Override public double value() { return level; }
        @Override public double[] window() { double d = drift(); return new double[]{s.low() + d, s.high() + d}; }
    }

    private final class TriggerStage extends Stage {
        final StageSpec.TimedTrigger s;
        double accuracy;
        boolean armed;

        TriggerStage(StageSpec.TimedTrigger s) { this.s = s; }

        @Override void begin() {
            // A release trigger that starts with the hold already in progress is armed at once.
            if (s.trigger() == StageSpec.Trigger.RELEASE && held[Control.PRIMARY.ordinal()]) armed = true;
        }

        double marker() {
            int t = stageElapsed();
            if (spec.untimed()) return Math.min(s.center(), 2.0 * t / s.periodMs());
            double phase = (t % s.periodMs()) / (double) s.periodMs();
            return phase < 0.5 ? phase * 2 : 2 - phase * 2;
        }

        void judge() {
            double off = Math.abs(marker() - s.center());
            if (off <= s.halfWidth()) {
                accuracy = 1 - off / s.halfWidth();
                done = true;
                cue(Cue.Kind.TRIGGER_HIT);
            } else {
                corrections++;
                cue(Cue.Kind.TRIGGER_MISSED);
            }
        }

        @Override void press() {
            if (done) return;
            if (s.trigger() == StageSpec.Trigger.PRESS) judge();
            else armed = true;
        }

        @Override void release() {
            if (done || s.trigger() != StageSpec.Trigger.RELEASE || !armed) return;
            armed = false;
            judge();
        }

        @Override void step(int dt) {}
        @Override void measures(MeasureSink sink) { sink.accept(s.measure(), done ? Math.max(0.3, accuracy) - 0.15 * corrections : 0); }
        @Override public StageSpec spec() { return s; }
        @Override public double progress() { return done ? 1 : 0; }
        @Override public double value() { return marker(); }
        @Override public double[] window() { return new double[]{s.center() - s.halfWidth(), s.center() + s.halfWidth()}; }
    }

    private final class StrokesStage extends Stage {
        final StageSpec.Strokes s;
        final double[] accuracy;
        int index, inked, bleeds;
        boolean awaitRelease, repairing;

        StrokesStage(StageSpec.Strokes s) {
            this.s = s;
            accuracy = new double[s.count()];
        }

        @Override void begin() {
            // A hold carried over from the previous stage must be lifted before the first stroke starts.
            awaitRelease = held[Control.PRIMARY.ordinal()];
        }

        @Override void step(int dt) {
            if (done || awaitRelease || !held[Control.PRIMARY.ordinal()]) return;
            int length = s.length(index);
            inked += dt;
            if (spec.untimed()) {
                inked = Math.min(inked, length);
                return;
            }
            if (inked > length + s.junctionHalfMs()) {
                // Bleed: the quill ran through the junction. Only this stroke's tail needs inking again.
                bleeds++;
                inked = Math.max(0, length - s.repairMs());
                repairing = true;
                awaitRelease = true;
                cue(Cue.Kind.STROKE_BLEED);
            }
        }

        @Override void release() {
            if (done) return;
            if (awaitRelease) {
                awaitRelease = false;
                return;
            }
            int length = s.length(index);
            int off = Math.abs(inked - length);
            if (off <= s.junctionHalfMs() || (spec.untimed() && inked >= length)) {
                accuracy[index] = 1 - off / (double) s.junctionHalfMs();
                index++;
                inked = 0;
                repairing = false;
                cue(Cue.Kind.STROKE_DONE);
                if (index >= s.count()) {
                    index = s.count() - 1;
                    inked = s.length(index);
                    done = true;
                }
            } else if (inked > 0) {
                cue(Cue.Kind.STROKE_LIFTED);
            }
        }

        @Override void measures(MeasureSink sink) {
            double sum = 0;
            for (double a : accuracy) sum += a;
            sink.accept(s.measure(), done ? sum / s.count() : 0);
            sink.accept(s.inkMeasure(), done ? 1 - bleeds / (double) s.count() : 0);
        }

        @Override public int corrections() { return bleeds; }
        @Override public StageSpec spec() { return s; }
        @Override public double progress() {
            if (done) return 1;
            double total = 0, sofar = 0;
            for (int i = 0; i < s.count(); i++) {
                total += s.length(i);
                if (i < index) sofar += s.length(i);
            }
            return (sofar + Math.min(inked, s.length(index))) / total;
        }
        @Override public double value() { return Math.min(1.5, inked / (double) s.length(index)); }
        @Override public double[] window() {
            double l = s.length(index);
            return new double[]{(l - s.junctionHalfMs()) / l, (l + s.junctionHalfMs()) / l};
        }
        @Override public int index() { return index; }
        @Override public boolean needsRepair() { return repairing; }
        @Override public boolean waitingForLift() { return awaitRelease; }
    }
}
