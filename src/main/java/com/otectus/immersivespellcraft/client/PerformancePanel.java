package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import com.otectus.immersivespellcraft.minigame.Simulation;
import com.otectus.immersivespellcraft.minigame.StageSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * The one panel every performance uses, over a station screen or in its own screen: the item being made, one short
 * instruction for the current action, the game, captions for every cue, the native costs, and finally one compact
 * result with the workmanship grade. Never opaque over the whole screen: the station stays visible around it.
 */
public final class PerformancePanel {
    public static final int WIDTH = 272, HEIGHT = 204;
    private static final int GAME_X = 8, GAME_Y = 34, GAME_W = 256, GAME_H = 108;

    public enum Button { NONE, STOP, AGAIN, DEMO }

    public record Layout(float scale, int x, int y, int w, int h) {
        public double localX(double mouseX) { return (mouseX - x) / scale; }
        public double localY(double mouseY) { return (mouseY - y) / scale; }
        public boolean inGame(double mouseX, double mouseY) {
            double lx = localX(mouseX), ly = localY(mouseY);
            return lx >= GAME_X && lx <= GAME_X + GAME_W && ly >= GAME_Y && ly <= GAME_Y + GAME_H;
        }
        /** -1000..1000 across the game area, for pointer alignment. */
        public int pointer(double mouseX) {
            double t = (localX(mouseX) - GAME_X) / GAME_W * 2 - 1;
            return (int) Math.round(Math.max(-1, Math.min(1, t)) * 1000);
        }
    }

    public static Layout layout(int screenW, int screenH) {
        float scale = (float) ClientConfig.get(ClientConfig.UI_SCALE);
        scale = Math.min(scale, Math.min((screenW - 8f) / WIDTH, (screenH - 8f) / HEIGHT));
        int w = Math.round(WIDTH * scale), h = Math.round(HEIGHT * scale);
        return new Layout(scale, (screenW - w) / 2, (screenH - h) / 2, w, h);
    }

