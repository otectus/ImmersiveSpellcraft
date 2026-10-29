package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.minigame.Control;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Draws the performance panel over a station's own menu screen (Scroll Forge, Glyph Press) and takes its input while a
 * performance runs there, so the native menu stays open on the server and nothing about it is replaced. Escape stops
 * the performance first; a second Escape closes the station as usual. Also shows a station's pending-performance
 * prompt (a Press job waiting for this player's embossing) and any refusal.
 */
public final class PerformanceOverlay {
    private static final int PROMPT_W = 200, PROMPT_H = 34;

    private static ClientSession session(ScreenEvent e) {
        return e.getScreen() instanceof AbstractContainerScreen<?> s ? ClientSession.forMenu(s.getMenu().containerId) : null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        ClientSession s = session(e);
        if (s != null) {
            e.setCanceled(true);
            PerformancePanel.Layout l = PerformancePanel.layout(screen.width, screen.height);
            if (PerformancePanel.click(l, e.getMouseX(), e.getMouseY(), false) == PerformancePanel.Button.STOP) s.stop();
            else if (e.getButton() == 0) s.input.pressed(Control.PRIMARY, PanelInput.MOUSE, s);
            return;
        }
        Packets.Notice pending = ClientSession.pending(screen.getMenu().containerId);
        if (pending != null && inPromptButton(screen, e.getMouseX(), e.getMouseY())) {
            e.setCanceled(true);
            ModNetwork.sendToServer(new Packets.Begin(screen.getMenu().containerId, screen.getMenu().getStateId(), -1, 0, -1));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre e) {
        ClientSession s = session(e);
        if (s == null) return;
        e.setCanceled(true);
        if (e.getButton() == 0) s.input.released(Control.PRIMARY, PanelInput.MOUSE, s);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void keyPressed(ScreenEvent.KeyPressed.Pre e) {
        ClientSession s = session(e);
        if (s == null) return;
        e.setCanceled(true);
        if (PanelControls.escape(e.getKeyCode())) {
            s.stop();
            return;
        }
        Control c = PanelControls.control(e.getKeyCode(), e.getScanCode());
        if (c != null) s.input.pressed(c, e.getKeyCode(), s);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void keyReleased(ScreenEvent.KeyReleased.Pre e) {
        ClientSession s = session(e);
        if (s == null) return;
        e.setCanceled(true);
        Control c = PanelControls.control(e.getKeyCode(), e.getScanCode());
        if (c != null) s.input.released(c, e.getKeyCode(), s);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dragged(ScreenEvent.MouseDragged.Pre e) {
        if (session(e) != null) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scrolled(ScreenEvent.MouseScrolled.Pre e) {
        if (session(e) != null) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void typed(ScreenEvent.CharacterTyped.Pre e) {
        if (session(e) != null) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void tooltip(RenderTooltipEvent.Pre e) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof AbstractContainerScreen<?> s && ClientSession.forMenu(s.getMenu().containerId) != null) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        GuiGraphics g = e.getGuiGraphics();
        int container = screen.getMenu().containerId;
        ClientSession s = ClientSession.forMenu(container);
        if (s != null) {
            s.frame();
            PerformancePanel.Layout l = PerformancePanel.layout(screen.width, screen.height);
            if (l.inGame(e.getMouseX(), e.getMouseY())) s.pointer(l.pointer(e.getMouseX()));
            PerformancePanel.render(g, s, l, screen.width, screen.height, e.getMouseX(), e.getMouseY());
            return;
        }
        Font font = Minecraft.getInstance().font;
        Packets.Notice pending = ClientSession.pending(container);
        Component refusal = ClientSession.refusal(container);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (pending != null) {
            int x = (screen.width - PROMPT_W) / 2, y = 4;
            g.fill(x, y, x + PROMPT_W, y + PROMPT_H, 0xE0221A14);
            g.renderOutline(x, y, PROMPT_W, PROMPT_H, 0xFFB08A4C);
            g.drawCenteredString(font, pending.text(), screen.width / 2, y + 4, 0xFFF6EBDD);
            boolean hover = inPromptButton(screen, e.getMouseX(), e.getMouseY());
            int bx = screen.width / 2 - 50, by = y + 17;
            g.fill(bx, by, bx + 100, by + 13, hover ? 0xFFFF9A3C : 0xFF4A3A2A);
            g.drawCenteredString(font, Component.translatable("immersive_spellcraft.panel.resume"), screen.width / 2, by + 3, 0xFFFFFFFF);
        }
        if (refusal != null) {
            int w = Math.min(screen.width - 8, font.width(refusal) + 12), x = (screen.width - w) / 2, y = screen.height - 22;
            g.fill(x, y, x + w, y + 14, 0xE0300A0A);
            g.drawCenteredString(font, refusal, screen.width / 2, y + 3, 0xFFFFD0C0);
        }
        g.pose().popPose();
    }

    private static boolean inPromptButton(AbstractContainerScreen<?> screen, double mx, double my) {
        int bx = screen.width / 2 - 50, by = 4 + 17;
        return mx >= bx && mx <= bx + 100 && my >= by && my <= by + 13;
    }

    @SubscribeEvent
    public static void closing(ScreenEvent.Closing e) {
        if (e.getScreen() instanceof AbstractContainerScreen<?> screen) ClientSession.menuGone(screen.getMenu().containerId);
    }

    private PerformanceOverlay() {}
}
