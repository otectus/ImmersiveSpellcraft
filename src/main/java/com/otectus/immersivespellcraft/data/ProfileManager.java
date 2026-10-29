package com.otectus.immersivespellcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.core.ProfileQuery;
import com.otectus.immersivespellcraft.core.Treatment;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads operation profiles as one validated snapshot per reload. A file that fails validation is reported and
 * skipped; if an earlier reload had a valid profile under the same id, that earlier version stays in force, so a
 * broken edit never silently changes or disables a working profile. Every applied snapshot gets a new generation:
 * running sessions planned under an older generation are released before they can commit.
 *
 * <p>Resolution order for an operation: exact recipe override, exact item/block override, tag or recipe-type
 * profile, namespace fallback, then the adapter's built-in generic profile. Ties go to the higher priority, then to
 * the lexically smaller resource id.
 */
public final class ProfileManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "immersive_spellcraft/operation_profiles";
    private static final Gson GSON = new GsonBuilder().create();
    private static final ProfileManager INSTANCE = new ProfileManager();

    private static volatile Snapshot snapshot = new Snapshot(Map.of(), List.of(), 0);

    public record Snapshot(Map<ResourceLocation, OperationProfile> profiles, List<String> problems, int generation) {}

    private ProfileManager() {
        super(GSON, DIRECTORY);
    }

    public static ProfileManager instance() {
        return INSTANCE;
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static int generation() {
        return snapshot.generation();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Snapshot previous = snapshot;
        Map<ResourceLocation, OperationProfile> next = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        files.keySet().stream().sorted().forEach(id -> {
            ProfileParser.Result r;
            try {
                r = ProfileParser.parse(id, files.get(id));
            } catch (RuntimeException e) {
                r = new ProfileParser.Result(null, List.of("could not be read: " + e));
            }
            if (r.ok()) {
                next.put(id, r.profile());
                return;
            }
            OperationProfile old = previous.profiles().get(id);
            String kept = old != null ? " The previous valid version stays in force." : " It is ignored.";
            String message = "Operation profile " + id + " is invalid: " + String.join("; ", r.errors()) + "." + kept;
            problems.add(message);
            ImmersiveSpellcraft.LOGGER.error(message);
            if (old != null) next.put(id, old);
        });
        snapshot = new Snapshot(Map.copyOf(next), List.copyOf(problems), previous.generation() + 1);
        ImmersiveSpellcraft.LOGGER.info("Loaded {} operation profile(s), {} problem(s), generation {}", next.size(),
                problems.size(), snapshot.generation());
    }

    /** The profile for one operation. Never empty for a registered adapter: the built-in generic profile is last. */
    public static ResolvedProfile resolve(MagicOperationAdapter adapter, ProfileQuery query) {
        Snapshot s = snapshot;
        OperationProfile best = s.profiles().values().stream()
                .filter(p -> p.adapter().equals(adapter.id()) && p.operation().equals(adapter.operation()) && p.selector().matches(query))
                .min(Comparator.comparingInt((OperationProfile p) -> -p.selector().specificity())
                        .thenComparingInt(p -> -p.priority())
                        .thenComparing(p -> p.id().toString()))
                .orElse(null);
        if (best == null) {
            OperationProfile generic = generic(adapter);
            return new ResolvedProfile(generic, "built-in generic profile for " + adapter.id(), s.generation());
        }
        String[] ranks = {"generic", "namespace fallback", "tag or recipe-type profile", "exact item or block override",
                "exact recipe override"};
        return new ResolvedProfile(best, ranks[best.selector().specificity()] + " " + best.id() + " (priority " + best.priority() + ")",
                s.generation());
    }

    /** The adapter fallback, identical to the shipped default profile for its operation. */
    public static OperationProfile generic(MagicOperationAdapter adapter) {
        String game = adapter.defaultGame();
        double target = GameTypes.GLYPH_EMBOSSING.equals(game) ? 7 : 8;
        OperationProfile.Game g = new OperationProfile.Game(game, 3, 6, target, 20, true);
        return new OperationProfile(ImmersiveSpellcraft.id("generic/" + adapter.id().getPath()), OperationProfile.SCHEMA_VERSION,
                Integer.MIN_VALUE, adapter.id(), adapter.operation(), OperationProfile.Selector.ANY, Treatment.PERFORM, g,
                OperationProfile.Outcome.DEFAULT, OperationProfile.Presentation.DEFAULT);
    }

    /** Profiles naming an adapter that is dormant here, for diagnostics. */
    public static List<OperationProfile> dormantProfiles() {
        return snapshot.profiles().values().stream()
                .filter(p -> com.otectus.immersivespellcraft.core.AdapterRegistry.byId(p.adapter()).isEmpty())
                .toList();
    }

    public static boolean knownAdapter(ResourceLocation id) {
        return Operations.known(id) != null;
    }
}
