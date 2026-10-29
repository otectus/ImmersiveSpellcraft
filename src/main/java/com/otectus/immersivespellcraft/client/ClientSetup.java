package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Client-only wiring, reached through {@code DistExecutor} from the mod constructor. */
public final class ClientSetup {
    public static void init(IEventBus modBus) {
        ModNetwork.client = new ClientPacketHandler();
        modBus.addListener(Keys::register);
        modBus.addListener((ModConfigEvent.Reloading e) -> {
            if (e.getConfig().getSpec() == ClientConfig.SPEC) ClientEvents.sendPreferences();
        });
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        MinecraftForge.EVENT_BUS.register(PerformanceOverlay.class);
    }

    private ClientSetup() {}
}
