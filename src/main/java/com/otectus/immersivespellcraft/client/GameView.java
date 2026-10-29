package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.minigame.Control;
import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.SimRandom;
import com.otectus.immersivespellcraft.minigame.Simulation;
import com.otectus.immersivespellcraft.minigame.StageSpec;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Draws the current stage of a simulation inside the game area. Each game has its own material: parchment and ink for
 * Sigil Inscription, a slate forge with a school-colored focus for Scroll Forging, and a brass press with a molten
 * plate for Glyph Embossing. Every target is a shape plus a color plus a label.
 */
public final class GameView {
    private static long cachedSeed = Long.MIN_VALUE;
    /** The current highlight strength: a gentle pulse on the active target, steady with reduced motion. */
    private static float glow = 1;
    private static float[][] cachedJunctions, cachedControls;

    public static void draw(GuiGraphics g, Simulation sim, Palette p, float x, float y, float w, float h, boolean live) {
        GameSpec spec = sim.spec();
        glow = pulse(com.otectus.immersivespellcraft.config.ClientConfig.get(com.otectus.immersivespellcraft.config.ClientConfig.REDUCED_MOTION));
        Draw d = Draw.begin(g);
        d.rect(x, y, x + w, y + h, p.area);
        d.frame(x, y, x + w, y + h, 1, p.areaEdge);
        Simulation.StageView v = sim.current();
        StageSpec s = v.spec();
        switch (s.kind()) {
            case STROKES -> strokes(d, sim, (StageSpec.Strokes) s, v, p, x, y, w, h);
            case ALIGN -> {
                if (GameTypes.GLYPH_EMBOSSING.equals(spec.type())) slider(d, (StageSpec.Align) s, v, p, x, y, w, h);
                else ring(d, (StageSpec.Align) s, v, p, x, y, w, h);
            }
            case HOLD_BAND -> band(d, sim, (StageSpec.HoldBand) s, v, p, x, y, w, h);
            case TIMED_TRIGGER -> trigger(d, sim, (StageSpec.TimedTrigger) s, v, p, x, y, w, h);
        }
        d.end();
        Font font = Minecraft.getInstance().font;
        switch (s.kind()) {
            case HOLD_BAND -> {
                double[] win = v.window();
                boolean in = v.value() >= win[0] && v.value() <= win[1];
                String key = in ? "immersive_spellcraft.panel.band.steady" : v.value() > win[1] ? "immersive_spellcraft.panel.band.high"
                        : "immersive_spellcraft.panel.band.low";
                g.drawString(font, Component.translatable(key), (int) (x + w - 70), (int) (y + h - 11), in ? p.good : p.warn, false);
            }
            case TIMED_TRIGGER -> {
                StageSpec.TimedTrigger t = (StageSpec.TimedTrigger) s;
                if (t.trigger() == StageSpec.Trigger.RELEASE)
                    g.drawString(font, Component.translatable(sim.held(Control.PRIMARY) ? "immersive_spellcraft.panel.holding"
                            : "immersive_spellcraft.panel.hold_first"), (int) x + 6, (int) (y + h - 11), p.textDim, false);
            }
            case STROKES -> {
                StageSpec.Strokes k = (StageSpec.Strokes) s;
                g.drawString(font, Component.translatable("immersive_spellcraft.panel.stroke", Math.min(k.count(), v.index() + 1), k.count()),
                        (int) x + 6, (int) (y + h - 11), Palette.alpha(p.ink, 0.85f), false);
                if (v.needsRepair()) g.drawString(font, Component.translatable("immersive_spellcraft.panel.repair"),
                        (int) (x + w - 6 - font.width(Component.translatable("immersive_spellcraft.panel.repair"))), (int) (y + h - 11), p.bad, false);
            }
            default -> {}
        }
    }

    // ---- Sigil Inscription -----------------------------------------------------------------------------------------

