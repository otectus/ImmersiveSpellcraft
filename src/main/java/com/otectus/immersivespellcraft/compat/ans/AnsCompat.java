package com.otectus.immersivespellcraft.compat.ans;

import com.otectus.arsnspells.block.GlyphPressBlockEntity;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.data.CoverageExporter;

/** Ars 'n Spells integration entry point; reached by name only after the version check (and Ars Nouveau's). */
public final class AnsCompat {
    public static String init() {
        if (!PressGateHolder.class.isAssignableFrom(GlyphPressBlockEntity.class))
            throw new IllegalStateException("the Glyph Press gate mixin did not apply");
        GlyphPressAdapter adapter = new GlyphPressAdapter();
        PressGate.adapter = adapter;
        AdapterRegistry.register(adapter);
        CoverageExporter.register(new AnsCoverage());
        return "Glyph Press: Glyph Embossing";
    }

    private AnsCompat() {}
}
