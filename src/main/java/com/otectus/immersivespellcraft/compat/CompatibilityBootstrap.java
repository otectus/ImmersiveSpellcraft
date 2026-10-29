package com.otectus.immersivespellcraft.compat;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.compat.family.FamilyIntegrations;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.Operations;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads each integration only after its mod and version are confirmed, reaching it by class name so no core class
 * ever links against an optional mod. A failing integration is made dormant with one warning; the rest of the mod and
 * every other station carry on. With none of the target mods installed the core still loads and says so.
 */
public final class CompatibilityBootstrap {
    public record Integration(String modId, IntegrationVersions.Status status, boolean active, String detail) {}

    private static final Map<String, Integration> STATUS = new LinkedHashMap<>();

    public static void init() {
        load(Operations.ARS, Operations.ARS_SCRIBE_ADAPTER, "com.otectus.immersivespellcraft.compat.ars.ArsCompat", null);
        load(Operations.IRONS, Operations.IRONS_SCROLL_FORGE_ADAPTER, "com.otectus.immersivespellcraft.compat.irons.IronsCompat", null);
        load(Operations.ANS, Operations.ANS_GLYPH_PRESS_ADAPTER, "com.otectus.immersivespellcraft.compat.ans.AnsCompat", Operations.ARS);
        FamilyIntegrations.init();
        if (STATUS.values().stream().noneMatch(Integration::active))
            ImmersiveSpellcraft.LOGGER.info("Immersive Spellcraft loaded with no active integrations: none of Ars Nouveau, "
                    + "Iron's Spells 'n Spellbooks or Ars 'n Spells is installed in a supported version, so every station is native.");
    }

    private static void load(String modId, ResourceLocation adapter, String entryClass, String requires) {
        IntegrationVersions.Status status = IntegrationVersions.status(modId);
        if (!status.loaded()) {
            dormant(modId, status, adapter, "not installed");
            return;
        }
        if (!status.supported()) {
            dormant(modId, status, adapter, "version " + status.version() + " is not supported (supported " + status.range() + ")");
            ImmersiveSpellcraft.LOGGER.warn("Immersive Spellcraft: {} {} is outside the supported range {}; its stations stay native",
                    modId, status.version(), status.range());
            return;
        }
        if (requires != null && !IntegrationVersions.status(requires).supported()) {
            dormant(modId, status, adapter, "needs a supported " + requires);
            return;
        }
        try {
            Object detail = Class.forName(entryClass).getMethod("init").invoke(null);
            STATUS.put(modId, new Integration(modId, status, true, detail == null ? "active" : detail.toString()));
            ImmersiveSpellcraft.LOGGER.info("Immersive Spellcraft integration for {} {} enabled", modId, status.version());
        } catch (Throwable t) {
            Throwable cause = t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : t;
            dormant(modId, status, adapter, "failed to initialize: " + cause);
            ImmersiveSpellcraft.LOGGER.warn("Immersive Spellcraft integration for {} failed to initialize and is disabled; "
                    + "that mod's stations keep their native behavior", modId, cause);
        }
    }

    private static void dormant(String modId, IntegrationVersions.Status status, ResourceLocation adapter, String why) {
        STATUS.put(modId, new Integration(modId, status, false, why));
        AdapterRegistry.dormant(adapter, why);
    }

    public static Map<String, Integration> integrations() {
        return Map.copyOf(STATUS);
    }

    private CompatibilityBootstrap() {}
}
