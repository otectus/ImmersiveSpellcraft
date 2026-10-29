package com.otectus.immersivespellcraft.session;

import com.otectus.immersivespellcraft.command.SpellcraftCommands;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.InterruptionReason;
import com.otectus.immersivespellcraft.data.ProfileManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge-bus wiring for sessions: ticking, every interruption path, reload listeners and commands. */
public final class SessionEvents {

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) SessionManager.tick();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) SessionManager.logout(p);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) SessionManager.interrupt(p, InterruptionReason.DIED);
    }

    /** An attack ends a performance at once (nothing is lost), so a player is never pinned to a panel in danger. */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getSource().getEntity() != null && event.getAmount() > 0)
            SessionManager.interrupt(p, InterruptionReason.ATTACKED);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) SessionManager.interrupt(p, InterruptionReason.DIMENSION_CHANGED);
    }

    @SubscribeEvent
    public static void menuOpened(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer p)
            AdapterRegistry.forMenu(event.getContainer()).ifPresent(a -> a.menuOpened(p, event.getContainer()));
    }

    @SubscribeEvent
    public static void menuClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer p) SessionManager.menuClosed(p, event.getContainer());
    }

    @SubscribeEvent
    public static void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        com.otectus.immersivespellcraft.data.CoverageExporter.exportIfRequested(event.getServer());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        SessionManager.clear();
    }

    @SubscribeEvent
    public static void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(ProfileManager.instance());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        SpellcraftCommands.register(event.getDispatcher());
    }

    private SessionEvents() {}
}
