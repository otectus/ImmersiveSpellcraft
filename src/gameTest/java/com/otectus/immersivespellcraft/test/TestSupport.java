package com.otectus.immersivespellcraft.test;

import com.mojang.authlib.GameProfile;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.minigame.Autoplay;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.session.PerformanceSession;
import com.otectus.immersivespellcraft.session.SessionClock;
import com.otectus.immersivespellcraft.session.SessionManager;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared GameTest plumbing. Performing players are real {@link ServerPlayer}s with an embedded connection (never
 * logged in; packets are buffered), because fake players are automation and never perform. Every message the server
 * sends is recorded per player. Sessions follow game time here, since the test server fast-forwards ticks.
 */
final class TestSupport {
    private static final Map<UUID, List<Object>> SENT = new ConcurrentHashMap<>();

    static {
        ModNetwork.testSink = (player, message) -> SENT.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(message);
        SessionClock.useSource(() -> ServerLifecycleHooks.getCurrentServer().getTickCount() * 50L);
    }

    static void init() {}

    /** A survival player standing beside {@code near} (relative), as a real client would be. */
    static ServerPlayer player(GameTestHelper h, String name, BlockPos near) {
        init();
        ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player);
        BlockPos pos = h.absolutePos(near);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 0.5, 0, 0);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Automation: a fake player, for the native reference path. */
    static FakePlayer automaton(GameTestHelper h, BlockPos near) {
        init();
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ism_automaton"));
        BlockPos pos = h.absolutePos(near);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 0.5, 0, 0);
        return player;
    }

    static void floor(GameTestHelper h) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
    }

    static boolean active(ResourceLocation adapter) {
        return AdapterRegistry.byId(adapter).isPresent();
    }

    static <T> List<T> sent(ServerPlayer player, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Object o : List.copyOf(SENT.getOrDefault(player.getUUID(), List.of()))) if (type.isInstance(o)) out.add(type.cast(o));
        return out;
    }

    static Packets.Update lastUpdate(ServerPlayer player) {
        List<Packets.Update> u = sent(player, Packets.Update.class);
        return u.isEmpty() ? null : u.get(u.size() - 1);
    }

    static Packets.Update terminal(ServerPlayer player) {
        for (Packets.Update u : sent(player, Packets.Update.class)) if (u.status() != Packets.Status.PROGRESS) return u;
        return null;
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new GameTestAssertException(message);
    }

    /** Feeds a started session the demonstration inputs, as a client would, in time with the session clock. */
    static final class Driver {
        final ServerPlayer player;
        final PerformanceSession session;
        final List<InputEvent> events;
        private int next, sequence;

        /**
         * Plans a run from the session's current simulation time: a session that has been idle has advanced its clock but
         * not its state, so the planned inputs are shifted by that time and replayed exactly as a late-joining client would.
         */
        Driver(ServerPlayer player, Autoplay.Style style) {
            this.player = player;
            this.session = SessionManager.session(player).orElseThrow(() -> new GameTestAssertException("no session started"));
            int offset = Math.max(0, session.simulation.now());
            this.events = Autoplay.play(session.spec, style).stream()
                    .map(e -> new InputEvent(e.timeMs() + offset, e.control(), e.value())).toList();
        }

        /** Sends every input whose time has come; call once per tick. */
        void tick() {
            int elapsed = session.performElapsed(SessionClock.now());
            List<InputEvent> batch = new ArrayList<>();
            while (next < events.size() && events.get(next).timeMs() <= elapsed && batch.size() < 32) batch.add(events.get(next++));
            if (!batch.isEmpty())
                SessionManager.input(player, new Packets.Input(session.id, session.nonce, sequence++, session.simulation.stageIndex(), batch));
        }

        boolean finished() {
            return terminal(player) != null;
        }
    }

    private TestSupport() {}
}
