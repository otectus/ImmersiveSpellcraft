package com.otectus.immersivespellcraft.session;

import com.otectus.immersivespellcraft.minigame.InputEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates one session's input batches: strictly increasing sequence numbers, nondecreasing event times, no event
 * from further ahead than the server's own clock allows, and a bounded event rate. A late event is applied at the
 * simulation's current time instead of its claimed time (bounded latency compensation): a client timestamp can never
 * reach back past the server's watermark to award itself earlier, better timing.
 */
final class InputValidator {
    static final int LEAD_MS = 250;
    private static final int MAX_SEQUENCE_GAP = 1000;

    enum Kind { ACCEPTED, DUPLICATE, INVALID }

    record Verdict(Kind kind, List<InputEvent> events, boolean adjusted, String why) {
        static Verdict of(Kind kind, String why) { return new Verdict(kind, List.of(), false, why); }
    }

    private int lastSequence = -1;
    private int lastTime;
    private long windowStart = Long.MIN_VALUE;
    private int windowCount;

    Verdict check(int sequence, List<InputEvent> events, int elapsedMs, int simNow, int maxPerSecond, long now) {
        if (sequence <= lastSequence) return Verdict.of(Kind.DUPLICATE, "replayed sequence " + sequence);
        if (sequence > lastSequence + MAX_SEQUENCE_GAP) return Verdict.of(Kind.INVALID, "sequence gap");
        if (now - windowStart >= 1000) {
            windowStart = now;
            windowCount = 0;
        }
        windowCount += events.size();
        if (windowCount > maxPerSecond) return Verdict.of(Kind.INVALID, "input rate above " + maxPerSecond + "/s");
        List<InputEvent> out = new ArrayList<>(events.size());
        boolean adjusted = false;
        int previous = lastTime;
        for (InputEvent e : events) {
            if (e.timeMs() < previous) return Verdict.of(Kind.INVALID, "input out of order");
            if (e.timeMs() > elapsedMs + LEAD_MS) return Verdict.of(Kind.INVALID, "input from the future (" + e.timeMs() + " > " + elapsedMs + ")");
            previous = e.timeMs();
            int t = Math.max(e.timeMs(), simNow);
            if (t != e.timeMs()) adjusted = true;
            out.add(t == e.timeMs() ? e : new InputEvent(t, e.control(), e.value()));
        }
        lastSequence = sequence;
        lastTime = previous;
        return new Verdict(Kind.ACCEPTED, out, adjusted, "");
    }

    int acknowledged() {
        return Math.max(0, lastSequence);
    }
}
