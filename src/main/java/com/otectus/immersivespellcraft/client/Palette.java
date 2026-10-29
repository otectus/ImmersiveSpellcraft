package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.minigame.GameTypes;

/**
 * Colors for the panel. Every cue also differs by shape and text, so color is never the only signal. The default good
 * and warning colors are Okabe-Ito (colorblind safe); high contrast swaps the material backgrounds for near-black with
 * white marks.
 */
public final class Palette {
    public final int panel, panelEdge, area, areaEdge, ink, inkFaint, accent, good, warn, bad, text, textDim, band, track;

    private Palette(int panel, int panelEdge, int area, int areaEdge, int ink, int inkFaint, int accent, int good, int warn, int bad,
                    int text, int textDim, int band, int track) {
        this.panel = panel; this.panelEdge = panelEdge; this.area = area; this.areaEdge = areaEdge; this.ink = ink; this.inkFaint = inkFaint;
        this.accent = accent; this.good = good; this.warn = warn; this.bad = bad; this.text = text; this.textDim = textDim;
        this.band = band; this.track = track;
    }

    public static Palette forGame(String game, int schoolColor) {
        boolean contrast = ClientConfig.get(ClientConfig.HIGH_CONTRAST);
        boolean safe = ClientConfig.get(ClientConfig.COLORBLIND_SAFE);
        int good = safe ? 0xFF009E73 : 0xFF3FD36B, warn = safe ? 0xFFE69F00 : 0xFFF2C744, bad = safe ? 0xFFD55E00 : 0xFFE04848;
        if (contrast)
            return new Palette(0xF0101010, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF7A7A7A, 0xFFFFFFFF, good, warn, bad,
                    0xFFFFFFFF, 0xFFDDDDDD, 0x90FFFFFF, 0xFF9A9A9A);
        int accent = schoolColor != 0 ? 0xFF000000 | schoolColor : 0xFFB57BFF;
        return switch (game) {
            // Parchment and ink, with Source-violet glow.
            case GameTypes.SIGIL_INSCRIPTION -> new Palette(0xF02A2033, 0xFF8C6BB1, 0xFFE9DFC6, 0xFF8A7552, 0xFF2B1D3A, 0x552B1D3A,
                    0xFFB57BFF, good, warn, bad, 0xFFF3EAD8, 0xFFBBAFC8, 0x66B57BFF, 0x40000000);
            // Slate forge, school-colored focus and ink.
            case GameTypes.SCROLL_FORGING -> new Palette(0xF01C1F26, 0xFF6F7C91, 0xFF262B35, 0xFF56627A, 0xFFE8E2D0, 0x55E8E2D0,
                    accent, good, warn, bad, 0xFFEDEFF3, 0xFFAAB3C2, 0x55FFFFFF & accent | 0x55000000, 0xFF3A4252);
            // Hot metal press: brass edges and a molten rune plate.
            default -> new Palette(0xF0221A14, 0xFFB08A4C, 0xFF2E241B, 0xFF8C6B3A, 0xFFF4D7A1, 0x55F4D7A1, 0xFFFF9A3C, good, warn, bad,
                    0xFFF6EBDD, 0xFFC9B79F, 0x55FF9A3C, 0xFF4A3A2A);
        };
    }

    public static int alpha(int argb, float a) {
        int base = (argb >>> 24) & 0xFF;
        return ((int) (base * Math.max(0, Math.min(1, a))) << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            r |= ((int) (ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return r;
    }
}
