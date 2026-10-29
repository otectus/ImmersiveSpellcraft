package com.otectus.immersivespellcraft.core;

import org.jetbrains.annotations.Nullable;

/** Whether an admitted operation still matches the world exactly. A failed validation moves nothing. */
public record Validation(boolean valid, @Nullable Refusal refusal, InterruptionReason reason) {
    public static final Validation OK = new Validation(true, null, InterruptionReason.CANCELLED);

    public static Validation invalid(InterruptionReason reason, Refusal refusal) {
        return new Validation(false, refusal, reason);
    }
}
