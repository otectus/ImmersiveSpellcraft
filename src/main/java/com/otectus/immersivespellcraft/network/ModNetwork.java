package com.otectus.immersivespellcraft.network;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * One channel, version-locked: a client without Immersive Spellcraft (or with another protocol version) cannot join a
 * server that has it, so no player can reach a gated station without the performance path. Bump {@link #VERSION} on any
 * wire change.
 */
public final class ModNetwork {
    public static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ImmersiveSpellcraft.id("main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    /** Installed by client setup; null on a dedicated server. */
    public static volatile ClientTarget client;
    /** Test hook: sees every server-to-client message before it is sent (automated tests only). */
    public static volatile BiConsumer<ServerPlayer, Object> testSink;

    public static void register() {
        int id = 0;
        c2s(Packets.Begin.class, id++, Packets.Begin::encode, Packets.Begin::decode, SessionManager::beginFromMenu);
        c2s(Packets.Input.class, id++, Packets.Input::encode, Packets.Input::decode, SessionManager::input);
        c2s(Packets.Cancel.class, id++, Packets.Cancel::encode, Packets.Cancel::decode, SessionManager::cancelRequest);
        c2s(Packets.Preferences.class, id++, Packets.Preferences::encode, Packets.Preferences::decode, SessionManager::preferences);
        s2c(Packets.Started.class, id++, Packets.Started::encode, Packets.Started::decode, m -> client().started(m));
        s2c(Packets.Update.class, id++, Packets.Update::encode, Packets.Update::decode, m -> client().update(m));
        s2c(Packets.Resync.class, id++, Packets.Resync::encode, Packets.Resync::decode, m -> client().resync(m));
        s2c(Packets.Refused.class, id++, Packets.Refused::encode, Packets.Refused::decode, m -> client().refused(m));
        s2c(Packets.Notice.class, id++, Packets.Notice::encode, Packets.Notice::decode, m -> client().notice(m));
        s2c(Packets.OpenPractice.class, id++, Packets.OpenPractice::encode, Packets.OpenPractice::decode, m -> client().openPractice(m));
    }

    private static ClientTarget client() {
        ClientTarget c = client;
        if (c == null) throw new IllegalStateException("client message received without a client target");
        return c;
    }

    private static <T> void c2s(Class<T> type, int id, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
                                BiConsumer<ServerPlayer, T> handler) {
        CHANNEL.messageBuilder(type, id, NetworkDirection.PLAY_TO_SERVER).encoder(encoder).decoder(decoder)
                .consumerMainThread((T message, Supplier<NetworkEvent.Context> ctx) -> {
                    ServerPlayer sender = ctx.get().getSender();
                    if (sender != null) handler.accept(sender, message);
                }).add();
    }

    private static <T> void s2c(Class<T> type, int id, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
                                Consumer<T> handler) {
        CHANNEL.messageBuilder(type, id, NetworkDirection.PLAY_TO_CLIENT).encoder(encoder).decoder(decoder)
                .consumerMainThread((T message, Supplier<NetworkEvent.Context> ctx) -> handler.accept(message)).add();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        BiConsumer<ServerPlayer, Object> sink = testSink;
        if (sink != null) sink.accept(player, message);
        if (player instanceof FakePlayer || player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private ModNetwork() {}
}
