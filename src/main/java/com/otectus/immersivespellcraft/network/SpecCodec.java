package com.otectus.immersivespellcraft.network;

import com.otectus.immersivespellcraft.minigame.Control;
import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.Grade;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import com.otectus.immersivespellcraft.minigame.StageKind;
import com.otectus.immersivespellcraft.minigame.StageSpec;
import com.otectus.immersivespellcraft.minigame.Weight;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded wire format for game specs, inputs and score cards. Every read is capped; oversize data fails decoding. */
public final class SpecCodec {
    public static final int MAX_EVENTS_PER_PACKET = 64;
    public static final int MAX_RESYNC_EVENTS = 1024;
    private static final int MAX_STRING = 96;

    public static void writeSpec(FriendlyByteBuf buf, GameSpec s) {
        buf.writeUtf(s.type(), MAX_STRING);
        buf.writeVarInt(s.generatorVersion());
        buf.writeLong(s.seed());
        buf.writeVarInt(s.countInMs());
        buf.writeVarInt(s.maxDurationMs());
        buf.writeBoolean(s.untimed());
        buf.writeDouble(s.assist());
        buf.writeVarInt(s.stageCount());
        for (StageSpec st : s.stages()) writeStage(buf, st);
        buf.writeVarInt(s.weights().size());
        for (Weight w : s.weights()) {
            buf.writeUtf(w.measure(), MAX_STRING);
            buf.writeDouble(w.weight());
        }
    }

    public static GameSpec readSpec(FriendlyByteBuf buf) {
        String type = buf.readUtf(MAX_STRING);
        int version = buf.readVarInt();
        long seed = buf.readLong();
        int countIn = buf.readVarInt(), max = buf.readVarInt();
        boolean untimed = buf.readBoolean();
        double assist = buf.readDouble();
        int n = bounded(buf.readVarInt(), GameSpec.MAX_STAGES);
        List<StageSpec> stages = new ArrayList<>(n);
        for (int i = 0; i < n; i++) stages.add(readStage(buf));
        int w = bounded(buf.readVarInt(), GameSpec.MAX_WEIGHTS);
        List<Weight> weights = new ArrayList<>(w);
        for (int i = 0; i < w; i++) weights.add(new Weight(buf.readUtf(MAX_STRING), buf.readDouble()));
        try {
            return new GameSpec(type, version, seed, countIn, max, untimed, assist, stages, weights);
        } catch (IllegalArgumentException e) {
            throw new DecoderException("invalid game spec: " + e.getMessage());
        }
    }

    private static void writeStage(FriendlyByteBuf buf, StageSpec s) {
        buf.writeByte(s.kind().ordinal());
        buf.writeUtf(s.label(), MAX_STRING);
        buf.writeUtf(s.measure(), MAX_STRING);
        if (s instanceof StageSpec.Align a) {
            buf.writeDouble(a.start()); buf.writeDouble(a.target()); buf.writeDouble(a.tolerance()); buf.writeDouble(a.speed());
        } else if (s instanceof StageSpec.HoldBand h) {
            buf.writeDouble(h.low()); buf.writeDouble(h.high()); buf.writeDouble(h.rise()); buf.writeDouble(h.fall());
            buf.writeVarInt(h.requiredMs()); buf.writeDouble(h.driftAmplitude()); buf.writeVarInt(h.driftPeriodMs());
        } else if (s instanceof StageSpec.TimedTrigger t) {
            buf.writeByte(t.trigger().ordinal()); buf.writeVarInt(t.periodMs()); buf.writeDouble(t.center()); buf.writeDouble(t.halfWidth());
        } else if (s instanceof StageSpec.Strokes k) {
            buf.writeUtf(k.inkMeasure(), MAX_STRING);
            buf.writeVarIntArray(k.lengthsMs());
            buf.writeVarInt(k.junctionHalfMs()); buf.writeVarInt(k.repairMs());
        }
    }

    private static StageSpec readStage(FriendlyByteBuf buf) {
        StageKind kind = StageKind.byId(buf.readByte());
        if (kind == null) throw new DecoderException("unknown stage kind");
        String label = buf.readUtf(MAX_STRING), measure = buf.readUtf(MAX_STRING);
        try {
            return switch (kind) {
                case ALIGN -> new StageSpec.Align(label, measure, buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
                case HOLD_BAND -> new StageSpec.HoldBand(label, measure, buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readVarInt(), buf.readDouble(), buf.readVarInt());
                case TIMED_TRIGGER -> {
                    int trig = buf.readByte();
                    if (trig < 0 || trig >= StageSpec.Trigger.values().length) throw new DecoderException("trigger");
                    yield new StageSpec.TimedTrigger(label, measure, StageSpec.Trigger.values()[trig], buf.readVarInt(),
                            buf.readDouble(), buf.readDouble());
                }
                case STROKES -> new StageSpec.Strokes(label, measure, buf.readUtf(MAX_STRING), buf.readVarIntArray(8),
                        buf.readVarInt(), buf.readVarInt());
            };
        } catch (IllegalArgumentException e) {
            throw new DecoderException("invalid stage: " + e.getMessage());
        }
    }

    public static void writeEvents(FriendlyByteBuf buf, List<InputEvent> events) {
        buf.writeVarInt(events.size());
        for (InputEvent e : events) {
            buf.writeVarInt(e.timeMs());
            buf.writeByte(e.control().ordinal());
            buf.writeShort(e.value());
        }
    }

    public static List<InputEvent> readEvents(FriendlyByteBuf buf, int max) {
        int n = bounded(buf.readVarInt(), max);
        List<InputEvent> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int time = buf.readVarInt();
            Control c = Control.byId(buf.readByte());
            int value = buf.readShort();
            if (c == null || time < 0) throw new DecoderException("invalid input event");
            try {
                out.add(new InputEvent(time, c, value));
            } catch (IllegalArgumentException e) {
                throw new DecoderException("invalid input event: " + e.getMessage());
            }
        }
        return out;
    }

    public static void writeScore(FriendlyByteBuf buf, ScoreCard s) {
        buf.writeBoolean(s.complete());
        buf.writeVarInt(s.quality());
        buf.writeVarInt(s.measures().size());
        s.measures().forEach((k, v) -> { buf.writeUtf(k, MAX_STRING); buf.writeDouble(v); });
        buf.writeVarInt(s.corrections());
        buf.writeVarInt(s.elapsedMs());
        buf.writeBoolean(s.assisted());
    }

    public static ScoreCard readScore(FriendlyByteBuf buf) {
        boolean complete = buf.readBoolean();
        int quality = Math.max(0, Math.min(100, buf.readVarInt()));
        int n = bounded(buf.readVarInt(), 8);
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) m.put(buf.readUtf(MAX_STRING), buf.readDouble());
        return new ScoreCard(complete, quality, Grade.of(quality), m, buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    static int bounded(int n, int max) {
        if (n < 0 || n > max) throw new DecoderException("count " + n + " exceeds " + max);
        return n;
    }

    private SpecCodec() {}
}
