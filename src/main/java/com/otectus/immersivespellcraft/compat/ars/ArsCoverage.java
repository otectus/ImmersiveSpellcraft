package com.otectus.immersivespellcraft.compat.ars;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.RitualRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.data.CoverageContributor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.TreeMap;

/** Ars Nouveau's runtime inventory: every registered glyph (by kind, tier and namespace) and every ritual. */
final class ArsCoverage implements CoverageContributor {
    @Override public String integration() { return Operations.ARS; }

    @Override
    public void contribute(MinecraftServer server, JsonObject out) {
        Map<String, Integer> byNamespace = new TreeMap<>();
        JsonArray glyphs = new JsonArray();
        new TreeMap<>(GlyphRegistry.getSpellpartMap()).forEach((ResourceLocation id, AbstractSpellPart part) -> {
            JsonObject o = new JsonObject();
            o.addProperty("id", id.toString());
            o.addProperty("kind", part instanceof AbstractCastMethod ? "form" : part instanceof AbstractEffect ? "effect"
                    : part instanceof AbstractAugment ? "augment" : "other");
            o.addProperty("tier", part.getConfigTier() == null ? 0 : part.getConfigTier().value);
            glyphs.add(o);
            byNamespace.merge(id.getNamespace(), 1, Integer::sum);
        });
        JsonObject counts = new JsonObject();
        byNamespace.forEach(counts::addProperty);
        out.add("glyph_count_by_namespace", counts);
        out.add("glyphs", glyphs);
        JsonArray rituals = new JsonArray();
        new TreeMap<>(RitualRegistry.getRitualMap()).keySet().forEach(id -> rituals.add(id.toString()));
        out.addProperty("ritual_count", rituals.size());
        out.add("rituals", rituals);
    }
}
