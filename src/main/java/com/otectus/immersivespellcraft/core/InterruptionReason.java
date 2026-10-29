package com.otectus.immersivespellcraft.core;

import java.util.Locale;

/** Why a session ended before its commit. Each maps to a refusal code shown to the player. */
public enum InterruptionReason {
    CANCELLED, MENU_CLOSED, DISCONNECTED, DIED, ATTACKED, DIMENSION_CHANGED, OUT_OF_REACH, STATION_CHANGED, IDLE, EXPIRED,
    RELOAD, CONFIG_CHANGED, INVALID_INPUT, ADMIN, ERROR, SERVER_STOPPING;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
