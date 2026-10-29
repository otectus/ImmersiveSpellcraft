package com.otectus.immersivespellcraft.compat.ars;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.core.OperationContext;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Right-clicking a Scribe's Table whose glyph awaits its inscription, with an empty main hand, not sneaking and with
 * nothing on the table, begins the inscription. Ars handles table clicks in its own normal-priority event listener,
 * so this runs first at high priority and cancels only that one click; every other use (selecting a glyph with a spell
 * book, adding an ingredient, placing or taking a book, sneak-inscribing) stays native.
 */
public final class ArsEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof ScribesTile clicked)) return;
        ScribesTile tile = clicked.getLogicTile();
        if (tile == null || ((ScribeGateHolder) tile).immersivespellcraft$gateState() != ScribeGate.AWAITING) return;
        if (!player.getMainHandItem().isEmpty() || player.isShiftKeyDown() || !tile.getStack().isEmpty()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        SessionManager.BeginResult result = SessionManager.begin(player, OperationContext.block(player, tile.getBlockPos()), ScribeGate.adapter);
        switch (result.outcome()) {
            case REFUSED -> player.displayClientMessage(result.refusal().message(), true);
            case NATIVE -> {
                // No performance applies to this player (creative bypass, or the operation was just disabled).
                ScribeGateHolder gate = (ScribeGateHolder) tile;
                gate.immersivespellcraft$setGate(ScribeGate.INSCRIBED, gate.immersivespellcraft$gateJob(), gate.immersivespellcraft$gateRecipe());
                tile.updateBlock();
                ImmersiveSpellcraft.debug("scribe job at {} finished natively for {}", tile.getBlockPos().toShortString(), player.getGameProfile().getName());
            }
            default -> {}
        }
    }

    private ArsEvents() {}
}
