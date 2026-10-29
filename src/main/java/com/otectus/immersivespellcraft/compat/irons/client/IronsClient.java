package com.otectus.immersivespellcraft.compat.irons.client;

import com.otectus.immersivespellcraft.client.MenuIntercepts;
import com.otectus.immersivespellcraft.config.ServerConfig;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import java.util.EnumSet;
import java.util.Set;

/** Client side of Scroll Forging: a click that would take the forge's result begins the performance instead. */
public final class IronsClient {
    private static final Set<ClickType> COMMIT = EnumSet.of(ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.SWAP, ClickType.THROW);

    public static void init() {
        MenuIntercepts.register(new MenuIntercepts.Intercept() {
            @Override public boolean handles(AbstractContainerMenu menu) { return menu instanceof ScrollForgeMenu; }

            @Override
            public boolean isCommitClick(AbstractContainerMenu menu, int slot, int button, ClickType type) {
                ScrollForgeMenu m = (ScrollForgeMenu) menu;
                return slot == m.getResultSlot().index && COMMIT.contains(type) && m.getResultSlot().hasItem();
            }

            @Override
            public boolean gated(LocalPlayer player) {
                return ServerConfig.enabled() && ServerConfig.get(ServerConfig.IRONS_SPELLBOOKS) && ServerConfig.get(ServerConfig.IRONS_SCROLL_FORGE)
                        && !(player.isCreative() && ServerConfig.get(ServerConfig.CREATIVE_BYPASS));
            }
        });
    }

    private IronsClient() {}
}
