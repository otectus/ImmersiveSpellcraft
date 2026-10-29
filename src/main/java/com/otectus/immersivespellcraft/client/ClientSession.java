package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.minigame.Control;
import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import com.otectus.immersivespellcraft.minigame.Simulation;
import com.otectus.immersivespellcraft.minigame.StageKind;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.network.SpecCodec;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client's one live performance. It predicts with the same simulation the server judges, sends only control edges
 * (batched each tick, numbered), and always defers to the server's result. Pausing the game pauses its clock.
 */
public final class ClientSession implements PanelModel, PanelInput.Sink {
    @Nullable private static ClientSession current;
    private static final Map<Integer, Packets.Notice> NOTICES = new HashMap<>();
    @Nullable private static Packets.Refused refused;
    private static long refusedAt;

    private final Packets.Started start;
    private Simulation sim;
    private final long startedAt = Util.getMillis();
    private final CueFeed cues = new CueFeed();
    final PanelInput input = new PanelInput();
    private final List<InputEvent> outbox = new ArrayList<>();
    private int sequence;
    private Phase phase = Phase.COUNT_IN;
    @Nullable private ScoreCard result;
    private Component message = Component.empty();
    private long endedAt;
    private long pausedTotal, pausedSince = -1;
    private int lastPointer = Integer.MIN_VALUE;
    private long lastPointerAt;
    private String endCode = "";

    private ClientSession(Packets.Started start) {
        this.start = start;
        this.sim = new Simulation(start.spec());
    }

    // ---- Static entry points (from packets and events) ----------------------------------------------------------

