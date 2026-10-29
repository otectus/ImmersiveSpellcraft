package com.otectus.immersivespellcraft.compat.ars;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import com.otectus.immersivespellcraft.config.ServerConfig;
import com.otectus.immersivespellcraft.core.AdapterCapability;
import com.otectus.immersivespellcraft.core.Admission;
import com.otectus.immersivespellcraft.core.CommitResult;
import com.otectus.immersivespellcraft.core.Inspection;
import com.otectus.immersivespellcraft.core.InterruptionReason;
import com.otectus.immersivespellcraft.core.InterruptionResult;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.OperationContext;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.core.PreparedOperation;
import com.otectus.immersivespellcraft.core.ProfileQuery;
import com.otectus.immersivespellcraft.core.Refusal;
import com.otectus.immersivespellcraft.core.StationKey;
import com.otectus.immersivespellcraft.core.Validation;
import com.otectus.immersivespellcraft.data.ProfileManager;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sigil Inscription for glyph crafting at the Scribe's Table (A01). An existing-native-job adapter: the job, its XP
 * charge and its ingredients belong to Ars; committing marks the job inscribed, and the table's own tick then drops
 * the glyph exactly once. The station key is always the master (head) half, whichever half was clicked.
 */
public final class ScribeGlyphAdapter implements MagicOperationAdapter {
    public static final int VERSION = 1;
    private static final ResourceLocation BLOCK = new ResourceLocation(Operations.ARS, "scribes_table");

    record Snapshot(ScribesTile tile, long job, String recipe, ItemStack output, int consumed) {}

    @Override public ResourceLocation id() { return Operations.ARS_SCRIBE_ADAPTER; }
    @Override public int version() { return VERSION; }
    @Override public ResourceLocation operation() { return Operations.ARS_GLYPH_CRAFT; }
    @Override public String integration() { return Operations.ARS; }
    @Override public AdapterCapability capability() { return AdapterCapability.EXISTING_NATIVE_JOB; }
    @Override public String defaultGame() { return GameTypes.SIGIL_INSCRIPTION; }
    @Override public boolean acceptsBlock(BlockEntity blockEntity) { return blockEntity instanceof ScribesTile; }

    ProfileQuery query(ScribesTile tile) {
        GlyphRecipe recipe = tile.recipe;
        List<ItemStack> items = new ArrayList<>();
        if (recipe != null) items.add(recipe.output);
        items.addAll(tile.consumedStacks);
        ResourceLocation type = ForgeRegistries.RECIPE_TYPES.getKey(RecipeRegistry.GLYPH_TYPE.get());
        return new ProfileQuery(operation(), id(), BLOCK, recipe == null ? null : recipe.getId(), type, items);
    }

    @Nullable
    private static ScribesTile logic(OperationContext context) {
        if (context.pos() == null || !(context.level().getBlockEntity(context.pos()) instanceof ScribesTile tile)) return null;
        return tile.getLogicTile();
    }

    @Override
    public Inspection inspect(OperationContext context) {
        ScribesTile tile = logic(context);
        if (tile == null) return Inspection.NOT_APPLICABLE;
        ProfileQuery query = query(tile);
        ScribeGateHolder gate = (ScribeGateHolder) tile;
        if (tile.recipe == null || !tile.crafting || gate.immersivespellcraft$gateState() != ScribeGate.AWAITING)
            return Inspection.refused(Refusal.of("nothing_to_inscribe"), query);
        if (!inReach(context.player(), tile.getBlockPos())) return Inspection.refused(Refusal.of("too_far"), query);
        return Inspection.eligible(query);
    }

    private static boolean inReach(ServerPlayer player, BlockPos pos) {
        double reach = ServerConfig.get(ServerConfig.REACH);
        return player.distanceToSqr(pos.getCenter()) <= reach * reach;
    }

    /** 0..1 from the glyph's Ars tier (I-III) and how many ingredients its recipe takes. */
    private static double complexity(GlyphRecipe recipe) {
        int tier = recipe.output.getItem() instanceof Glyph glyph && glyph.spellPart != null && glyph.spellPart.getConfigTier() != null
                ? glyph.spellPart.getConfigTier().value : 1;
        double t = Math.max(0, Math.min(2, tier - 1)) / 2.0;
        double inputs = Math.min(9, recipe.inputs.size()) / 9.0;
        return 0.6 * t + 0.4 * inputs;
    }

