package com.otectus.immersivespellcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Rebindable controls (Options > Controls > Immersive Spellcraft). The primary action is also the left mouse button;
 * Escape stops a performance without losing anything. GUI context only, so they never clash with movement keys.
 */
public final class Keys {
    public static final String CATEGORY = "key.categories.immersive_spellcraft";
    public static final KeyMapping PRIMARY = new KeyMapping("key.immersive_spellcraft.primary", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, CATEGORY);
    public static final KeyMapping LEFT = new KeyMapping("key.immersive_spellcraft.left", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, CATEGORY);
    public static final KeyMapping RIGHT = new KeyMapping("key.immersive_spellcraft.right", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(PRIMARY);
        event.register(LEFT);
        event.register(RIGHT);
    }

    private Keys() {}
}
