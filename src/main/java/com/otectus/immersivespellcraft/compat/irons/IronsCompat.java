package com.otectus.immersivespellcraft.compat.irons;

import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.data.CoverageExporter;
import com.otectus.immersivespellcraft.session.MenuGuards;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Iron's Spells 'n Spellbooks integration entry point; reached by name only after the version check. */
public final class IronsCompat {
    public static String init() {
        ScrollForgeAdapter adapter = new ScrollForgeAdapter();
        AdapterRegistry.register(adapter);
        MenuGuards.register(adapter);
        CoverageExporter.register(new IronsCoverage());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.otectus.immersivespellcraft.compat.irons.client.IronsClient.init());
        return "Scroll Forge: Scroll Forging";
    }

    private IronsCompat() {}
}
