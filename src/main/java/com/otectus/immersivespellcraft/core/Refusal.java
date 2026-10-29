package com.otectus.immersivespellcraft.core;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A structured refusal: a stable code (for logs, commands and tests) plus the arguments of its translated message
 * {@code immersive_spellcraft.refusal.<code>}. Native refusals keep their native message instead.
 */
public record Refusal(String code, List<Component> args, Component nativeMessage) {
    public Refusal {
        args = List.copyOf(args);
    }

    public static Refusal of(String code, Component... args) {
        return new Refusal(code, List.of(args), null);
    }

    /** A refusal the native station gave in its own words. */
    public static Refusal nativeRefusal(String code, Component message) {
        return new Refusal(code, List.of(), message);
    }

    public Component message() {
        if (nativeMessage != null) return nativeMessage;
        return Component.translatable("immersive_spellcraft.refusal." + code, args.toArray());
    }
}
