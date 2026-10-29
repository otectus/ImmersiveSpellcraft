package com.otectus.immersivespellcraft.core;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The outcome of the one native transition. {@link Status#HELD} means the outcome could not be reconciled safely: the
 * actual inventory and native state are left exactly as they are, logged, and nothing is reissued automatically.
 */
public record CommitResult(Status status, @Nullable Refusal refusal, Component summary) {
    public enum Status { COMMITTED, REFUSED, HELD }

    public static CommitResult committed(Component summary) {
        return new CommitResult(Status.COMMITTED, null, summary);
    }

    public static CommitResult refused(Refusal refusal) {
        return new CommitResult(Status.REFUSED, refusal, Component.empty());
    }

    public static CommitResult held(Refusal refusal) {
        return new CommitResult(Status.HELD, refusal, Component.empty());
    }
}