    private static void shape(long seed, int n) {
        if (cachedSeed == seed && cachedJunctions != null && cachedJunctions.length == n + 1) return;
        SimRandom r = new SimRandom(seed ^ 0x5161_7C0DEL);
        float[][] pts = new float[n + 1][];
        double start = r.range(0, Math.PI * 2);
        for (int i = 0; i <= n; i++) {
            double a = start + (Math.PI * 2 * 0.86) * i / n + r.range(-0.25, 0.25);
            double rad = (i % 2 == 0 ? 0.36 : 0.2) + r.range(0, 0.12);
            pts[i] = new float[]{(float) (0.5 + Math.cos(a) * rad), (float) (0.5 + Math.sin(a) * rad)};
        }
        float[][] ctl = new float[n][];
        for (int i = 0; i < n; i++) {
            float mx = (pts[i][0] + pts[i + 1][0]) / 2, my = (pts[i][1] + pts[i + 1][1]) / 2;
            float dx = pts[i + 1][0] - pts[i][0], dy = pts[i + 1][1] - pts[i][1];
            float k = (float) r.range(-0.45, 0.45);
            ctl[i] = new float[]{mx - dy * k, my + dx * k};
        }
        cachedSeed = seed;
        cachedJunctions = pts;
        cachedControls = ctl;
    }

    private static float[] at(int i, double t, float x, float y, float size) {
        float[] a = cachedJunctions[i], c = cachedControls[i], b = cachedJunctions[i + 1];
        double tt = Math.min(1, t);
        double u = 1 - tt;
        double px = u * u * a[0] + 2 * u * tt * c[0] + tt * tt * b[0];
        double py = u * u * a[1] + 2 * u * tt * c[1] + tt * tt * b[1];
        if (t > 1) {
            px += (t - 1) * 2 * (b[0] - c[0]);
            py += (t - 1) * 2 * (b[1] - c[1]);
        }
        return new float[]{x + (float) px * size, y + (float) py * size};
    }

    private static void path(Draw d, int i, double t0, double t1, float x, float y, float size, float width, int color, boolean dashed) {
        int segments = 18;
        float[] prev = at(i, t0, x, y, size);
        for (int k = 1; k <= segments; k++) {
            double t = t0 + (t1 - t0) * k / segments;
            float[] next = at(i, t, x, y, size);
            if (!dashed || k % 2 == 0) d.line(prev[0], prev[1], next[0], next[1], width, color);
            prev = next;
        }
    }

    private static void strokes(Draw d, Simulation sim, StageSpec.Strokes s, Simulation.StageView v, Palette p, float x, float y, float w, float h) {
        int n = s.count();
        shape(sim.spec().seed(), n);
        float size = Math.min(w, h) - 14;
        float ox = x + (w - size) / 2, oy = y + 4;
        int current = v.index();
        boolean done = v.done();
        for (int i = 0; i < n; i++) {
            if (i < current || done) path(d, i, 0, 1, ox, oy, size, 3, p.ink, false);
            else if (i > current) path(d, i, 0, 1, ox, oy, size, 1.5f, p.inkFaint, true);
        }
        for (int i = 0; i <= n; i++) {
            float[] j = at(Math.min(i, n - 1), i == n ? 1 : 0, ox, oy, size);
            d.disc(j[0], j[1], 2.2f, i <= current ? p.ink : p.inkFaint);
        }
        if (done) return;
        double value = v.value();
        double[] win = v.window();
        path(d, current, 0, 1, ox, oy, size, 1.5f, p.inkFaint, true);
        path(d, current, Math.max(0, win[0]), Math.min(1.5, win[1]), ox, oy, size, 6, Palette.alpha(p.accent, 0.55f * glow), false);
        if (value > 0) path(d, current, 0, Math.min(value, 1.5), ox, oy, size, 3, p.ink, false);
        float[] junction = at(current, 1, ox, oy, size);
        d.ring(junction[0], junction[1], 5.5f, 1.6f, p.accent);
        float[] quill = at(current, Math.min(value, 1.5), ox, oy, size);
        if (v.needsRepair()) d.disc(quill[0] + 1, quill[1] + 1, 4, Palette.alpha(p.bad, 0.8f));
        boolean inking = sim.held(Control.PRIMARY) && !v.waitingForLift();
        d.line(quill[0], quill[1], quill[0] + 7, quill[1] - 10, 2, inking ? p.accent : p.textDim);
        d.disc(quill[0], quill[1], inking ? 2.6f : 2f, inking ? p.accent : p.ink);
    }

