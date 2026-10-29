package com.otectus.immersivespellcraft.network;

import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Every message, as plain records with their codecs. Clients send intent and bounded control samples only: a menu
 * click to begin, inputs, a cancel and their accessibility requests. Never a score, a stack, a cost or a completion.
 */
public final class Packets {
    public static final int MAX_LINES = 8;

    // ---- Client to server ------------------------------------------------------------------------------------------

    /**
     * Begin a performance at the open menu. {@code slot}/{@code button}/{@code clickType} replay the native click the
     * player made (-1 slot and click type for a station button such as "resume"); {@code stateId} is the menu revision
     * the client saw, so a stale request changes nothing.
     */
    public record Begin(int containerId, int stateId, int slot, int button, int clickType) {
        public static void encode(Begin m, FriendlyByteBuf b) {
            b.writeVarInt(m.containerId); b.writeVarInt(m.stateId); b.writeShort(m.slot); b.writeByte(m.button); b.writeByte(m.clickType);
        }
        public static Begin decode(FriendlyByteBuf b) {
            return new Begin(b.readVarInt(), b.readVarInt(), b.readShort(), b.readByte(), b.readByte());
        }
    }

    public record Input(UUID session, long nonce, int sequence, int stageHint, List<InputEvent> events) {
        public static void encode(Input m, FriendlyByteBuf b) {
            b.writeUUID(m.session); b.writeLong(m.nonce); b.writeVarInt(m.sequence); b.writeVarInt(m.stageHint);
            SpecCodec.writeEvents(b, m.events);
        }
        public static Input decode(FriendlyByteBuf b) {
            return new Input(b.readUUID(), b.readLong(), b.readVarInt(), b.readVarInt(), SpecCodec.readEvents(b, SpecCodec.MAX_EVENTS_PER_PACKET));
        }
    }

    public record Cancel(UUID session) {
        public static void encode(Cancel m, FriendlyByteBuf b) { b.writeUUID(m.session); }
        public static Cancel decode(FriendlyByteBuf b) { return new Cancel(b.readUUID()); }
    }

    /** Accessibility requests; the server caps them. Sent on join and whenever the client settings change. */
    public record Preferences(float timingAssist, boolean untimed) {
        public static void encode(Preferences m, FriendlyByteBuf b) { b.writeFloat(m.timingAssist); b.writeBoolean(m.untimed); }
        public static Preferences decode(FriendlyByteBuf b) { return new Preferences(b.readFloat(), b.readBoolean()); }
    }

    // ---- Server to client ------------------------------------------------------------------------------------------