    public static void render(GuiGraphics g, PanelModel m, Layout l, int screenW, int screenH, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        GameSpec spec = m.spec();
        Palette p = Palette.forGame(spec.type(), 0);
        float dim = (float) ClientConfig.get(ClientConfig.BACKGROUND_DIM);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (dim > 0) g.fill(0, 0, screenW, screenH, Palette.alpha(0xFF000000, dim));
        g.pose().translate(l.x(), l.y(), 0);
        g.pose().scale(l.scale(), l.scale(), 1);

        Draw d = Draw.begin(g);
        d.rect(0, 0, WIDTH, HEIGHT, p.panel);
        d.frame(0, 0, WIDTH, HEIGHT, 1.5f, p.panelEdge);
        d.end();

        // Header: the operation and the item it makes.
        ItemStack output = m.output();
        int titleX = 8;
        if (!output.isEmpty()) {
            g.renderItem(output, 8, 6);
            titleX = 30;
        }
        g.drawString(font, ellipsize(font, m.title(), WIDTH - titleX - 10), titleX, 7, p.text, false);
        Component game = Component.translatable(CueFeed.prefix(spec.type()) + ".name");
        if (m.practice()) game = Component.translatable("immersive_spellcraft.panel.practice", game);
        if (spec.untimed()) game = Component.translatable("immersive_spellcraft.panel.untimed", game);
        g.drawString(font, game, titleX, 19, p.textDim, false);
        pips(g, m.simulation(), p, WIDTH - 8, 19);

        PanelModel.Phase phase = m.phase();
        if (phase == PanelModel.Phase.RESULT || phase == PanelModel.Phase.ENDED) {
            result(g, font, m, p);
        } else {
            GameView.draw(g, m.simulation(), p, GAME_X, GAME_Y, GAME_W, GAME_H, true);
            if (phase == PanelModel.Phase.COUNT_IN) {
                int left = (int) Math.ceil(-m.now() / 400.0);
                Component count = Component.literal(String.valueOf(Math.max(1, left)));
                g.pose().pushPose();
                g.pose().translate(GAME_X + GAME_W / 2f, GAME_Y + GAME_H / 2f - 8, 0);
                g.pose().scale(3, 3, 1);
                g.drawCenteredString(font, count, 0, 0, p.accent);
                g.pose().popPose();
            } else if (phase == PanelModel.Phase.FINISHING) {
                g.drawCenteredString(font, Component.translatable("immersive_spellcraft.panel.finishing"), GAME_X + GAME_W / 2, GAME_Y + 4, p.good);
            }
            Component instruction = instruction(m.simulation(), m.instructionKey());
            List<FormattedCharSequence> lines = font.split(instruction, WIDTH - 16);
            for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), 8, GAME_Y + GAME_H + 4 + i * 10, p.text, false);
            int cy = GAME_Y + GAME_H + 26;
            for (CueFeed.Caption c : m.cues().captions()) {
                int color = switch (c.kind()) {
                    case ALIGNED, BAND_ENTERED, TRIGGER_HIT, STROKE_DONE, COMPLETE -> p.good;
                    case MISALIGNED, TRIGGER_MISSED, STROKE_BLEED, BAND_HIGH, EXPIRED -> p.bad;
                    default -> p.warn;
                };
                g.drawString(font, Component.literal(symbol(c.kind())).append(" ").append(c.text()), 8, cy, color, false);
                cy += 10;
                if (cy > HEIGHT - 26) break;
            }
        }

        // Footer: native costs, unchanged by the performance.
        List<Component> costs = m.costs();
        int fy = HEIGHT - 12;
        if (!costs.isEmpty() && phase != PanelModel.Phase.RESULT && phase != PanelModel.Phase.ENDED) {
            Component line = costs.get(0);
            for (int i = 1; i < Math.min(costs.size(), 3); i++) line = line.copy().append(" · ").append(costs.get(i));
            g.drawString(font, ellipsize(font, line, (m.practice() ? WIDTH - 186 : WIDTH - 70) - 8), 8, fy, p.textDim, false);
        }
        button(g, font, m.practice() ? "immersive_spellcraft.panel.close" : phase == PanelModel.Phase.RESULT
                || phase == PanelModel.Phase.ENDED ? "immersive_spellcraft.panel.close" : "immersive_spellcraft.panel.stop", stopRect(), p, l, mouseX, mouseY);
        if (m.practice()) {
            button(g, font, "immersive_spellcraft.panel.again", againRect(), p, l, mouseX, mouseY);
            button(g, font, "immersive_spellcraft.panel.demo", demoRect(), p, l, mouseX, mouseY);
        }
        g.pose().popPose();
    }

    private static Component instruction(Simulation sim, String key) {
        StageSpec s = sim.currentSpec();
        Component primary = Keys.PRIMARY.getTranslatedKeyMessage();
        Component left = Keys.LEFT.getTranslatedKeyMessage(), right = Keys.RIGHT.getTranslatedKeyMessage();
        String prefix = key.endsWith(".instruction") ? key.substring(0, key.length() - ".instruction".length()) : key;
        return Component.translatable(prefix + ".stage." + s.label(), primary, left, right);
    }

    private static void pips(GuiGraphics g, Simulation sim, Palette p, int right, int y) {
        int n = sim.spec().stageCount();
        for (int i = 0; i < n; i++) {
            int x = right - (n - i) * 10;
            boolean done = sim.view(i).done();
            boolean current = i == sim.stageIndex() && !sim.complete();
            g.fill(x, y + 1, x + 7, y + 8, done ? p.good : current ? p.accent : p.track);
        }
    }

    private static void result(GuiGraphics g, Font font, PanelModel m, Palette p) {
        ScoreCard s = m.result();
        int x = GAME_X, y = GAME_Y;
        g.fill(x, y, x + GAME_W, y + GAME_H + 20, p.area);
        if (m.phase() == PanelModel.Phase.RESULT && s != null) {
            Component grade = Component.translatable("immersive_spellcraft.grade." + s.grade().key());
            g.pose().pushPose();
            g.pose().translate(x + 8, y + 6, 0);
            g.pose().scale(2, 2, 1);
            g.drawString(font, grade, 0, 0, p.accent, false);
            g.pose().popPose();
            if (ClientConfig.get(ClientConfig.SHOW_QUALITY))
                g.drawString(font, Component.translatable("immersive_spellcraft.panel.quality", s.quality()), x + 8, y + 26, p.text, false);
            int my = y + 40;
            for (Map.Entry<String, Double> e : s.measures().entrySet()) {
                g.drawString(font, Component.translatable("immersive_spellcraft.measure." + e.getKey()), x + 8, my, p.textDim, false);
                int bx = x + 110, bw = 100;
                g.fill(bx, my + 1, bx + bw, my + 7, p.track);
                g.fill(bx, my + 1, bx + (int) (bw * e.getValue()), my + 7, p.good);
                g.drawString(font, Math.round(e.getValue() * 100) + "%", bx + bw + 6, my, p.textDim, false);
                my += 11;
            }
            g.drawString(font, Component.translatable("immersive_spellcraft.panel.corrections", s.corrections(), s.elapsedMs() / 1000.0),
                    x + 8, my + 2, p.textDim, false);
            List<FormattedCharSequence> lines = font.split(m.message(), GAME_W - 16);
            for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), x + 8, my + 16 + i * 10, p.text, false);
            if (m.practice()) {
                List<FormattedCharSequence> note = font.split(Component.translatable("immersive_spellcraft.panel.practice_note"), GAME_W - 16);
                for (int i = 0; i < Math.min(2, note.size()); i++) g.drawString(font, note.get(i), x + 8, y + GAME_H + 2 + i * 10, p.textDim, false);
            }
        } else {
            List<FormattedCharSequence> lines = font.split(m.message(), GAME_W - 16);
            g.drawString(font, Component.translatable("immersive_spellcraft.panel.ended"), x + 8, y + 8, p.warn, false);
            for (int i = 0; i < Math.min(6, lines.size()); i++) g.drawString(font, lines.get(i), x + 8, y + 24 + i * 10, p.text, false);
        }
    }

    private static String symbol(com.otectus.immersivespellcraft.minigame.Cue.Kind kind) {
        return switch (kind) {
            case ALIGNED, BAND_ENTERED, TRIGGER_HIT, STROKE_DONE, COMPLETE -> "✔";
            case MISALIGNED, TRIGGER_MISSED, STROKE_BLEED, BAND_HIGH, EXPIRED -> "✖";
            default -> "•";
        };
    }

    private static int[] stopRect() { return new int[]{WIDTH - 62, HEIGHT - 16, 54, 12}; }
    private static int[] againRect() { return new int[]{WIDTH - 182, HEIGHT - 16, 54, 12}; }
    private static int[] demoRect() { return new int[]{WIDTH - 122, HEIGHT - 16, 54, 12}; }

    private static void button(GuiGraphics g, Font font, String key, int[] r, Palette p, Layout l, double mx, double my) {
        boolean hover = inside(r, l.localX(mx), l.localY(my));
        // The panel color keeps the light label readable in every palette, high contrast included.
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hover ? Palette.mix(p.panel, p.accent, 0.45f) : p.panel);
        g.renderOutline(r[0], r[1], r[2], r[3], hover ? p.accent : p.panelEdge);
        g.drawCenteredString(font, Component.translatable(key), r[0] + r[2] / 2, r[1] + 2, p.text);
    }

    private static boolean inside(int[] r, double x, double y) {
        return x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3];
    }

    public static Button click(Layout l, double mouseX, double mouseY, boolean practice) {
        double x = l.localX(mouseX), y = l.localY(mouseY);
        if (inside(stopRect(), x, y)) return Button.STOP;
        if (practice && inside(againRect(), x, y)) return Button.AGAIN;
        if (practice && inside(demoRect(), x, y)) return Button.DEMO;
        return Button.NONE;
    }

    public static boolean inside(Layout l, double mouseX, double mouseY) {
        return mouseX >= l.x() && mouseX <= l.x() + l.w() && mouseY >= l.y() && mouseY <= l.y() + l.h();
    }

    private static Component ellipsize(Font font, Component text, int width) {
        if (font.width(text) <= width) return text;
        String s = text.getString();
        while (!s.isEmpty() && font.width(s + "…") > width) s = s.substring(0, s.length() - 1);
        return Component.literal(s + "…").withStyle(text.getStyle());
    }

    private PerformancePanel() {}
}