    @Override
    public Admission begin(OperationContext context, Inspection inspection) {
        ScribesTile tile = logic(context);
        if (tile == null || tile.recipe == null) return Admission.refused(Refusal.of("nothing_to_inscribe"));
        ScribeGateHolder gate = (ScribeGateHolder) tile;
        GlyphRecipe recipe = tile.recipe;
        List<Component> costs = List.of(
                Component.translatable("immersive_spellcraft.cost.xp_paid", recipe.exp),
                Component.translatable("immersive_spellcraft.cost.ingredients_held", tile.consumedStacks.size()),
                Component.translatable("immersive_spellcraft.cost.learn_separately"));
        StationKey station = new StationKey(context.level().dimension(), tile.getBlockPos());
        PreparedOperation op = PreparedOperation.builder(this, context.player().getUUID(), station)
                .selection(recipe.getId().toString())
                .generations(ProfileManager.generation(), ServerConfig.revision())
                .nativeJob(Long.toHexString(gate.immersivespellcraft$gateJob()))
                .inputs(List.copyOf(tile.consumedStacks))
                .output(recipe.output)
                .title(Component.translatable("immersive_spellcraft.operation.ars_scribe.title", recipe.output.getHoverName()))
                .costs(costs)
                .complexity(complexity(recipe))
                .payload(new Snapshot(tile, gate.immersivespellcraft$gateJob(), recipe.getId().toString(), recipe.output.copy(),
                        tile.consumedStacks.size()))
                .build();
        return Admission.admitted(op);
    }

    @Override
    public Validation validate(PreparedOperation op, ServerPlayer player) {
        Snapshot s = (Snapshot) op.payload;
        ScribesTile tile = s.tile();
        if (tile.isRemoved() || tile.getLevel() == null || tile.getLevel().getBlockEntity(tile.getBlockPos()) != tile)
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("station_changed"));
        ScribeGateHolder gate = (ScribeGateHolder) tile;
        if (gate.immersivespellcraft$gateState() != ScribeGate.AWAITING || gate.immersivespellcraft$gateJob() != s.job()
                || tile.recipe == null || !tile.recipe.getId().toString().equals(s.recipe()) || !tile.crafting)
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("job_changed"));
        if (!player.isAlive() || player.level() != tile.getLevel()) return Validation.invalid(InterruptionReason.DIMENSION_CHANGED, Refusal.of("changed"));
        if (!inReach(player, tile.getBlockPos())) return Validation.invalid(InterruptionReason.OUT_OF_REACH, Refusal.of("too_far"));
        return Validation.OK;
    }

    @Override
    public CommitResult commit(PreparedOperation op, ServerPlayer player, ScoreCard performance) {
        Snapshot s = (Snapshot) op.payload;
        ScribeGateHolder gate = (ScribeGateHolder) s.tile();
        gate.immersivespellcraft$setGate(ScribeGate.INSCRIBED, s.job(), s.recipe());
        s.tile().setChanged();
        s.tile().updateBlock();
        return CommitResult.committed(Component.translatable("immersive_spellcraft.result.ars_scribe", s.output().getHoverName()));
    }

    @Override
    public InterruptionResult interrupt(PreparedOperation op, InterruptionReason reason) {
        if (reason == InterruptionReason.STATION_CHANGED || reason == InterruptionReason.SERVER_STOPPING) return InterruptionResult.RELEASED;
        return new InterruptionResult(Component.translatable("immersive_spellcraft.note.scribe_waits"));
    }

    @Override
    public Component releaseJob(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ScribesTile clicked)) return null;
        ScribesTile tile = clicked.getLogicTile();
        if (tile == null) return null;
        ScribeGateHolder gate = (ScribeGateHolder) tile;
        if (gate.immersivespellcraft$gateState() != ScribeGate.AWAITING) return null;
        gate.immersivespellcraft$setGate(ScribeGate.INSCRIBED, gate.immersivespellcraft$gateJob(), gate.immersivespellcraft$gateRecipe());
        tile.updateBlock();
        return Component.translatable("immersive_spellcraft.command.release.done", pos.toShortString());
    }
}
