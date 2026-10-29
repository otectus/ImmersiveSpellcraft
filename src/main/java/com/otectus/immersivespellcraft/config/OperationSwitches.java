package com.otectus.immersivespellcraft.config;

import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.core.ProfileQuery;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** The server switches for one adapter: master, integration and operation toggles, and the recipe/output exclusions. */
public final class OperationSwitches {

    public static boolean enabled(ResourceLocation adapter) {
        if (!ServerConfig.enabled()) return false;
        if (Operations.ARS_SCRIBE_ADAPTER.equals(adapter))
            return ServerConfig.get(ServerConfig.ARS_NOUVEAU) && ServerConfig.get(ServerConfig.ARS_GLYPH_CRAFTING);
        if (Operations.IRONS_SCROLL_FORGE_ADAPTER.equals(adapter))
            return ServerConfig.get(ServerConfig.IRONS_SPELLBOOKS) && ServerConfig.get(ServerConfig.IRONS_SCROLL_FORGE);
        if (Operations.ANS_GLYPH_PRESS_ADAPTER.equals(adapter))
            return ServerConfig.get(ServerConfig.ARS_N_SPELLS) && ServerConfig.get(ServerConfig.ANS_GLYPH_PRESS);
        return false;
    }

    /** Whether the operation's recipe or its output (the query's first item) is excluded by the server. */
    public static boolean excluded(ProfileQuery query) {
        if (query.recipe() != null && ServerConfig.get(ServerConfig.EXCLUDED_RECIPES).contains(query.recipe().toString())) return true;
        if (query.items().isEmpty()) return false;
        ItemStack output = query.items().get(0);
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(output.getItem());
        for (String entry : ServerConfig.get(ServerConfig.EXCLUDED_OUTPUTS)) {
            if (entry.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
                if (tag != null && output.is(TagKey.create(Registries.ITEM, tag))) return true;
            } else if (key != null && entry.equals(key.toString())) {
                return true;
            }
        }
        return false;
    }

    private OperationSwitches() {}
}
