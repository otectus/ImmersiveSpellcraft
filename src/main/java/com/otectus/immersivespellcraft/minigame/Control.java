package com.otectus.immersivespellcraft.minigame;

/**
 * The bounded set of controls a performance accepts. PRIMARY is the one action every game uses (mouse button or its
 * rebindable key); LEFT and RIGHT turn or slide an alignment; POINTER is an optional absolute position for the same
 * alignment, rate-limited by the simulation so it can never jump straight to the target.
 */
public enum Control {
    PRIMARY, LEFT, RIGHT, POINTER;

    private static final Control[] VALUES = values();

    public static Control byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }

    public boolean button() {
        return this != POINTER;
    }
}
