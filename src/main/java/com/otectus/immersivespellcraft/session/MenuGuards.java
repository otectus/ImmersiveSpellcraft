package com.otectus.immersivespellcraft.session;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.core.MenuGuard;
import com.otectus.immersivespellcraft.core.StationKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The server-side {@code AbstractContainerMenu.clicked()} guard. It covers pickup, shift-click, number-key and offhand
 * swaps, throws, pick-all, drags and direct container packets. During a player's own performance every click in that
 * menu is refused except the one permitted native commit; at a station another player is performing at, its station
 * slots are refused; and a commit click that requires a performance is refused unless it came through the session.
 */
public final class MenuGuards {
    private static final List<MenuGuard> GUARDS = new CopyOnWriteArrayList<>();

    public static void register(MenuGuard guard) {
        GUARDS.add(guard);
    }

    public static boolean blockClick(ServerPlayer player, AbstractContainerMenu menu, int slot, int button, ClickType type) {
        PerformanceSession own = SessionManager.session(player).orElse(null);
        if (own != null && own.operation.menu == menu) return !SessionManager.permitsClick(player, menu, slot, button, type);
        if (GUARDS.isEmpty()) return false;
        MenuGuard guard = null;
        for (MenuGuard g : GUARDS) if (g.handles(menu)) { guard = g; break; }
        if (guard == null || slot < 0 || slot >= menu.slots.size()) return false;
        try {
            StationKey key = guard.station(menu);
            PerformanceSession other = key == null ? null : SessionManager.atStation(key);
            if (other != null && other.player != player && guard.isStationSlot(menu, slot)) {
                player.displayClientMessage(Component.translatable("immersive_spellcraft.notice.station_in_use",
                        other.player.getDisplayName()), true);
                return true;
            }
            if (guard.isCommitClick(menu, slot, button, type) && guard.requiresPerformance(player, menu)) {
                player.displayClientMessage(Component.translatable("immersive_spellcraft.notice.perform_to_take"), true);
                return true;
            }
        } catch (RuntimeException e) {
            ImmersiveSpellcraft.LOGGER.error("Menu guard failed closed for {}", player.getGameProfile().getName(), e);
            return true;
        }
        return false;
    }

    private MenuGuards() {}
}
