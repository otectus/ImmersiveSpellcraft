package com.otectus.immersivespellcraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.core.Treatment;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict parsing of schema version 1. Every problem is collected with its JSON path; a profile with any error is
 * rejected whole, so a malformed file can never half-apply or touch native costs.
 */
public final class ProfileParser {
    private static final Pattern KEY = Pattern.compile("[a-z0-9_.\\-]+");
    private static final Set<String> TOP_LEVEL = Set.of("schema_version", "priority", "adapter", "operation", "selector",
            "treatment", "game", "outcome", "presentation", "comment");
    private static final Set<String> SELECTOR = Set.of("blocks", "items", "recipes", "recipe_types", "namespaces");
    private static final Set<String> GAME = Set.of("type", "min_actions", "max_actions", "target_duration_seconds",
            "max_duration_seconds", "timing_assist_allowed");

    public record Result(OperationProfile profile, List<String> errors) {
        public boolean ok() { return profile != null && errors.isEmpty(); }
    }

    public static Result parse(ResourceLocation id, JsonElement element) {
        List<String> errors = new ArrayList<>();
        if (!element.isJsonObject()) return new Result(null, List.of("the file must contain a JSON object"));
        JsonObject o = element.getAsJsonObject();
        for (String key : o.keySet()) if (!TOP_LEVEL.contains(key)) errors.add("unknown field '" + key + "'");

        int schema = integer(o, "schema_version", -1, errors);
        if (schema != OperationProfile.SCHEMA_VERSION) errors.add("schema_version must be " + OperationProfile.SCHEMA_VERSION);
        int priority = integer(o, "priority", 0, errors);
        ResourceLocation adapter = location(o, "adapter", true, errors);
        ResourceLocation operation = location(o, "operation", true, errors);
        if (adapter != null) {
            Operations.Known known = Operations.known(adapter);
            if (!AdapterRegistry.known(adapter) || known == null) errors.add("adapter: unknown adapter " + adapter);
            else if (operation != null && !known.operation().equals(operation))
                errors.add("operation: adapter " + adapter + " performs " + known.operation() + ", not " + operation);
        }

        Treatment treatment = Treatment.byId(string(o, "treatment", "", errors));
        if (treatment == null) errors.add("treatment must be one of perform, attune, preserve");
        else if (treatment == Treatment.ATTUNE) errors.add("treatment: these adapters perform or preserve; attune is not available for them");

        OperationProfile.Selector selector = selector(o, errors);
        OperationProfile.Game game = null;
        if (o.has("game")) game = game(o.get("game"), errors);
        else if (treatment == Treatment.PERFORM) errors.add("game is required when treatment is perform");

        OperationProfile.Outcome outcome = OperationProfile.Outcome.DEFAULT;
        if (o.has("outcome")) {
            JsonObject out = object(o, "outcome", errors);
            if (out != null) {
                String policy = string(out, "policy", OperationProfile.Outcome.NATIVE_EXACT, errors);
                boolean grade = bool(out, "persistent_grade", false, errors);
                if (!OperationProfile.Outcome.NATIVE_EXACT.equals(policy)) errors.add("outcome.policy must be native_exact");
                if (grade) errors.add("outcome.persistent_grade: glyphs and scrolls are knowledge and consumables; no grade is stored on them");
                outcome = new OperationProfile.Outcome(policy, grade);
            }
        }

        OperationProfile.Presentation presentation = OperationProfile.Presentation.DEFAULT;
        if (o.has("presentation")) {
            JsonObject p = object(o, "presentation", errors);
            if (p != null) {
                ResourceLocation theme = location(p, "theme", false, errors);
                String key = p.has("instruction_key") ? string(p, "instruction_key", "", errors) : null;
                if (key != null && !KEY.matcher(key).matches()) errors.add("presentation.instruction_key must be a translation key");
                presentation = new OperationProfile.Presentation(theme, key);
            }
        }

        if (!errors.isEmpty() || adapter == null || operation == null || treatment == null) return new Result(null, errors);
        return new Result(new OperationProfile(id, schema, priority, adapter, operation, selector, treatment, game, outcome,
                presentation), List.of());
    }

    private static OperationProfile.Selector selector(JsonObject o, List<String> errors) {
        if (!o.has("selector")) return OperationProfile.Selector.ANY;
        JsonObject s = object(o, "selector", errors);
        if (s == null) return OperationProfile.Selector.ANY;
        for (String key : s.keySet()) if (!SELECTOR.contains(key)) errors.add("selector: unknown dimension '" + key + "'");
        return new OperationProfile.Selector(locations(s, "blocks", errors), idsOrTags(s, "items", errors),
                locations(s, "recipes", errors), locations(s, "recipe_types", errors), namespaces(s, errors));
    }

