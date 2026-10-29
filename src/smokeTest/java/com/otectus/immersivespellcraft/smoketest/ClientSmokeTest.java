package com.otectus.immersivespellcraft.smoketest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.otectus.immersivespellcraft.client.PanelModel;
import com.otectus.immersivespellcraft.client.PracticeScreen;
import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Plays every practice game on the title screen with the demonstration inputs, in the default look and then in high
 * contrast, saving a screenshot mid-performance and one of the result, writes {@code smoketest-result.json}, and quits.
 * It drives the real client panel, renderer, input model and simulation; it does not touch a world or a server.
 */
public final class ClientSmokeTest {
    private record Step(String game, boolean highContrast) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static final JsonArray RESULTS = new JsonArray();
    private static boolean started;
    private static int step = -1, ticks;
    private static boolean midShot;

    static {
        for (String g : List.of(GameTypes.SIGIL_INSCRIPTION, GameTypes.SCROLL_FORGING, GameTypes.GLYPH_EMBOSSING)) STEPS.add(new Step(g, false));
        STEPS.add(new Step(GameTypes.GLYPH_EMBOSSING, true));
    }

    @SubscribeEvent
    public static void titleOpened(ScreenEvent.Init.Post e) {
        if (!started && e.getScreen() instanceof TitleScreen) {
            started = true;
            next();
        }
    }

    private static void next() {
        step++;
        ticks = 0;
        midShot = false;
        Minecraft mc = Minecraft.getInstance();
        if (step >= STEPS.size()) {
            finish();
            return;
        }
        Step s = STEPS.get(step);
        ClientConfig.HIGH_CONTRAST.set(s.highContrast());
        PracticeScreen screen = new PracticeScreen(s.game());
        mc.setScreen(screen);
        screen.start(s.game(), true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || step < 0 || step >= STEPS.size()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof PracticeScreen screen)) return;
        ticks++;
        Step s = STEPS.get(step);
        String name = s.game().substring(s.game().indexOf(':') + 1) + (s.highContrast() ? "_contrast" : "");
        if (!midShot && ticks == 70) {
            midShot = true;
            shot("smoketest_" + name + "_performing.png");
        }
        PanelModel.Phase phase = screen.phase();
        boolean done = phase == PanelModel.Phase.RESULT || phase == PanelModel.Phase.ENDED;
        if ((done && ticks > 90) || ticks > 900) {
            shot("smoketest_" + name + "_result.png");
            ScoreCard card = screen.result();
            JsonObject o = new JsonObject();
            o.addProperty("game", s.game());
            o.addProperty("high_contrast", s.highContrast());
            o.addProperty("phase", phase.name());
            o.addProperty("complete", card != null && card.complete());
            o.addProperty("quality", card == null ? -1 : card.quality());
            o.addProperty("grade", card == null ? "" : card.grade().key());
            o.addProperty("elapsed_ms", card == null ? -1 : card.elapsedMs());
            o.addProperty("ticks", ticks);
            RESULTS.add(o);
            next();
        }
    }

    private static void shot(String file) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), m -> {});
    }

    private static void finish() {
        Minecraft mc = Minecraft.getInstance();
        ClientConfig.HIGH_CONTRAST.set(false);
        try {
            String dir = System.getProperty("immersive_spellcraft.smoketest.output", new File(mc.gameDirectory, "smoketest").getAbsolutePath());
            Path out = Path.of(dir, "smoketest-result.json");
            Files.createDirectories(out.getParent());
            JsonObject root = new JsonObject();
            root.add("runs", RESULTS);
            boolean ok = RESULTS.size() == STEPS.size();
            for (var r : RESULTS) ok &= r.getAsJsonObject().get("complete").getAsBoolean();
            root.addProperty("passed", ok);
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mc.stop();
    }

    private ClientSmokeTest() {}
}
