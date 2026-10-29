package com.otectus.immersivespellcraft.data;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

/** An integration's runtime inventory for the coverage manifest (glyphs, rituals, spells, schools...). */
public interface CoverageContributor {
    String integration();

    void contribute(MinecraftServer server, JsonObject out);
}
