package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client half of the menu gate: a click that would take a gated native result is sent as a begin request carrying
 * that exact click instead, and the server decides. If no performance applies, the server performs the click itself,
 * exactly as the station would have; if one does, the panel opens over the station screen.
 */
public final class MenuIntercepts {
    /** Registered by an integration's client setup for its station menu. */
    public interface Intercept {
        boolean handles(AbstractContainerMenu menu);

        boolean isCommitClick(AbstractContainerMenu menu, int slot, int button, ClickType type);

        /** The synced server switches still gate this station for this player (cheap; the server decides finally). */
        boolean gated(LocalPlayer player);
    }

    private static final List<Intercept> INTERCEPTS = new CopyOnWriteArrayList<>();

    public static void register(Intercept intercept) {
        INTERCEPTS.add(intercept);
    }

    public static boolean intercept(int container, int slot, int button, ClickType type) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != container) return false;
        if (ClientSession.lockedMenu(container)) return true;
        for (Intercept i : INTERCEPTS) {
            if (!i.handles(menu)) continue;
            if (slot < 0 || slot >= menu.slots.size() || !i.isCommitClick(menu, slot, button, type) || !i.gated(player)) return false;
            ModNetwork.sendToServer(new Packets.Begin(container, menu.getStateId(), slot, button, type.ordinal()));
            return true;
        }
        return false;
    }

    private MenuIntercepts() {}
}