    private static OperationProfile.Game game(JsonElement e, List<String> errors) {
        if (!e.isJsonObject()) {
            errors.add("game must be an object");
            return null;
        }
        JsonObject g = e.getAsJsonObject();
        for (String key : g.keySet()) if (!GAME.contains(key)) errors.add("game: unknown field '" + key + "'");
        String type = string(g, "type", "", errors);
        if (!GameTypes.known(type)) errors.add("game.type: unknown game '" + type + "' (known: " + String.join(", ", GameTypes.ALL) + ")");
        int min = integer(g, "min_actions", 3, errors), max = integer(g, "max_actions", 6, errors);
        if (min < 1 || min > 8) errors.add("game.min_actions must be 1-8");
        if (max < min || max > 8) errors.add("game.max_actions must be min_actions-8");
        double target = number(g, "target_duration_seconds", 8, errors);
        double limit = number(g, "max_duration_seconds", 20, errors);
        if (target < 2 || target > 30) errors.add("game.target_duration_seconds must be 2-30");
        if (limit < target || limit > 60) errors.add("game.max_duration_seconds must be target_duration_seconds-60");
        boolean assist = bool(g, "timing_assist_allowed", true, errors);
        return new OperationProfile.Game(type, min, max, target, limit, assist);
    }

    // ---- primitives --------------------------------------------------------------------------------------------

    private static JsonObject object(JsonObject o, String key, List<String> errors) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonObject()) {
            errors.add(key + " must be an object");
            return null;
        }
        return e.getAsJsonObject();
    }

    private static int integer(JsonObject o, String key, int fallback, List<String> errors) {
        JsonElement e = o.get(key);
        if (e == null) return fallback;
        try {
            double d = e.getAsDouble();
            if (d != Math.rint(d)) throw new NumberFormatException();
            return (int) d;
        } catch (RuntimeException ex) {
            errors.add(key + " must be an integer");
            return fallback;
        }
    }

    private static double number(JsonObject o, String key, double fallback, List<String> errors) {
        JsonElement e = o.get(key);
        if (e == null) return fallback;
        try {
            return e.getAsDouble();
        } catch (RuntimeException ex) {
            errors.add(key + " must be a number");
            return fallback;
        }
    }

    private static boolean bool(JsonObject o, String key, boolean fallback, List<String> errors) {
        JsonElement e = o.get(key);
        if (e == null) return fallback;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) {
            errors.add(key + " must be true or false");
            return fallback;
        }
        return e.getAsBoolean();
    }

    private static String string(JsonObject o, String key, String fallback, List<String> errors) {
        JsonElement e = o.get(key);
        if (e == null) return fallback;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            errors.add(key + " must be a string");
            return fallback;
        }
        return e.getAsString();
    }

    private static ResourceLocation location(JsonObject o, String key, boolean required, List<String> errors) {
        if (!o.has(key)) {
            if (required) errors.add(key + " is required");
            return null;
        }
        ResourceLocation r = ResourceLocation.tryParse(string(o, key, "", errors));
        if (r == null) errors.add(key + " must be a resource id");
        return r;
    }

    private static List<ResourceLocation> locations(JsonObject o, String key, List<String> errors) {
        List<ResourceLocation> out = new ArrayList<>();
        for (String s : strings(o, key, errors)) {
            ResourceLocation r = ResourceLocation.tryParse(s);
            if (r == null || s.startsWith("#")) errors.add("selector." + key + ": '" + s + "' is not a resource id");
            else out.add(r);
        }
        return List.copyOf(out);
    }

    private static List<String> idsOrTags(JsonObject o, String key, List<String> errors) {
        List<String> out = new ArrayList<>();
        for (String s : strings(o, key, errors)) {
            String id = s.startsWith("#") ? s.substring(1) : s;
            if (ResourceLocation.tryParse(id) == null) errors.add("selector." + key + ": '" + s + "' is not an id or #tag");
            else out.add(s.startsWith("#") ? "#" + ResourceLocation.tryParse(id) : String.valueOf(ResourceLocation.tryParse(id)));
        }
        return List.copyOf(out);
    }

    private static List<String> namespaces(JsonObject o, List<String> errors) {
        List<String> out = new ArrayList<>();
        for (String s : strings(o, "namespaces", errors)) {
            if (!ResourceLocation.isValidNamespace(s)) errors.add("selector.namespaces: '" + s + "' is not a namespace");
            else out.add(s);
        }
        return List.copyOf(out);
    }

    private static List<String> strings(JsonObject o, String key, List<String> errors) {
        JsonElement e = o.get(key);
        if (e == null) return List.of();
        if (!e.isJsonArray()) {
            errors.add("selector." + key + " must be a list");
            return List.of();
        }
        List<String> out = new ArrayList<>();
        JsonArray a = e.getAsJsonArray();
        for (JsonElement x : a) {
            if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isString()) out.add(x.getAsString());
            else errors.add("selector." + key + " entries must be strings");
        }
        return out;
    }

    private ProfileParser() {}
}
