package com.otectus.immersivespellcraft.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.compat.CompatMixinPlugin;
import com.otectus.immersivespellcraft.compat.CompatibilityBootstrap;
import com.otectus.immersivespellcraft.compat.IntegrationVersions;
import com.otectus.immersivespellcraft.compat.family.FamilyIntegrations;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.Operations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Builds the machine-readable coverage manifest from the running game: loaded versions and file hashes, active and
 * dormant integrations, every registered block, block entity, menu, recipe type, serializer and recipe of the target
 * mods, each integration's own inventory (glyphs, rituals, spells, schools), and one record per required operation.
 * Registries alone miss hard-coded branches and a class in a source tree proves nothing, so the operation records come
 * from {@link CoverageCatalog}, which was written from source inspection.
 */
public final class CoverageExporter {
    private static final List<String> NAMESPACES = List.of(Operations.ARS, Operations.IRONS, Operations.ANS);
    private static final List<CoverageContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    public static void register(CoverageContributor contributor) {
        CONTRIBUTORS.add(contributor);
    }

    public static Path export(MinecraftServer server, Path target) throws IOException {
        JsonObject root = build(server);
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.writeString(target, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root));
        return target;
    }

    public static JsonObject build(MinecraftServer server) {
        JsonObject root = new JsonObject();
        root.addProperty("generated_by", ImmersiveSpellcraft.MOD_ID + " " + version(ImmersiveSpellcraft.MOD_ID));
        root.addProperty("generated_at", Instant.now().toString());
        root.addProperty("profile", System.getProperty("immersive_spellcraft.expectedProfile", "unspecified"));

        JsonObject mods = new JsonObject();
        for (String id : List.of("minecraft", "forge", ImmersiveSpellcraft.MOD_ID, Operations.ARS, Operations.IRONS, Operations.ANS,
                "geckolib", "curios", "playeranimator", "irons_lib")) {
            JsonObject m = new JsonObject();
            m.addProperty("loaded", ModList.get().isLoaded(id));
            if (ModList.get().isLoaded(id)) {
                m.addProperty("version", version(id));
                try {
                    Path file = ModList.get().getModFileById(id).getFile().getFilePath();
                    m.addProperty("file", file.getFileName().toString());
                    if (Files.isRegularFile(file)) m.addProperty("sha256", sha256(file));
                } catch (RuntimeException | IOException e) {
                    m.addProperty("file_error", e.toString());
                }
            }
            mods.add(id, m);
        }
        root.add("mods", mods);

        JsonObject integrations = new JsonObject();
        CompatibilityBootstrap.integrations().forEach((id, i) -> {
            JsonObject o = new JsonObject();
            o.addProperty("loaded", i.status().loaded());
            o.addProperty("version", i.status().version());
            o.addProperty("supported_range", i.status().range());
            o.addProperty("supported", i.status().supported());
            o.addProperty("active", i.active());
            o.addProperty("detail", i.detail());
            integrations.add(id, o);
        });
        root.add("integrations", integrations);
        JsonObject mixins = new JsonObject();
        new TreeMap<>(CompatMixinPlugin.decisions()).forEach(mixins::addProperty);
        root.add("compat_mixins_applied", mixins);
        JsonArray family = new JsonArray();
        FamilyIntegrations.present().forEach(family::add);
        root.add("family_mods_present", family);

        JsonObject owners = new JsonObject();
        for (MagicOperationAdapter a : AdapterRegistry.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("adapter", a.id().toString());
            o.addProperty("adapter_version", a.version());
            o.addProperty("capability", a.capability().name().toLowerCase());
            o.addProperty("game", a.defaultGame());
            owners.add(a.operation().toString(), o);
        }
        root.add("operation_owners", owners);
        JsonObject dormant = new JsonObject();
        AdapterRegistry.dormant().forEach((k, v) -> dormant.addProperty(k.toString(), v));
        root.add("dormant_adapters", dormant);
        JsonArray diagnostics = new JsonArray();
        AdapterRegistry.diagnostics().forEach(diagnostics::add);
        root.add("ownership_diagnostics", diagnostics);

        JsonObject profiles = new JsonObject();
        profiles.addProperty("generation", ProfileManager.generation());
        JsonArray loaded = new JsonArray();
        ProfileManager.snapshot().profiles().keySet().stream().sorted().forEach(id -> loaded.add(id.toString()));
        profiles.add("loaded", loaded);
        JsonArray problems = new JsonArray();
        ProfileManager.snapshot().problems().forEach(problems::add);
        profiles.add("problems", problems);
        root.add("profiles", profiles);

        JsonObject registries = new JsonObject();
        for (String ns : NAMESPACES) {
            if (!ModList.get().isLoaded(ns)) continue;
            JsonObject r = new JsonObject();
            JsonArray functional = new JsonArray(), decorative = new JsonArray();
            ForgeRegistries.BLOCKS.getEntries().stream().filter(e -> e.getKey().location().getNamespace().equals(ns))
                    .sorted(Map.Entry.comparingByKey((a, b) -> a.location().compareTo(b.location())))
                    .forEach(e -> (e.getValue() instanceof EntityBlock ? functional : decorative).add(e.getKey().location().toString()));
            r.add("blocks_with_block_entity", functional);
            r.addProperty("blocks_without_block_entity_count", decorative.size());
            r.add("blocks_without_block_entity", decorative);
            r.add("block_entity_types", ids(ForgeRegistries.BLOCK_ENTITY_TYPES, ns));
            r.add("menu_types", ids(ForgeRegistries.MENU_TYPES, ns));
            r.add("recipe_types", ids(ForgeRegistries.RECIPE_TYPES, ns));
            r.add("recipe_serializers", ids(ForgeRegistries.RECIPE_SERIALIZERS, ns));
            r.addProperty("item_count", ForgeRegistries.ITEMS.getKeys().stream().filter(k -> k.getNamespace().equals(ns)).count());
            r.addProperty("entity_type_count", ForgeRegistries.ENTITY_TYPES.getKeys().stream().filter(k -> k.getNamespace().equals(ns)).count());
            registries.add(ns, r);
        }
        root.add("registries", registries);

        Map<String, JsonArray> byType = new TreeMap<>();
        for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
            ResourceLocation type = ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType());
            String typeId = type == null ? "unregistered" : type.toString();
            boolean relevant = NAMESPACES.contains(recipe.getId().getNamespace()) || (type != null && NAMESPACES.contains(type.getNamespace()));
            if (relevant) byType.computeIfAbsent(typeId, k -> new JsonArray()).add(recipe.getId().toString());
        }
        JsonObject recipes = new JsonObject();
        byType.forEach((type, list) -> {
            JsonObject o = new JsonObject();
            o.addProperty("count", list.size());
            o.add("ids", list);
            recipes.add(type, o);
        });
        root.add("recipes_by_type", recipes);

        JsonObject inventories = new JsonObject();
        for (CoverageContributor c : CONTRIBUTORS) {
            JsonObject o = new JsonObject();
            try {
                c.contribute(server, o);
            } catch (RuntimeException e) {
                o.addProperty("error", e.toString());
            }
            inventories.add(c.integration(), o);
        }
        root.add("integration_inventories", inventories);

        JsonArray operations = new JsonArray();
        for (CoverageCatalog.Row row : CoverageCatalog.ROWS) {
            JsonObject o = new JsonObject();
            o.addProperty("id", row.id());
            o.addProperty("integration", row.integration());
            o.addProperty("integration_loaded", ModList.get().isLoaded(row.integration()));
            o.addProperty("system", row.system());
            o.addProperty("target_treatment", row.target());
            o.addProperty("status", row.status().name().toLowerCase());
            if (!row.adapter().isEmpty()) {
                o.addProperty("adapter", row.adapter());
                o.addProperty("adapter_active", AdapterRegistry.byId(new ResourceLocation(row.adapter())).isPresent());
            }
            if (!row.profile().isEmpty()) o.addProperty("profile", row.profile());
            if (!row.entry().isEmpty()) o.addProperty("native_entry_and_commit", row.entry());
            if (!row.routes().isEmpty()) o.addProperty("entry_routes", row.routes());
            o.addProperty("milestone", row.milestone());
            o.addProperty("notes", row.notes());
            operations.add(o);
        }
        root.add("operations", operations);
        JsonObject summary = new JsonObject();
        for (CoverageCatalog.Status s : CoverageCatalog.Status.values()) summary.addProperty(s.name().toLowerCase(), CoverageCatalog.count(s));
        summary.addProperty("registry_entries_triaged", false);
        root.add("summary", summary);
        return root;
    }

    private static JsonArray ids(IForgeRegistry<?> registry, String ns) {
        JsonArray out = new JsonArray();
        registry.getKeys().stream().filter(k -> k.getNamespace().equals(ns)).sorted().forEach(k -> out.add(k.toString()));
        return out;
    }

    private static String version(String id) {
        return ModList.get().getModContainerById(id).map(c -> c.getModInfo().getVersion().toString()).orElse("absent");
    }

    private static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1 << 16];
            for (int n; (n = in.read(buffer)) > 0; ) digest.update(buffer, 0, n);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    /** {@code -Dimmersive_spellcraft.coverage.export=<path>} writes the manifest once the server has started. */
    public static void exportIfRequested(MinecraftServer server) {
        String path = System.getProperty("immersive_spellcraft.coverage.export");
        if (path == null || path.isBlank()) return;
        try {
            Path out = export(server, Path.of(path));
            ImmersiveSpellcraft.LOGGER.info("Coverage manifest written to {}", out.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            ImmersiveSpellcraft.LOGGER.error("Could not write the coverage manifest to {}", path, e);
        }
    }

    private CoverageExporter() {}
}
