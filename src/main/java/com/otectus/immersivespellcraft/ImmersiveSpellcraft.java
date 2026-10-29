package com.otectus.immersivespellcraft;

import com.mojang.logging.LogUtils;
import com.otectus.immersivespellcraft.compat.CompatibilityBootstrap;
import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.config.ServerConfig;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.session.SessionEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ImmersiveSpellcraft.MOD_ID)
public class ImmersiveSpellcraft {
    public static final String MOD_ID = "immersive_spellcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ImmersiveSpellcraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modBus.addListener(ServerConfig::onConfigChanged);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);
        MinecraftForge.EVENT_BUS.register(SessionEvents.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.otectus.immersivespellcraft.client.ClientSetup.init(modBus));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            CompatibilityBootstrap.init();
        });
    }

    private void loadComplete(final FMLLoadCompleteEvent event) {
        AdapterRegistry.freeze();
    }

    @SuppressWarnings("removal")
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /** Lifecycle evidence: INFO when the server's debugLogging is on, DEBUG otherwise. */
    public static void debug(String message, Object... args) {
        if (ServerConfig.debugLogging()) LOGGER.info("[spellcraft] " + message, args);
        else LOGGER.debug("[spellcraft] " + message, args);
    }
}
