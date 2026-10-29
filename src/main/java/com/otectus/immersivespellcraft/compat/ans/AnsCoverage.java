package com.otectus.immersivespellcraft.compat.ans;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.RitualRegistry;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.data.CoverageContributor;
import net.minecraft.server.MinecraftServer;

import java.util.TreeSet;

/** Ars 'n Spells' runtime inventory: its registered rituals and how many imported Iron's glyphs are present. */
final class AnsCoverage implements CoverageContributor {
    @Override public String integration() { return Operations.ANS; }

    @Override
    public void contribute(MinecraftServer server, JsonObject out) {
        JsonArray rituals = new JsonArray();
        new TreeSet<>(RitualRegistry.getRitualMap().keySet()).stream().filter(id -> id.getNamespace().equals(Operations.ANS))
                .forEach(id -> rituals.add(id.toString()));
        out.add("rituals", rituals);
        out.addProperty("imported_glyph_count",
                GlyphRegistry.getSpellpartMap().keySet().stream().filter(id -> id.getNamespace().equals(Operations.ANS)).count());
    }
}
