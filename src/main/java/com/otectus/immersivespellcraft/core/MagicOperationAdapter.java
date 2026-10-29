package com.otectus.immersivespellcraft.core;

import com.otectus.immersivespellcraft.minigame.ScoreCard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * One native operation's integration. Immersive Spellcraft owns the performance session; the adapter's target mod
 * owns the operation. The commit invokes the native transition exactly once and never builds a parallel result.
 *
 * <p>All methods run on the server thread.
 */
public interface MagicOperationAdapter {
    /** e.g. {@code immersive_spellcraft:ars_scribe}. */
    ResourceLocation id();

    /** Bumped whenever the adapter's admission, validation or commit semantics change. */
    int version();

    /** The operation this adapter performs, e.g. {@code immersive_spellcraft:ars/glyph_craft}. */
    ResourceLocation operation();

    /** The integration it belongs to: the target mod id. */
    String integration();

    AdapterCapability capability();

    /** The default game for this operation when no profile names one. */
    String defaultGame();

    /** Whether this adapter handles operations started from {@code menu}. */
    default boolean acceptsMenu(AbstractContainerMenu menu) { return false; }

    /** Whether this adapter handles operations started by using {@code blockEntity}. */
    default boolean acceptsBlock(BlockEntity blockEntity) { return false; }

    /** Called when a player opens a menu this adapter accepts, e.g. to tell them a job waits for their performance. */
    default void menuOpened(ServerPlayer player, AbstractContainerMenu menu) {}

    /**
     * Operator recovery for a native job this adapter holds at {@code pos}: lets it finish natively, once, without a
     * performance. Returns what was done, or null when there is no held job there. Never moves an item itself.
     */
    default net.minecraft.network.chat.Component releaseJob(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return null;
    }

    /** Read-only. No XP drain, recipe roll, item mutation or cast. */
    Inspection inspect(OperationContext context);

    /** Admission: recheck, then claim only resources this operation really owns. */
    Admission begin(OperationContext context, Inspection inspection);

    /** Recheck native validity, ownership and resource identity. */
    Validation validate(PreparedOperation operation, ServerPlayer player);

    /** Invoke the native transition once. Runs under the session manager's scoped one-use permit. */
    CommitResult commit(PreparedOperation operation, ServerPlayer player, ScoreCard performance);

    /** Release a claim or hand control back without undoing anything native. */
    InterruptionResult interrupt(PreparedOperation operation, InterruptionReason reason);
}
