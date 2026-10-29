package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.minigame.Control;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The panel for a block station (the Scribe's Table), which has no menu of its own. It does not pause the game and
 * the world stays visible around the panel; Escape stops the performance and closes it without losing anything.
 */
public class PerformanceScreen extends Screen {
    private PerformancePanel.Layout layout;

    public PerformanceScreen() {
        super(Component.translatable("immersive_spellcraft.screen.performance"));
    }

    @Override
    protected void init() {
        layout = PerformancePanel.layout(width, height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        ClientSession s = ClientSession.current();
        if (s == null) return;
        s.frame();
        if (layout.inGame(mouseX, mouseY)) s.pointer(layout.pointer(mouseX));
        PerformancePanel.render(g, s, layout, width, height, mouseX, mouseY);
    }

    @Override
    public void tick() {
        if (ClientSession.current() == null || ClientSession.current().containerId() >= 0) onClose();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        ClientSession s = ClientSession.current();
        if (s == null) return false;
        if (PerformancePanel.click(layout, x, y, false) == PerformancePanel.Button.STOP) {
            s.stop();
            return true;
        }
        if (button == 0) s.input.pressed(Control.PRIMARY, PanelInput.MOUSE, s);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        ClientSession s = ClientSession.current();
        if (s != null && button == 0) s.input.released(Control.PRIMARY, PanelInput.MOUSE, s);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        ClientSession s = ClientSession.current();
        if (s == null) return super.keyPressed(key, scan, modifiers);
        if (PanelControls.escape(key)) {
            s.stop();
            onClose();
            return true;
        }
        Control c = PanelControls.control(key, scan);
        if (c != null) s.input.pressed(c, key, s);
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        ClientSession s = ClientSession.current();
        Control c = PanelControls.control(key, scan);
        if (s != null && c != null) s.input.released(c, key, s);
        return true;
    }

    @Override
    public void removed() {
        ClientSession s = ClientSession.current();
        if (s != null && s.containerId() < 0 && (s.phase() == PanelModel.Phase.COUNT_IN || s.phase() == PanelModel.Phase.PERFORMING)) s.stop();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
