package com.otectus.immersivespellcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * What the server knows about a start request. Everything else is resolved by the adapter from the world: a client
 * only ever names a menu (by id and revision) or a block it clicked, never a recipe, cost or result.
 */
public record OperationContext(ServerPlayer player, ServerLevel level, @Nullable BlockPos pos,
                               @Nullable AbstractContainerMenu menu, @Nullable ClickRequest click) {

    public static OperationContext block(ServerPlayer player, BlockPos pos) {
        return new OperationContext(player, player.serverLevel(), pos, null, null);
    }

    public static OperationContext menu(ServerPlayer player, AbstractContainerMenu menu, @Nullable ClickRequest click) {
        return new OperationContext(player, player.serverLevel(), null, menu, click);
    }
}