    static void started(Packets.Started m) {
        if (m.spec().generatorVersion() != GameTypes.GENERATOR_VERSION) {
            ModNetwork.sendToServer(new Packets.Cancel(m.session()));
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("immersive_spellcraft.refusal.version"), false);
            return;
        }
        current = new ClientSession(m);
        NOTICES.remove(m.containerId());
        refused = null;
        if (m.containerId() < 0) Minecraft.getInstance().setScreen(new PerformanceScreen());
    }

    static void update(Packets.Update u) {
        ClientSession s = current;
        if (s == null || !s.start.session().equals(u.session())) return;
        switch (u.status()) {
            case PROGRESS -> {}
            case COMPLETE -> s.finish(Phase.RESULT, u.message(), u.score(), "");
            default -> s.finish(Phase.ENDED, u.message(), null, u.code());
        }
    }

    static void resync(Packets.Resync r) {
        ClientSession s = current;
        if (s == null || !s.start.session().equals(r.session())) return;
        Simulation rebuilt = new Simulation(s.start.spec());
        for (InputEvent e : r.applied()) rebuilt.apply(e);
        s.sim = rebuilt;
        s.cues.reset(rebuilt);
    }

    static void refused(Packets.Refused r) {
        refused = r;
        refusedAt = Util.getMillis();
        if (Minecraft.getInstance().screen == null) Minecraft.getInstance().gui.setOverlayMessage(r.message(), false);
    }

    static void notice(Packets.Notice n) {
        if (n.pending()) NOTICES.put(n.containerId(), n);
        else NOTICES.remove(n.containerId());
    }

    @Nullable
    public static ClientSession current() {
        return current;
    }

    /** The session drawn over the open menu {@code containerId}, if any. */
    @Nullable
    public static ClientSession forMenu(int containerId) {
        ClientSession s = current;
        return s != null && s.start.containerId() >= 0 && s.start.containerId() == containerId ? s : null;
    }

    public static boolean lockedMenu(int containerId) {
        ClientSession s = forMenu(containerId);
        return s != null && (s.phase == Phase.COUNT_IN || s.phase == Phase.PERFORMING || s.phase == Phase.FINISHING);
    }

    @Nullable
    public static Packets.Notice pending(int containerId) {
        return current == null ? NOTICES.get(containerId) : null;
    }

    @Nullable
    public static Component refusal(int containerId) {
        Packets.Refused r = refused;
        if (r == null || Util.getMillis() - refusedAt > 4000 || (r.containerId() != containerId && r.containerId() >= 0)) return null;
        return r.message();
    }

    public static void clientTick() {
        ClientSession s = current;
        if (s == null) return;
        s.frame();
        s.flush();
        if ((s.phase == Phase.RESULT || s.phase == Phase.ENDED)
                && Util.getMillis() - s.endedAt > ClientConfig.get(ClientConfig.RESULT_SECONDS) * 1000L) {
            s.dismiss();
        }
    }

    public static void clearAll() {
        current = null;
        NOTICES.clear();
        refused = null;
    }

    public static void menuGone(int containerId) {
        NOTICES.remove(containerId);
        ClientSession s = forMenu(containerId);
        if (s != null) {
            if (s.phase == Phase.RESULT || s.phase == Phase.ENDED) current = null;
            else s.message = Component.translatable("immersive_spellcraft.refusal.menu_closed");
        }
    }

    // ---- Instance --------------------------------------------------------------------------------------------------

    private long clientMillis() {
        long now = Util.getMillis();
        boolean paused = Minecraft.getInstance().isPaused();
        if (paused && pausedSince < 0) pausedSince = now;
        else if (!paused && pausedSince >= 0) {
            pausedTotal += now - pausedSince;
            pausedSince = -1;
        }
        return now - pausedTotal - (pausedSince >= 0 ? now - pausedSince : 0);
    }

    void frame() {
        int now = now();
        if (phase == Phase.COUNT_IN && now >= 0) {
            phase = Phase.PERFORMING;
            for (Control c : new Control[]{Control.PRIMARY, Control.LEFT, Control.RIGHT})
                if (input.held(c)) emit(new InputEvent(0, c, 1));
        }
        if (phase == Phase.PERFORMING) {
            sim.advanceTo(Math.max(sim.now(), now));
            if (sim.finished()) {
                phase = Phase.FINISHING;
                flush();
            }
        }
        cues.poll(sim, start.spec().type());
    }

    private void emit(InputEvent e) {
        if (sim.apply(e)) outbox.add(e);
    }

    private void flush() {
        while (!outbox.isEmpty()) {
            int n = Math.min(outbox.size(), SpecCodec.MAX_EVENTS_PER_PACKET);
            List<InputEvent> batch = List.copyOf(outbox.subList(0, n));
            outbox.subList(0, n).clear();
            ModNetwork.sendToServer(new Packets.Input(start.session(), start.nonce(), sequence++, sim.stageIndex(), batch));
        }
    }

    @Override
    public void control(Control control, boolean down) {
        if (phase != Phase.PERFORMING) return;
        frame();
        if (phase != Phase.PERFORMING) return;
        emit(new InputEvent(Math.max(sim.now(), now()), control, down ? 1 : 0));
        if (sim.finished()) {
            phase = Phase.FINISHING;
            flush();
        }
    }

    @Override
    public void pointer(int value) {
        if (phase != Phase.PERFORMING || !ClientConfig.get(ClientConfig.POINTER_ALIGNMENT)) return;
        if (sim.currentSpec().kind() != StageKind.ALIGN) return;
        long t = Util.getMillis();
        if (Math.abs(value - lastPointer) < 8 || t - lastPointerAt < 40) return;
        lastPointer = value;
        lastPointerAt = t;
        emit(new InputEvent(Math.max(sim.now(), now()), Control.POINTER, value));
    }

    /** Stop the performance: nothing is lost, the station and any native job are untouched. */
    public void stop() {
        if (phase == Phase.RESULT || phase == Phase.ENDED) {
            dismiss();
            return;
        }
        ModNetwork.sendToServer(new Packets.Cancel(start.session()));
        finish(Phase.ENDED, Component.translatable("immersive_spellcraft.refusal.cancelled"), null, "cancelled");
    }

    void dismiss() {
        if (current == this) current = null;
        if (start.containerId() < 0 && Minecraft.getInstance().screen instanceof PerformanceScreen screen) screen.onClose();
    }

    private void finish(Phase phase, Component message, @Nullable ScoreCard score, String code) {
        if (this.phase == Phase.RESULT) return;
        this.phase = phase;
        this.message = message;
        this.result = score;
        this.endCode = code;
        this.endedAt = Util.getMillis();
        input.reset();
        boolean screenGone = start.containerId() >= 0 ? forMenuOpen() : !(Minecraft.getInstance().screen instanceof PerformanceScreen);
        if (screenGone || "attacked".equals(code)) {
            Minecraft.getInstance().gui.setOverlayMessage(message, false);
            dismiss();
        }
    }

    private boolean forMenuOpen() {
        var player = Minecraft.getInstance().player;
        return player == null || player.containerMenu.containerId != start.containerId();
    }

    public int containerId() {
        return start.containerId();
    }

    // ---- PanelModel ------------------------------------------------------------------------------------------------

    @Override public GameSpec spec() { return start.spec(); }
    @Override public Simulation simulation() { return sim; }
    @Override public Phase phase() { return phase; }
    @Override public int now() { return (int) (clientMillis() - startedAt - start.spec().countInMs()); }
    @Override public Component title() { return start.title(); }
    @Override public List<Component> costs() { return start.costs(); }
    @Override public ItemStack output() { return start.output(); }
    @Override public boolean practice() { return false; }
    @Override public @Nullable ScoreCard result() { return result; }
    @Override public Component message() { return message; }
    @Override public String instructionKey() { return start.instructionKey(); }
    @Override public int nativeWindowMs() { return start.nativeWindowMs(); }
    @Override public CueFeed cues() { return cues; }

    public UUID id() {
        return start.session();
    }
}
