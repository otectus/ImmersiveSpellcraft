package com.otectus.immersivespellcraft.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.Nullable;

/**
 * For adapters whose native commit is a menu click (taking a result). The server-side {@code clicked()} guard asks
 * the guard which clicks commit the operation, so every route to the native take (pickup, shift-click, number-key
 * swap, drop, direct packets) is gated, and which slots belong to the station, so a second viewer cannot change a
 * performing player's inputs.
 */
public interface MenuGuard {
    boolean handles(AbstractContainerMenu menu);

    /** Whether this click would perform the native operation's commit (e.g. take the forge's result). */
    boolean isCommitClick(AbstractContainerMenu menu, int slot, int button, ClickType type);

    /** Whether that commit currently requires a performance for this player. Fails closed on error. */
    boolean requiresPerformance(ServerPlayer player, AbstractContainerMenu menu);

    @Nullable StationKey station(AbstractContainerMenu menu);

    /** Whether {@code slot} is one of the station's own slots rather than the player's inventory. */
    boolean isStationSlot(AbstractContainerMenu menu, int slot);
}
