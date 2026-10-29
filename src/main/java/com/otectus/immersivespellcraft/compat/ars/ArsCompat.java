package com.otectus.immersivespellcraft.compat.ars;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.data.CoverageExporter;
import net.minecraftforge.common.MinecraftForge;

/** Ars Nouveau integration entry point; reached by name only after the version check. */
public final class ArsCompat {
    public static String init() {
        if (!ScribeGateHolder.class.isAssignableFrom(ScribesTile.class))
            throw new IllegalStateException("the Scribe's Table gate mixin did not apply");
        ScribeGlyphAdapter adapter = new ScribeGlyphAdapter();
        ScribeGate.adapter = adapter;
        AdapterRegistry.register(adapter);
        MinecraftForge.EVENT_BUS.register(ArsEvents.class);
        CoverageExporter.register(new ArsCoverage());
        return "Scribe's Table: Sigil Inscription";
    }

    private ArsCompat() {}
}
