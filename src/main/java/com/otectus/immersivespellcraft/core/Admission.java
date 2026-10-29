package com.otectus.immersivespellcraft.core;

import org.jetbrains.annotations.Nullable;

/** The result of {@link MagicOperationAdapter#begin}: an admitted operation, or the refusal that stopped it. */
public record Admission(@Nullable PreparedOperation prepared, @Nullable Refusal refusal) {
    public static Admission admitted(PreparedOperation prepared) {
        return new Admission(prepared, null);
    }

    public static Admission refused(Refusal refusal) {
        return new Admission(null, refusal);
    }
}
