package com.otectus.immersivespellcraft.core;

import java.util.Locale;

/** How an operation is treated: a performed game, a brief setup attunement, or native behavior preserved. */
public enum Treatment {
    PERFORM, ATTUNE, PRESERVE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Treatment byId(String id) {
        for (Treatment t : values()) if (t.id().equals(id)) return t;
        return null;
    }
}
