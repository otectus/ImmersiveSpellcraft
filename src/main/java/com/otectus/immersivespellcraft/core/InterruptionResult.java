package com.otectus.immersivespellcraft.core;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** What an interruption left behind, for the player: e.g. "the glyph still waits on the table". */
public record InterruptionResult(@Nullable Component note) {
    public static final InterruptionResult RELEASED = new InterruptionResult(null);
}
