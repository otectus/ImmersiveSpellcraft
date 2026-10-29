package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.minigame.Autoplay;
import com.otectus.immersivespellcraft.minigame.Control;
import com.otectus.immersivespellcraft.minigame.GameParams;
import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import com.otectus.immersivespellcraft.minigame.Simulation;
import com.otectus.immersivespellcraft.minigame.StageKind;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An isolated practice of any game: the same generators and simulation, run entirely on the client. Nothing is sent to
 * the server and nothing is cast, crafted, consumed, granted or recorded. "Watch" plays a short demonstration.
 */
public class PracticeScreen extends Screen implements PanelModel, PanelInput.Sink {
    private String game;
    private GameSpec spec;
    private Simulation sim;
    private long startedAt;
    private final CueFeed cues = new CueFeed();
    private final PanelInput input = new PanelInput();
    @Nullable private Autoplay demo;
    private Phase phase = Phase.COUNT_IN;
    @Nullable private ScoreCard result;
    private PerformancePanel.Layout layout;

    public PracticeScreen(String game) {
        super(Component.translatable("immersive_spellcraft.screen.practice"));
        this.game = game;
    }

    @Override
    protected void init() {
        layout = PerformancePanel.layout(width, height);
        clearWidgets();
        if (game == null || game.isEmpty() || !GameTypes.known(game)) {
            int y = height / 2 - 34;
            for (String type : List.of(GameTypes.SIGIL_INSCRIPTION, GameTypes.SCROLL_FORGING, GameTypes.GLYPH_EMBOSSING)) {
                addRenderableWidget(Button.builder(Component.translatable(CueFeed.prefix(type) + ".name"), b -> start(type, false))
                        .bounds(width / 2 - 90, y, 180, 20).build());
                y += 24;
            }
        } else if (spec == null) {
            start(game, false);
        }
    }

    public void start(String type, boolean demonstrate) {
        game = type;
        clearWidgets();
        int window = GameTypes.GLYPH_EMBOSSING.equals(type) ? 7000 : 0;
        double assist = Math.max(1, Math.min(2, ClientConfig.get(ClientConfig.TIMING_ASSIST)));
        spec = GameTypes.create(type, new GameParams(0.5, window > 0 ? window : 8000, 20_000, 3, 6,
                ThreadLocalRandom.current().nextLong(), assist, ClientConfig.get(ClientConfig.UNTIMED_MODE), 1.0, window));
        sim = new Simulation(spec);
        demo = demonstrate ? new Autoplay(sim, Autoplay.Style.CLEAN) : null;
        startedAt = Util.getMillis();
        phase = Phase.COUNT_IN;
        result = null;
        input.reset();
        cues.reset(sim);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (spec == null) {
            renderBackground(g);
            g.drawCenteredString(font, Component.translatable("immersive_spellcraft.screen.practice.choose"), width / 2, height / 2 - 56, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable("immersive_spellcraft.panel.practice_note"), width / 2, height / 2 + 46, 0xFFBBBBBB);
            super.render(g, mouseX, mouseY, partial);
            return;
        }
        tickModel();
        if (demo == null && phase == Phase.PERFORMING && layout.inGame(mouseX, mouseY) && ClientConfig.get(ClientConfig.POINTER_ALIGNMENT)
                && sim.currentSpec().kind() == StageKind.ALIGN) pointer(layout.pointer(mouseX));
        PerformancePanel.render(g, this, layout, width, height, mouseX, mouseY);
    }

    private void tickModel() {
        int now = now();
        if (phase == Phase.COUNT_IN && now >= 0) phase = Phase.PERFORMING;
        if (phase == Phase.PERFORMING) {
            if (demo != null) demo.act(Math.max(sim.now(), now));
            else sim.advanceTo(Math.max(sim.now(), now));
            if (sim.finished()) {
                phase = sim.complete() ? Phase.RESULT : Phase.ENDED;
                result = sim.score();
            }
        }
        cues.poll(sim, game);
    }

    @Override
    public void control(Control control, boolean down) {
        if (phase != Phase.PERFORMING || demo != null) return;
        sim.apply(new InputEvent(Math.max(sim.now(), now()), control, down ? 1 : 0));
    }

    private int lastPointer = Integer.MIN_VALUE;

    @Override
    public void pointer(int value) {
        if (Math.abs(value - lastPointer) < 8) return;
        lastPointer = value;
        sim.apply(new InputEvent(Math.max(sim.now(), now()), Control.POINTER, value));
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (spec == null) return super.mouseClicked(x, y, button);
        switch (PerformancePanel.click(layout, x, y, true)) {
            case STOP -> { onClose(); return true; }
            case AGAIN -> { start(game, false); return true; }
            case DEMO -> { start(game, true); return true; }
            default -> {}
        }
        if (button == 0) input.pressed(Control.PRIMARY, PanelInput.MOUSE, this);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (spec != null && button == 0) input.released(Control.PRIMARY, PanelInput.MOUSE, this);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (spec == null || PanelControls.escape(key)) return super.keyPressed(key, scan, modifiers);
        Control c = PanelControls.control(key, scan);
        if (c != null) input.pressed(c, key, this);
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        Control c = PanelControls.control(key, scan);
        if (spec != null && c != null) input.released(c, key, this);
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ---- PanelModel ------------------------------------------------------------------------------------------------

    @Override public GameSpec spec() { return spec; }
    @Override public Simulation simulation() { return sim; }
    @Override public Phase phase() { return phase; }
    @Override public int now() { return (int) (Util.getMillis() - startedAt - spec.countInMs()); }
    @Override public Component title() { return Component.translatable("immersive_spellcraft.screen.practice"); }
    @Override public List<Component> costs() { return List.of(Component.translatable("immersive_spellcraft.panel.practice_costs")); }
    @Override public ItemStack output() { return ItemStack.EMPTY; }
    @Override public boolean practice() { return true; }
    @Override public @Nullable ScoreCard result() { return result; }
    @Override public Component message() {
        return phase == Phase.ENDED ? Component.translatable("immersive_spellcraft.refusal.expired")
                : Component.translatable(demo != null ? "immersive_spellcraft.panel.demo_done" : "immersive_spellcraft.panel.practice_done");
    }
    @Override public String instructionKey() { return GameTypes.keyPrefix(spec.type()) + ".instruction"; }
    @Override public int nativeWindowMs() { return 0; }
    @Override public CueFeed cues() { return cues; }
}
