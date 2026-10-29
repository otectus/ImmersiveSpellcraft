package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Client ticking and connection lifecycle. */
public final class ClientEvents {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) ClientSession.clientTick();
    }

    @SubscribeEvent
    public static void loggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        sendPreferences();
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientSession.clearAll();
    }

    /** The accessibility requests the server caps: timing assistance and untimed mode. */
    public static void sendPreferences() {
        if (net.minecraft.client.Minecraft.getInstance().getConnection() == null) return;
        ModNetwork.sendToServer(new Packets.Preferences((float) ClientConfig.get(ClientConfig.TIMING_ASSIST),
                ClientConfig.get(ClientConfig.UNTIMED_MODE)));
    }

    private ClientEvents() {}
}
