package com.otectus.immersivespellcraft.compat.ars;

/**
 * Implemented on Ars Nouveau's {@code ScribesTile} by {@code ScribesTileMixin}: the inscription gate of the table's
 * current glyph job. Saved with the tile (key {@code immersive_spellcraft}) and in its update tag, so a restart keeps
 * a finished inscription and clients can show that a glyph is waiting.
 */
public interface ScribeGateHolder {
    int immersivespellcraft$gateState();

    long immersivespellcraft$gateJob();

    String immersivespellcraft$gateRecipe();

    void immersivespellcraft$setGate(int state, long job, String recipe);
}
