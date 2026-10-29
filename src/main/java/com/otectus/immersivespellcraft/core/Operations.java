package com.otectus.immersivespellcraft.core;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The adapters this release knows and the operations they own. A profile may name only these adapters; an adapter
 * whose target mod is absent or unsupported is dormant, not unknown, so its profiles still load cleanly.
 */
public final class Operations {
    public static final ResourceLocation ARS_GLYPH_CRAFT = ImmersiveSpellcraft.id("ars/glyph_craft");
    public static final ResourceLocation IRONS_SCROLL_FORGE = ImmersiveSpellcraft.id("irons/scroll_forge");
    public static final ResourceLocation ANS_GLYPH_PRESS = ImmersiveSpellcraft.id("ans/glyph_press");

    public static final ResourceLocation ARS_SCRIBE_ADAPTER = ImmersiveSpellcraft.id("ars_scribe");
    public static final ResourceLocation IRONS_SCROLL_FORGE_ADAPTER = ImmersiveSpellcraft.id("irons_scroll_forge");
    public static final ResourceLocation ANS_GLYPH_PRESS_ADAPTER = ImmersiveSpellcraft.id("ans_glyph_press");

    public static final String ARS = "ars_nouveau";
    public static final String IRONS = "irons_spellbooks";
    public static final String ANS = "ars_n_spells";

    /** One known adapter: the operation it owns and the mod it integrates. */
    public record Known(ResourceLocation adapter, ResourceLocation operation, String integration, String coverageId) {}

    public static final List<Known> KNOWN = List.of(
            new Known(ARS_SCRIBE_ADAPTER, ARS_GLYPH_CRAFT, ARS, "A01"),
            new Known(IRONS_SCROLL_FORGE_ADAPTER, IRONS_SCROLL_FORGE, IRONS, "I01"),
            new Known(ANS_GLYPH_PRESS_ADAPTER, ANS_GLYPH_PRESS, ANS, "N01"));

    public static Known known(ResourceLocation adapter) {
        for (Known k : KNOWN) if (k.adapter().equals(adapter)) return k;
        return null;
    }

    private Operations() {}
}
