package com.otectus.immersivespellcraft.compat.irons;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.data.CoverageContributor;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.server.MinecraftServer;

/** Iron's runtime inventory: every registered spell (school, levels, enabled, craftable) and school. */
final class IronsCoverage implements CoverageContributor {
    @Override public String integration() { return Operations.IRONS; }

    @Override
    public void contribute(MinecraftServer server, JsonObject out) {
        JsonArray schools = new JsonArray();
        for (SchoolType school : SchoolRegistry.REGISTRY.get().getValues()) schools.add(school.getId().toString());
        out.add("schools", schools);
        JsonArray spells = new JsonArray();
        int enabled = 0;
        for (AbstractSpell spell : SpellRegistry.REGISTRY.get().getValues()) {
            if (spell == SpellRegistry.none()) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", spell.getSpellId());
            o.addProperty("school", spell.getSchoolType().getId().toString());
            o.addProperty("min_level", spell.getMinLevel());
            o.addProperty("max_level", spell.getMaxLevel());
            o.addProperty("enabled", spell.isEnabled());
            o.addProperty("scroll_forge_craftable", spell.allowCrafting());
            if (spell.isEnabled()) enabled++;
            spells.add(o);
        }
        out.addProperty("spell_count", spells.size());
        out.addProperty("enabled_spell_count", enabled);
        out.add("spells", spells);
    }
}
