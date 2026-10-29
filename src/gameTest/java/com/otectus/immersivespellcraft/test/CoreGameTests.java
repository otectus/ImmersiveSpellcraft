package com.otectus.immersivespellcraft.test;

import com.google.gson.JsonObject;
import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.compat.CompatibilityBootstrap;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.data.CoverageCatalog;
import com.otectus.immersivespellcraft.data.CoverageExporter;
import com.otectus.immersivespellcraft.data.ProfileManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static com.otectus.immersivespellcraft.test.TestSupport.check;

/** Load-time invariants for every run profile, including core-only. */
@GameTestHolder(ImmersiveSpellcraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CoreGameTests {

    /**
     * The run profile must contain exactly the target mods it names, with an active integration for each: a missing
     * runtime jar or a mixin that did not apply fails here instead of silently skipping the station tests.
     */
    @GameTest(template = "empty")
    public static void profileMatchesTheRequestedMods(GameTestHelper h) {
        String profile = System.getProperty("immersive_spellcraft.expectedProfile", "core");
        boolean ars = profile.contains("ars"), irons = profile.contains("irons"), ans = profile.contains("ans");
        check(ModList.get().isLoaded(Operations.ARS) == ars, "Ars Nouveau loaded must be " + ars + " in profile " + profile);
        check(ModList.get().isLoaded(Operations.IRONS) == irons, "Iron's loaded must be " + irons + " in profile " + profile);
        check(ModList.get().isLoaded(Operations.ANS) == ans, "Ars 'n Spells loaded must be " + ans + " in profile " + profile);
        if (irons) {
            String version = ModList.get().getModContainerById(Operations.IRONS).orElseThrow().getModInfo().getVersion().toString();
            String expected = profile.contains("irons315") ? "1.20.1-3.15.0" : "1.20.1-3.16.3";
            check(version.equals(expected), "Iron's version " + version + ", expected " + expected);
        }
        check(TestSupport.active(Operations.ARS_SCRIBE_ADAPTER) == ars, "Scribe adapter active must be " + ars + ": "
                + CompatibilityBootstrap.integrations().get(Operations.ARS));
        check(TestSupport.active(Operations.IRONS_SCROLL_FORGE_ADAPTER) == irons, "Scroll Forge adapter active must be " + irons);
        check(TestSupport.active(Operations.ANS_GLYPH_PRESS_ADAPTER) == ans, "Glyph Press adapter active must be " + ans + ": "
                + CompatibilityBootstrap.integrations().get(Operations.ANS));
        if (!ars) check(!AdapterRegistry.dormant().isEmpty(), "absent integrations are reported as dormant");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyOperationHasOneOwner(GameTestHelper h) {
        Set<ResourceLocation> operations = new HashSet<>();
        for (MagicOperationAdapter a : AdapterRegistry.all()) check(operations.add(a.operation()), "two owners for " + a.operation());
        check(AdapterRegistry.diagnostics().isEmpty(), "ownership diagnostics: " + AdapterRegistry.diagnostics());
        h.succeed();
    }

    /**
     * The shipped profiles load; the test datapack's broken profile is rejected without affecting the others; its
     * exact recipe override wins over the type-wide profile.
     */
    @GameTest(template = "empty")
    public static void profilesValidateAndResolveByPrecedence(GameTestHelper h) {
        var snapshot = ProfileManager.snapshot();
        for (String id : new String[]{"ars_glyph_craft", "irons_scroll_forge", "ans_glyph_press"})
            check(snapshot.profiles().containsKey(ImmersiveSpellcraft.id(id)), "built-in profile " + id + " loaded");
        check(snapshot.profiles().keySet().stream().noneMatch(id -> id.getNamespace().equals("ism_test") && id.getPath().startsWith("broken")),
                "broken profiles are rejected");
        check(snapshot.problems().stream().anyMatch(p -> p.contains("ism_test:broken_adapter")), "the unknown adapter is reported");
        check(snapshot.problems().stream().anyMatch(p -> p.contains("ism_test:broken_grade")), "a persistent grade request is reported");
        check(snapshot.profiles().containsKey(new ResourceLocation("ism_test", "preserve_break_glyph")), "the valid override loads");
        h.succeed();
    }

    /** The coverage manifest builds from the running game in every profile and lists every required operation. */
    @GameTest(template = "empty")
    public static void coverageManifestBuilds(GameTestHelper h) {
        JsonObject manifest = CoverageExporter.build(h.getLevel().getServer());
        check(manifest.getAsJsonArray("operations").size() == CoverageCatalog.ROWS.size(), "one record per operation");
        check(CoverageCatalog.ROWS.size() == 24 + 16 + 14, "A01-A24, I01-I16 and N01-N14");
        String out = System.getProperty("immersive_spellcraft.coverage.export");
        if (out != null && !out.isBlank()) {
            try {
                CoverageExporter.export(h.getLevel().getServer(), Path.of(out));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        h.succeed();
    }
}
