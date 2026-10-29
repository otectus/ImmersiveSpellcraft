package com.otectus.immersivespellcraft.compat.family;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The other Immersive mods and the operations they own when installed together. Ownership is by operation, not by
 * block namespace: none of the 0.1.0 operations (glyph crafting, scroll forging, glyph pressing) is claimed by another
 * family mod, so coexistence needs no runtime arbitration yet. This records what was found for diagnostics.
 */
public final class FamilyIntegrations {
    public static final Map<String, String> OWNED_ELSEWHERE = Map.of(
            "immersive_enchanting", "native enchanting-table offers and anvil/grindstone enchantment binding",
            "immersive_apotheosis", "Apotheosis reforge, salvage, socket and gem cutting",
            "immersive_smithing", "base metal equipment manufacture",
            "immersive_tanning", "base leather manufacture and stitching",
            "immersive_jewelry", "base jewelry creation and ordinary gem work",
            "immersive_brewing", "its own brewing workshop stations");

    private static final List<String> PRESENT = new ArrayList<>();

    public static void init() {
        PRESENT.clear();
        OWNED_ELSEWHERE.keySet().stream().sorted().filter(id -> ModList.get().isLoaded(id)).forEach(PRESENT::add);
        if (!PRESENT.isEmpty())
            ImmersiveSpellcraft.LOGGER.info("Immersive family mods present: {}. None owns an operation Immersive Spellcraft performs.", PRESENT);
    }

    public static List<String> present() {
        return List.copyOf(PRESENT);
    }

    private FamilyIntegrations() {}
}
