package com.otectus.immersivespellcraft.config;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/**
 * Gameplay settings. A Forge SERVER config: it lives in each world's {@code serverconfig/} folder (a dedicated server's
 * {@code world/serverconfig/immersive_spellcraft-server.toml}), copies from {@code defaultconfigs/} when a world is
 * created, and is synced to every client on join, so clients read the same switches when deciding what to intercept.
 * It is per world, not global. Every read falls back to its default while no world is loaded.
 */
public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_BYPASS;
    public static final ForgeConfigSpec.BooleanValue ARS_NOUVEAU;
    public static final ForgeConfigSpec.BooleanValue IRONS_SPELLBOOKS;
    public static final ForgeConfigSpec.BooleanValue ARS_N_SPELLS;
    public static final ForgeConfigSpec.BooleanValue ARS_GLYPH_CRAFTING;
    public static final ForgeConfigSpec.BooleanValue IRONS_SCROLL_FORGE;
    public static final ForgeConfigSpec.BooleanValue ANS_GLYPH_PRESS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_RECIPES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_OUTPUTS;
    public static final ForgeConfigSpec.DoubleValue DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WINDOW_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue MAX_ACTIONS;
    public static final ForgeConfigSpec.DoubleValue MAX_CLIENT_TIMING_ASSIST;
    public static final ForgeConfigSpec.BooleanValue ALLOW_UNTIMED;
    public static final ForgeConfigSpec.DoubleValue REACH;
    public static final ForgeConfigSpec.IntValue MAX_CONCURRENT_SESSIONS;
    public static final ForgeConfigSpec.IntValue MAX_INPUT_EVENTS_PER_SECOND;
    public static final ForgeConfigSpec.IntValue IDLE_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.IntValue START_COOLDOWN_MILLIS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    private static volatile int revision;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        ENABLED = b.comment("Master switch. When false every station keeps its native behavior.").define("enabled", true);
        CREATIVE_BYPASS = b.comment("Creative-mode players complete operations natively, without a performance.")
                .define("creativeBypass", false);
        b.pop();

        b.comment("Integrations. Each activates only when its mod is installed in a supported version.").push("integrations");
        ARS_NOUVEAU = b.comment("Ars Nouveau 4.12.x stations.").define("arsNouveau", true);
        IRONS_SPELLBOOKS = b.comment("Iron's Spells 'n Spellbooks 3.15.x / 3.16.x stations.").define("ironsSpellbooks", true);
        ARS_N_SPELLS = b.comment("Ars 'n Spells 3.4.x stations.").define("arsNSpells", true);
        b.pop();

        b.comment("Operations. A disabled operation, excluded recipe or excluded output keeps its exact native behavior.",
                "Changing these leaves running native jobs intact: a job already waiting for a performance finishes natively",
                "once its operation is disabled.").push("operations");
        ARS_GLYPH_CRAFTING = b.comment("Sigil Inscription for glyph crafting at the Ars Nouveau Scribe's Table.")
                .define("arsGlyphCrafting", true);
        IRONS_SCROLL_FORGE = b.comment("Scroll Forging for taking a scroll from the Iron's Scroll Forge.").define("ironsScrollForge", true);
        ANS_GLYPH_PRESS = b.comment("Glyph Embossing for Ars 'n Spells Glyph Press jobs.").define("ansGlyphPress", true);
        EXCLUDED_RECIPES = b.comment("Recipe ids that keep native behavior, e.g. \"ars_nouveau:glyph_projectile\".")
                .defineListAllowEmpty("excludedRecipes", List.of(), ServerConfig::isIdOrTag);
        EXCLUDED_OUTPUTS = b.comment("Output item ids or #tags that keep native behavior, e.g. \"irons_spellbooks:scroll\".")
                .defineListAllowEmpty("excludedOutputs", List.of(), ServerConfig::isIdOrTag);
        b.pop();

        b.comment("Difficulty. Bounded and predictable: profiles set the base, these scale every performance.").push("difficulty");
        DURATION_MULTIPLIER = b.comment("Multiplies each profile's target and maximum durations.")
                .defineInRange("durationMultiplier", 1.0, 0.5, 2.0);
        WINDOW_MULTIPLIER = b.comment("Multiplies timing windows, bands and alignment tolerances. Above 1 is more forgiving.")
                .defineInRange("windowMultiplier", 1.0, 0.5, 2.0);
        MAX_ACTIONS = b.comment("Caps the number of meaningful actions (strokes) a profile may ask for.")
                .defineInRange("maxActions", 6, 1, 8);
        b.pop();

        b.comment("Accessibility the server allows. Players choose within these limits in their client settings.").push("accessibility");
        MAX_CLIENT_TIMING_ASSIST = b.comment("Largest window widening a player may request. 1.0 gives every player identical windows.")
                .defineInRange("maxClientTimingAssist", 1.5, 1.0, 2.0);
        ALLOW_UNTIMED = b.comment("Allow players to choose untimed mode: markers wait on their marks and nothing expires.",
                "Untimed performances make exactly the same native result.").define("allowUntimedMode", true);
        b.pop();

        b.comment("Multiplayer limits").push("multiplayer");
        REACH = b.comment("How far (blocks) a player may be from a block station while performing at it.")
                .defineInRange("reach", 6.0, 2.0, 16.0);
        MAX_CONCURRENT_SESSIONS = b.comment("Most performances running at once on this server.")
                .defineInRange("maxConcurrentSessions", 64, 1, 1024);
        MAX_INPUT_EVENTS_PER_SECOND = b.comment("Input events per second a client may send before its session is ended.")
                .defineInRange("maxInputEventsPerSecond", 120, 20, 1000);
        IDLE_TIMEOUT_SECONDS = b.comment("A performance with no input for this long is released (nothing is lost).")
                .defineInRange("idleTimeoutSeconds", 30, 5, 600);
        START_COOLDOWN_MILLIS = b.comment("Minimum time between two start requests from one player.")
                .defineInRange("startCooldownMillis", 250, 0, 5000);
        b.pop();

        b.comment("Debugging").push("debugging");
        DEBUG_LOGGING = b.comment("Log adapter choice, profile resolution and every session transition at INFO.",
                "Never logs inventory contents beyond the operation's own items.").define("debugLogging", false);
        b.pop();
        SPEC = b.build();
    }

    private static boolean isIdOrTag(Object o) {
        if (!(o instanceof String s) || s.isEmpty()) return false;
        String id = s.startsWith("#") ? s.substring(1) : s;
        return net.minecraft.resources.ResourceLocation.isValidResourceLocation(id);
    }

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            revision++;
            ImmersiveSpellcraft.debug("server config {} (revision {})", event instanceof ModConfigEvent.Loading ? "loaded" : "reloaded", revision);
        }
    }

    /** Increments on every load or reload; running sessions planned under another revision are released. */
    public static int revision() {
        return revision;
    }

    public static boolean get(ForgeConfigSpec.BooleanValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static double get(ForgeConfigSpec.DoubleValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static int get(ForgeConfigSpec.IntValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static List<? extends String> get(ForgeConfigSpec.ConfigValue<List<? extends String>> v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static boolean enabled() { return get(ENABLED); }
    public static boolean debugLogging() { return get(DEBUG_LOGGING); }

    private ServerConfig() {}
}
