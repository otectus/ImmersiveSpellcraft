package com.otectus.immersivespellcraft.minigame;

/**
 * One control change at a time in milliseconds since the performance began (after the count-in). Buttons carry 1 for
 * down and 0 for up; the pointer carries a position from -1000 to 1000 along the current alignment axis.
 */
public record InputEvent(int timeMs, Control control, int value) {
    public static final int POINTER_RANGE = 1000;

    public InputEvent {
        if (control == null) throw new IllegalArgumentException("control");
        if (control.button() && value != 0 && value != 1) throw new IllegalArgumentException("button value " + value);
        if (!control.button() && (value < -POINTER_RANGE || value > POINTER_RANGE)) throw new IllegalArgumentException("pointer " + value);
    }

    public static InputEvent down(int timeMs, Control control) { return new InputEvent(timeMs, control, 1); }
    public static InputEvent up(int timeMs, Control control) { return new InputEvent(timeMs, control, 0); }
}
