package com.otectus.immersivespellcraft.minigame;

/**
 * A feedback moment for presentation: captions, sounds and highlights. Cues never feed back into the simulation, so
 * the server can ignore them entirely.
 */
public record Cue(int timeMs, int stage, Kind kind) {
    public enum Kind {
        ALIGNED, MISALIGNED, BAND_ENTERED, BAND_LOW, BAND_HIGH, TRIGGER_HIT, TRIGGER_MISSED,
        STROKE_DONE, STROKE_LIFTED, STROKE_BLEED, STAGE_COMPLETE, COMPLETE, EXPIRED
    }
}
