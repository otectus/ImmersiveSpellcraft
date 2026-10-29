package com.otectus.immersivespellcraft.data;

import com.otectus.immersivespellcraft.core.ProfileQuery;
import com.otectus.immersivespellcraft.core.Treatment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A validated operation profile ({@code data/<namespace>/immersive_spellcraft/operation_profiles/<name>.json}). It
 * controls the performance only: which game, its bounds and presentation, or that the operation stays native. It
 * never carries recipe economics; costs are always read from the native plan.
 */
public record OperationProfile(ResourceLocation id, int schemaVersion, int priority, ResourceLocation adapter,
                               ResourceLocation operation, Selector selector, Treatment treatment, @Nullable Game game,
                               Outcome outcome, Presentation presentation) {
    public static final int SCHEMA_VERSION = 1;

    /**
     * Selector dimensions. Every specified dimension must match (AND); within one dimension any entry may match (OR).
     * An empty dimension matches everything. Item entries are ids or {@code #tags}, tested against every item the
     * operation involves (its output first).
     */
    public record Selector(List<ResourceLocation> blocks, List<String> items, List<ResourceLocation> recipes,
                           List<ResourceLocation> recipeTypes, List<String> namespaces) {
        public static final Selector ANY = new Selector(List.of(), List.of(), List.of(), List.of(), List.of());

        public boolean matches(ProfileQuery q) {
            if (!blocks.isEmpty() && (q.block() == null || !blocks.contains(q.block()))) return false;
            if (!recipes.isEmpty() && (q.recipe() == null || !recipes.contains(q.recipe()))) return false;
            if (!recipeTypes.isEmpty() && (q.recipeType() == null || !recipeTypes.contains(q.recipeType()))) return false;
            if (!items.isEmpty() && q.items().stream().noneMatch(s -> items.stream().anyMatch(e -> itemMatches(e, s)))) return false;
            if (!namespaces.isEmpty()) {
                String ns = q.recipe() != null ? q.recipe().getNamespace()
                        : q.items().isEmpty() ? null : key(q.items().get(0)).getNamespace();
                if (ns == null || !namespaces.contains(ns)) return false;
            }
            return true;
        }

        /**
         * The precedence rank: exact recipe override 4, exact item or block override 3, tag or recipe-type profile 2,
         * namespace fallback 1, generic 0. The most specific matching dimension decides.
         */
        public int specificity() {
            if (!recipes.isEmpty()) return 4;
            if (items.stream().anyMatch(e -> !e.startsWith("#")) || !blocks.isEmpty()) return 3;
            if (!items.isEmpty() || !recipeTypes.isEmpty()) return 2;
            if (!namespaces.isEmpty()) return 1;
            return 0;
        }

        private static boolean itemMatches(String entry, ItemStack stack) {
            if (entry.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
                return tag != null && stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag));
            }
            return entry.equals(key(stack).toString());
        }

        private static ResourceLocation key(ItemStack stack) {
            ResourceLocation k = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return k == null ? new ResourceLocation("minecraft", "air") : k;
        }
    }

    public record Game(String type, int minActions, int maxActions, double targetDurationSeconds,
                       double maxDurationSeconds, boolean timingAssistAllowed) {}

    public record Outcome(String policy, boolean persistentGrade) {
        public static final String NATIVE_EXACT = "native_exact";
        public static final Outcome DEFAULT = new Outcome(NATIVE_EXACT, false);
    }

    public record Presentation(@Nullable ResourceLocation theme, @Nullable String instructionKey) {
        public static final Presentation DEFAULT = new Presentation(null, null);
    }

    public boolean performs() {
        return treatment == Treatment.PERFORM && game != null;
    }
}
