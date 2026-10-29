package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.minigame.Control;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Turns raw mouse buttons and keys into control edges. Several sources may hold one control (mouse and Space); the
 * control is down while any is. In toggle mode the primary action flips on each press instead, so no hold is ever
 * required; the server receives exactly the same edges either way.
 */
public final class PanelInput {
    public interface Sink {
        void control(Control control, boolean down);
        void pointer(int value);
    }

    public static final int MOUSE = -1;
    private final Map<Control, Set<Integer>> sources = new EnumMap<>(Control.class);
    private final Map<Control, Boolean> down = new EnumMap<>(Control.class);

    public void pressed(Control c, int source, Sink sink) {
        if (c == Control.PRIMARY && ClientConfig.holdMode() == ClientConfig.HoldMode.TOGGLE) {
            boolean next = !down.getOrDefault(c, false);
            down.put(c, next);
            sink.control(c, next);
            return;
        }
        sources.computeIfAbsent(c, k -> new HashSet<>()).add(source);
        if (!down.getOrDefault(c, false)) {
            down.put(c, true);
            sink.control(c, true);
        }
    }

    public void released(Control c, int source, Sink sink) {
        if (c == Control.PRIMARY && ClientConfig.holdMode() == ClientConfig.HoldMode.TOGGLE) return;
        Set<Integer> s = sources.get(c);
        if (s != null) s.remove(source);
        if ((s == null || s.isEmpty()) && down.getOrDefault(c, false)) {
            down.put(c, false);
            sink.control(c, false);
        }
    }

    public boolean held(Control c) {
        return down.getOrDefault(c, false);
    }

    public void reset() {
        sources.clear();
        down.clear();
    }
}