    // ---- Alignment -------------------------------------------------------------------------------------------------

    private static final double SWING = Math.toRadians(150);

    private static void ring(Draw d, StageSpec.Align s, Simulation.StageView v, Palette p, float x, float y, float w, float h) {
        float cx = x + w / 2, cy = y + h / 2 + 4, r = Math.min(w, h) / 2 - 12;
        double top = -Math.PI / 2;
        d.ring(cx, cy, r, 6, p.track);
        double mark = top + s.target() * SWING, tol = s.tolerance() * SWING;
        d.arc(cx, cy, r - 6, r + 6, mark - tol, mark + tol, Palette.alpha(p.good, 0.5f * glow));
        d.line(cx + (float) Math.cos(mark) * (r + 9), cy + (float) Math.sin(mark) * (r + 9),
                cx + (float) Math.cos(mark) * (r + 3), cy + (float) Math.sin(mark) * (r + 3), 3, p.good);
        double notch = top + v.value() * SWING;
        boolean aligned = Math.abs(v.value() - s.target()) <= s.tolerance();
        d.line(cx, cy, cx + (float) Math.cos(notch) * (r - 4), cy + (float) Math.sin(notch) * (r - 4), 2, Palette.alpha(p.accent, 0.6f));
        d.tri(cx + (float) Math.cos(notch) * (r + 7), cy + (float) Math.sin(notch) * (r + 7),
                cx + (float) Math.cos(notch - 0.12) * (r - 4), cy + (float) Math.sin(notch - 0.12) * (r - 4),
                cx + (float) Math.cos(notch + 0.12) * (r - 4), cy + (float) Math.sin(notch + 0.12) * (r - 4), aligned ? p.good : p.accent);
        d.disc(cx, cy, 7, p.accent);
        d.disc(cx, cy, 4, p.area);
    }

    private static void slider(Draw d, StageSpec.Align s, Simulation.StageView v, Palette p, float x, float y, float w, float h) {
        float cy = y + h / 2 + 8, half = w / 2 - 18, cx = x + w / 2;
        d.rect(cx - half, cy + 10, cx + half, cy + 14, p.track);
        float plate = cx + (float) s.target() * half;
        float tol = (float) s.tolerance() * half;
        d.rect(plate - 16, cy - 10, plate + 16, cy + 10, Palette.alpha(p.band, 1));
        d.frame(plate - 16, cy - 10, plate + 16, cy + 10, 1, p.accent);
        d.rect(plate - tol, cy + 9, plate + tol, cy + 15, Palette.alpha(p.good, 0.7f * glow));
        float die = cx + (float) v.value() * half;
        boolean aligned = Math.abs(v.value() - s.target()) <= s.tolerance();
        d.rect(die - 13, cy - 30, die + 13, cy - 14, aligned ? p.good : p.ink);
        d.rect(die - 4, cy - 42, die + 4, cy - 30, p.textDim);
        d.tri(die - 5, cy - 14, die + 5, cy - 14, die, cy - 9, aligned ? p.good : p.ink);
    }

    // ---- Hold band -------------------------------------------------------------------------------------------------

