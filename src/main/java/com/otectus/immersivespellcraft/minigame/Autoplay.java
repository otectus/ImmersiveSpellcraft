package com.otectus.immersivespellcraft.minigame;

import java.util.ArrayList;
import java.util.List;

/**
 * A player model that drives a simulation by watching it, used for the in-game demonstration, the automated tests and
 * the client smoke agent. {@link Style#CLEAN} aims for the middle of every window; {@link Style#SLOPPY} overshoots and
 * misses on purpose but still finishes, which exercises every correction path.
 */
public final class Autoplay {
    public enum Style { CLEAN, SLOPPY }

    private final Simulation sim;
    private final Style style;
    private final List<InputEvent> recorded = new ArrayList<>();
    private int lastStage = -1;
    private int mistakes;
    private int pressAt = -1;

    public Autoplay(Simulation sim, Style style) {
        this.sim = sim;
        this.style = style;
    }

    /** Plays a fresh copy of {@code spec} to completion (or expiry) and returns the inputs it used. */
    public static List<InputEvent> play(GameSpec spec, Style style) {
        Simulation s = new Simulation(spec);
        Autoplay bot = new Autoplay(s, style);
        int limit = spec.maxDurationMs() > 0 ? spec.maxDurationMs() : 120_000;
        for (int t = 0; t <= limit && !s.finished(); t += Simulation.STEP_MS) bot.act(t);
        return bot.recorded();
    }

    public List<InputEvent> recorded() {
        return List.copyOf(recorded);
    }

    /** Decides at {@code timeMs} (call with nondecreasing times) and applies any input it chooses to its simulation. */
    public List<InputEvent> act(int timeMs) {
        List<InputEvent> out = new ArrayList<>();
        sim.advanceTo(timeMs);
        if (sim.finished()) {
            releaseAll(timeMs, out);
            return out;
        }
        if (sim.stageIndex() != lastStage) {
            lastStage = sim.stageIndex();
            mistakes = 0;
            pressAt = -1;
        }
        Simulation.StageView v = sim.current();
        switch (v.spec().kind()) {
            case ALIGN -> align(timeMs, (StageSpec.Align) v.spec(), v, out);
            case HOLD_BAND -> band(timeMs, v, out);
            case TIMED_TRIGGER -> trigger(timeMs, (StageSpec.TimedTrigger) v.spec(), v, out);
            case STROKES -> strokes(timeMs, v, out);
        }
        return out;
    }

    private void align(int t, StageSpec.Align s, Simulation.StageView v, List<InputEvent> out) {
        if (sim.held(Control.PRIMARY)) { set(t, Control.PRIMARY, false, out); return; }
        double off = v.value() - s.target();
        double aim = style == Style.SLOPPY && mistakes == 0 ? s.tolerance() * 2.5 : s.tolerance() * 0.2;
        if (Math.abs(off) > aim) {
            set(t, off < 0 ? Control.RIGHT : Control.LEFT, true, out);
            set(t, off < 0 ? Control.LEFT : Control.RIGHT, false, out);
            return;
        }
        set(t, Control.LEFT, false, out);
        set(t, Control.RIGHT, false, out);
        if (style == Style.SLOPPY && mistakes == 0) mistakes++;
        set(t, Control.PRIMARY, true, out);
    }

    private void band(int t, Simulation.StageView v, List<InputEvent> out) {
        set(t, Control.LEFT, false, out);
        set(t, Control.RIGHT, false, out);
        if (sim.spec().untimed()) { set(t, Control.PRIMARY, true, out); return; }
        double[] w = v.window();
        double target = style == Style.SLOPPY && sim.stageElapsed() < 1200 ? w[1] + 0.06 : (w[0] + w[1]) / 2;
        // Hysteresis, as a person would: hold to a little above the target, release to a little below it.
        double margin = (w[1] - w[0]) * 0.25;
        boolean holding = sim.held(Control.PRIMARY);
        if (holding && v.value() >= target + margin) set(t, Control.PRIMARY, false, out);
        else if (!holding && v.value() <= target - margin) set(t, Control.PRIMARY, true, out);
    }

    private void trigger(int t, StageSpec.TimedTrigger s, Simulation.StageView v, List<InputEvent> out) {
        double marker = v.value();
        boolean sloppyMiss = style == Style.SLOPPY && mistakes == 0;
        boolean inWindow = Math.abs(marker - s.center()) <= s.halfWidth() * 0.35;
        boolean outside = Math.abs(marker - s.center()) > s.halfWidth() * 1.5;
        boolean fire = sloppyMiss ? outside && sim.stageElapsed() > 200 : inWindow;
        if (s.trigger() == StageSpec.Trigger.PRESS) {
            if (sim.held(Control.PRIMARY)) { set(t, Control.PRIMARY, false, out); return; }
            if (fire) {
                if (sloppyMiss) mistakes++;
                set(t, Control.PRIMARY, true, out);
            }
        } else {
            if (!sim.held(Control.PRIMARY)) { set(t, Control.PRIMARY, true, out); return; }
            if (fire) {
                if (sloppyMiss) mistakes++;
                set(t, Control.PRIMARY, false, out);
            }
        }
    }

    private void strokes(int t, Simulation.StageView v, List<InputEvent> out) {
        StageSpec.Strokes s = (StageSpec.Strokes) v.spec();
        if (!sim.held(Control.PRIMARY)) {
            if (pressAt < 0) pressAt = t + 60;
            if (t >= pressAt) {
                pressAt = -1;
                set(t, Control.PRIMARY, true, out);
            }
            return;
        }
        if (v.waitingForLift()) { set(t, Control.PRIMARY, false, out); return; }
        double junction = 1.0;
        double half = s.junctionHalfMs() / (double) s.length(v.index());
        boolean bleedOnPurpose = style == Style.SLOPPY && v.index() == 0 && !v.needsRepair();
        if (bleedOnPurpose) return; // keep holding through the junction once
        if (v.value() >= junction - half * 0.1) set(t, Control.PRIMARY, false, out);
    }

    private void releaseAll(int t, List<InputEvent> out) {
        for (Control c : new Control[]{Control.PRIMARY, Control.LEFT, Control.RIGHT}) set(t, c, false, out);
    }

    private void set(int t, Control c, boolean down, List<InputEvent> out) {
        if (sim.held(c) == down) return;
        InputEvent e = new InputEvent(t, c, down ? 1 : 0);
        if (sim.apply(e)) {
            recorded.add(e);
            out.add(e);
        }
    }
}
