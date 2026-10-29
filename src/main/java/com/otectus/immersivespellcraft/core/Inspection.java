package com.otectus.immersivespellcraft.core;

import org.jetbrains.annotations.Nullable;

/**
 * The read-only answer to "what would this station do here?". Produced without draining XP, rolling a recipe,
 * mutating an item or casting anything. {@code query} carries the registry identities profile selection matches on.
 */
public record Inspection(boolean applicable, @Nullable Refusal refusal, @Nullable ProfileQuery query) {
    /** The adapter does not handle this block or menu at all; another adapter (or native behavior) may. */
    public static final Inspection NOT_APPLICABLE = new Inspection(false, null, null);

    public static Inspection eligible(ProfileQuery query) {
        return new Inspection(true, null, query);
    }

    public static Inspection refused(Refusal refusal, @Nullable ProfileQuery query) {
        return new Inspection(true, refusal, query);
    }

    public boolean eligible() {
        return applicable && refusal == null;
    }
}