    private static void band(Draw d, Simulation sim, StageSpec.HoldBand s, Simulation.StageView v, Palette p, float x, float y, float w, float h) {
        float gx0 = x + w - 34, gx1 = x + w - 18, gy0 = y + 8, gy1 = y + h - 16;
        d.rect(gx0, gy0, gx1, gy1, p.track);
        double[] win = v.window();
        float lo = gy1 - (float) win[0] * (gy1 - gy0), hi = gy1 - (float) win[1] * (gy1 - gy0);
        d.rect(gx0 - 3, hi, gx1 + 3, lo, Palette.alpha(p.good, 0.45f));
        d.frame(gx0 - 3, hi, gx1 + 3, lo, 1, p.good);
        float level = gy1 - (float) v.value() * (gy1 - gy0);
        boolean in = v.value() >= win[0] && v.value() <= win[1];
        d.rect(gx0, level, gx1, gy1, Palette.alpha(in ? p.accent : p.warn, 0.75f));
        d.rect(gx0 - 6, level - 1.5f, gx1 + 6, level + 1.5f, in ? p.good : p.warn);
        // The work itself: ink filling the sigil, or the plate engraving under the die.
        float cx = x + (w - 40) / 2, cy = y + h / 2 + 2, r = Math.min(w - 40, h) / 2 - 14;
        double progress = v.progress();
        if (GameTypes.GLYPH_EMBOSSING.equals(sim.spec().type())) {
            d.rect(cx - r, cy - r / 2, cx + r, cy + r / 2, p.track);
            d.rect(cx - r, cy - r / 2, cx - r + (float) (2 * r * progress), cy + r / 2, Palette.alpha(p.accent, 0.8f));
            d.frame(cx - r, cy - r / 2, cx + r, cy + r / 2, 1, p.areaEdge);
            boolean pressing = sim.held(Control.PRIMARY);
            float dieY = cy - r / 2 - (pressing ? 4 : 10);
            d.rect(cx - 14, dieY - 12, cx + 14, dieY, pressing ? p.accent : p.ink);
        } else {
            d.ring(cx, cy, r, 3, p.track);
            d.arc(cx, cy, r - 3, r + 3, -Math.PI / 2, -Math.PI / 2 + 2 * Math.PI * progress, p.accent);
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                d.line(cx, cy, cx + (float) Math.cos(a) * r * 0.62f, cy + (float) Math.sin(a) * r * 0.62f, 1.5f,
                        Palette.alpha(p.accent, (float) Math.min(1, 0.2 + progress)));
            }
            d.disc(cx, cy, 3, p.accent);
        }
    }

    // ---- Timed trigger ---------------------------------------------------------------------------------------------

    private static void trigger(Draw d, Simulation sim, StageSpec.TimedTrigger s, Simulation.StageView v, Palette p, float x, float y, float w, float h) {
        float cx = x + w / 2, cy = y + h - 22, r = Math.min(w / 2 - 14, h - 34);
        d.arc(cx, cy, r - 5, r + 5, Math.PI, 2 * Math.PI, p.track);
        double a0 = Math.PI + (s.center() - s.halfWidth()) * Math.PI, a1 = Math.PI + (s.center() + s.halfWidth()) * Math.PI;
        d.arc(cx, cy, r - 7, r + 7, a0, a1, Palette.alpha(p.good, 0.65f * glow));
        double centre = Math.PI + s.center() * Math.PI;
        d.line(cx + (float) Math.cos(centre) * (r + 9), cy + (float) Math.sin(centre) * (r + 9),
                cx + (float) Math.cos(centre) * (r - 9), cy + (float) Math.sin(centre) * (r - 9), 2, p.good);
        double m = Math.PI + v.value() * Math.PI;
        boolean in = Math.abs(v.value() - s.center()) <= s.halfWidth();
        d.disc(cx + (float) Math.cos(m) * r, cy + (float) Math.sin(m) * r, 5, in ? p.good : p.accent);
        d.line(cx, cy, cx + (float) Math.cos(m) * (r - 6), cy + (float) Math.sin(m) * (r - 6), 1.5f, Palette.alpha(p.accent, 0.5f));
        boolean holding = sim.held(Control.PRIMARY);
        float sealR = 12 + (s.trigger() == StageSpec.Trigger.RELEASE && holding ? 3 : 0);
        d.disc(cx, cy, sealR, holding ? p.accent : p.ink);
        d.ring(cx, cy, sealR + 3, 1.5f, p.areaEdge);
    }

    /** A gentle pulse for highlights; flat when reduced motion is on. */
    static float pulse(boolean reduced) {
        return reduced ? 1 : 0.75f + 0.25f * (float) Math.sin(Util.getMillis() / 180.0);
    }

    private GameView() {}
}