    /**
     * A performance was admitted. {@code containerId} is the menu the panel overlays, or -1 for a block station (the
     * client opens its own screen). The output is a preview copy for display only.
     */
    public record Started(UUID session, long nonce, int containerId, @Nullable BlockPos station, String operation,
                          GameSpec spec, Component title, List<Component> costs, ItemStack output, List<ItemStack> inputs,
                          String instructionKey, int nativeWindowMs) {
        public static void encode(Started m, FriendlyByteBuf b) {
            b.writeUUID(m.session); b.writeLong(m.nonce); b.writeVarInt(m.containerId);
            b.writeBoolean(m.station != null);
            if (m.station != null) b.writeBlockPos(m.station);
            b.writeUtf(m.operation, 96);
            SpecCodec.writeSpec(b, m.spec);
            b.writeComponent(m.title);
            writeLines(b, m.costs);
            b.writeItem(m.output);
            b.writeVarInt(Math.min(m.inputs.size(), MAX_LINES));
            for (int i = 0; i < Math.min(m.inputs.size(), MAX_LINES); i++) b.writeItem(m.inputs.get(i));
            b.writeUtf(m.instructionKey, 128);
            b.writeVarInt(m.nativeWindowMs);
        }
        public static Started decode(FriendlyByteBuf b) {
            UUID session = b.readUUID();
            long nonce = b.readLong();
            int container = b.readVarInt();
            BlockPos station = b.readBoolean() ? b.readBlockPos() : null;
            String operation = b.readUtf(96);
            GameSpec spec = SpecCodec.readSpec(b);
            Component title = b.readComponent();
            List<Component> costs = readLines(b);
            ItemStack output = b.readItem();
            int n = SpecCodec.bounded(b.readVarInt(), MAX_LINES);
            List<ItemStack> inputs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) inputs.add(b.readItem());
            return new Started(session, nonce, container, station, operation, spec, title, costs, output, inputs,
                    b.readUtf(128), b.readVarInt());
        }
    }

    public enum Status { PROGRESS, COMPLETE, CANCELLED, EXPIRED, HELD }

    /** Authoritative progress, the final result, or why the session ended. {@code score} is present for COMPLETE. */
    public record Update(UUID session, Status status, int stage, int acknowledged, String code, Component message,
                         @Nullable ScoreCard score) {
        public static void encode(Update m, FriendlyByteBuf b) {
            b.writeUUID(m.session); b.writeEnum(m.status); b.writeVarInt(m.stage); b.writeVarInt(m.acknowledged);
            b.writeUtf(m.code, 64); b.writeComponent(m.message);
            b.writeBoolean(m.score != null);
            if (m.score != null) SpecCodec.writeScore(b, m.score);
        }
        public static Update decode(FriendlyByteBuf b) {
            return new Update(b.readUUID(), b.readEnum(Status.class), b.readVarInt(), b.readVarInt(), b.readUtf(64),
                    b.readComponent(), b.readBoolean() ? SpecCodec.readScore(b) : null);
        }
    }

    /** The server applied some inputs later than they were sent; the client rebuilds its prediction from these. */
    public record Resync(UUID session, List<InputEvent> applied) {
        public static void encode(Resync m, FriendlyByteBuf b) { b.writeUUID(m.session); SpecCodec.writeEvents(b, m.applied); }
        public static Resync decode(FriendlyByteBuf b) { return new Resync(b.readUUID(), SpecCodec.readEvents(b, SpecCodec.MAX_RESYNC_EVENTS)); }
    }

    /** A start request that never became a session, with its reason in the player's language. */
    public record Refused(int containerId, String code, Component message) {
        public static void encode(Refused m, FriendlyByteBuf b) { b.writeVarInt(m.containerId); b.writeUtf(m.code, 64); b.writeComponent(m.message); }
        public static Refused decode(FriendlyByteBuf b) { return new Refused(b.readVarInt(), b.readUtf(64), b.readComponent()); }
    }

    /** Station state for the open menu: e.g. a press job that waits for this player's embossing. */
    public record Notice(int containerId, boolean pending, String operation, Component text) {
        public static void encode(Notice m, FriendlyByteBuf b) {
            b.writeVarInt(m.containerId); b.writeBoolean(m.pending); b.writeUtf(m.operation, 96); b.writeComponent(m.text);
        }
        public static Notice decode(FriendlyByteBuf b) { return new Notice(b.readVarInt(), b.readBoolean(), b.readUtf(96), b.readComponent()); }
    }

    /** Opens an isolated practice game on the client ({@code /immersivespellcraft practice}). Nothing is sent back. */
    public record OpenPractice(String game) {
        public static void encode(OpenPractice m, FriendlyByteBuf b) { b.writeUtf(m.game, 96); }
        public static OpenPractice decode(FriendlyByteBuf b) { return new OpenPractice(b.readUtf(96)); }
    }

    private static void writeLines(FriendlyByteBuf b, List<Component> lines) {
        int n = Math.min(lines.size(), MAX_LINES);
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) b.writeComponent(lines.get(i));
    }

    private static List<Component> readLines(FriendlyByteBuf b) {
        int n = SpecCodec.bounded(b.readVarInt(), MAX_LINES);
        List<Component> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(b.readComponent());
        return out;
    }

    private Packets() {}
}
