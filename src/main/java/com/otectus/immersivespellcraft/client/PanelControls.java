package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.minigame.Control;
import org.lwjgl.glfw.GLFW;

/** Shared key and mouse mapping for the overlay, the station screen and practice. */
final class PanelControls {
    static Control control(int key, int scan) {
        if (Keys.PRIMARY.matches(key, scan)) return Control.PRIMARY;
        if (Keys.LEFT.matches(key, scan) || key == GLFW.GLFW_KEY_A) return Control.LEFT;
        if (Keys.RIGHT.matches(key, scan) || key == GLFW.GLFW_KEY_D) return Control.RIGHT;
        return null;
    }

    static boolean escape(int key) {
        return key == GLFW.GLFW_KEY_ESCAPE;
    }

    private PanelControls() {}
}
