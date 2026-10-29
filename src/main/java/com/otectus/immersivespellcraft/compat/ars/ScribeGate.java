package com.otectus.immersivespellcraft.compat.ars;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.config.OperationSwitches;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Scribe's Table gate, run at the head of the master tile's own tick. The native job is untouched: Ars chooses the
 * recipe, charges XP at selection, collects the ingredients and runs its 120-tick crafting window. Only the window's
 * final tick, the one that drops the glyph, is held until the job's inscription is complete. A replaced or refunded
 * job resets the gate; turning the operation off releases a held job so it finishes natively.
 */
public final class ScribeGate {
    public static final int NONE = 0, AWAITING = 1, INSCRIBED = 2, EXEMPT = 3;
    static ScribeGlyphAdapter adapter;

    public static void beforeTick(ScribesTile tile) {
        if (tile.getLevel() == null || adapter == null || !tile.isMasterTile()) return;
        ScribeGateHolder gate = (ScribeGateHolder) tile;
        int state = gate.immersivespellcraft$gateState();
        if (!tile.crafting || tile.recipe == null) {
            if (state != NONE) gate.immersivespellcraft$setGate(NONE, 0, "");
            return;
        }
        if (tile.getLevel().isClientSide()) {
            // Mirror the hold so the client keeps showing the waiting glyph; the server alone decides when it drops.
            if (state == AWAITING && tile.craftingTicks <= 1) tile.craftingTicks = 2;
            return;
        }
        String recipe = tile.recipe.getId().toString();
        if (state != NONE && !recipe.equals(gate.immersivespellcraft$gateRecipe())) state = NONE;
        switch (state) {
            case NONE -> {
                boolean required = SessionManager.requiresPerformance(null, adapter, adapter.query(tile));
                gate.immersivespellcraft$setGate(required ? AWAITING : EXEMPT, ThreadLocalRandom.current().nextLong(), recipe);
                tile.updateBlock();
                if (required) announce(tile);
                ImmersiveSpellcraft.debug("scribe job {} at {}: {}", recipe, tile.getBlockPos().toShortString(), required ? "awaiting inscription" : "native");
            }
            case AWAITING -> {
                if (!OperationSwitches.enabled(adapter.id())) {
                    gate.immersivespellcraft$setGate(EXEMPT, gate.immersivespellcraft$gateJob(), recipe);
                    tile.updateBlock();
                    return;
                }
                // Hold the native window on its last tick: the native decrement then leaves 1, never 0.
                if (tile.craftingTicks <= 1) tile.craftingTicks = 2;
            }
            default -> {}
        }
    }

    private static void announce(ScribesTile tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) return;
        Component message = Component.translatable("immersive_spellcraft.notice.scribe_ready", tile.recipe.output.getHoverName());
        for (ServerPlayer p : level.players())
            if (p.distanceToSqr(tile.getBlockPos().getCenter()) <= 64) p.displayClientMessage(message, true);
    }

    public static void tooltip(ScribesTile tile, List<Component> tooltip) {
        if (!tile.isMasterTile()) return;
        int state = ((ScribeGateHolder) tile).immersivespellcraft$gateState();
        if (state == AWAITING && tile.crafting)
            tooltip.add(Component.translatable("immersive_spellcraft.tooltip.scribe_awaiting").withStyle(ChatFormatting.LIGHT_PURPLE));
        else if (state == INSCRIBED && tile.crafting)
            tooltip.add(Component.translatable("immersive_spellcraft.tooltip.scribe_inscribed").withStyle(ChatFormatting.AQUA));
    }

    private ScribeGate() {}
}
