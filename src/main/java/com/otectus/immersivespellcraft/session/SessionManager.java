package com.otectus.immersivespellcraft.session;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.config.OperationSwitches;
import com.otectus.immersivespellcraft.config.ServerConfig;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.Admission;
import com.otectus.immersivespellcraft.core.ClickRequest;
import com.otectus.immersivespellcraft.core.CommitResult;
import com.otectus.immersivespellcraft.core.Inspection;
import com.otectus.immersivespellcraft.core.InterruptionReason;
import com.otectus.immersivespellcraft.core.InterruptionResult;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.OperationContext;
import com.otectus.immersivespellcraft.core.PreparedOperation;
import com.otectus.immersivespellcraft.core.ProfileQuery;
import com.otectus.immersivespellcraft.core.Refusal;
import com.otectus.immersivespellcraft.core.StationKey;
import com.otectus.immersivespellcraft.core.Validation;
import com.otectus.immersivespellcraft.data.OperationProfile;
import com.otectus.immersivespellcraft.data.ProfileManager;
import com.otectus.immersivespellcraft.data.ResolvedProfile;
import com.otectus.immersivespellcraft.minigame.GameParams;
import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-thread owner of every performance: admission, the station lock (one active operator per station), input
 * validation, authoritative simulation, interruption, and the one native commit under a scoped, one-use permit. The
 * permit exists only for the duration of the adapter's commit call and authorizes exactly the click the player made;
 * it is cleared in {@code finally} and never left on a player.
 */
public final class SessionManager {
    private static final Map<UUID, PerformanceSession> BY_PLAYER = new HashMap<>();
    private static final Map<StationKey, PerformanceSession> BY_STATION = new HashMap<>();
    private static final Map<UUID, Long> LAST_START = new HashMap<>();
    private static final Map<UUID, String> TERMINAL = new LinkedHashMap<>();
    private static final Map<UUID, PlayerPreferences> PREFERENCES = new HashMap<>();
    private static final int TERMINAL_LIMIT = 512;
    private static final int VALIDATE_EVERY_TICKS = 4;
    private static final int MAX_VIOLATIONS = 3;
    private static final int RESYNC_INTERVAL_MS = 400;

    @Nullable private static PerformanceSession committing;
    private static boolean permitUsed;

    public enum Outcome { STARTED, REFUSED, NATIVE, NOT_HANDLED }

    public record BeginResult(Outcome outcome, @Nullable Refusal refusal, @Nullable PerformanceSession session) {
        static final BeginResult NATIVE = new BeginResult(Outcome.NATIVE, null, null);
        static final BeginResult NOT_HANDLED = new BeginResult(Outcome.NOT_HANDLED, null, null);
        static BeginResult refused(Refusal r) { return new BeginResult(Outcome.REFUSED, r, null); }
    }

    record PlayerPreferences(double assist, boolean untimed) {
        static final PlayerPreferences DEFAULT = new PlayerPreferences(1, false);
    }

    // ---- Policy --------------------------------------------------------------------------------------------------

    /**
     * Whether this operation needs a performance now: master, integration and operation switches, exclusions,
     * automation (fake players never perform), the creative bypass and the resolved profile's treatment. {@code player} may be null for a station job with no
     * operator yet (the bypass is then decided when someone performs).
     */
    public static boolean requiresPerformance(@Nullable ServerPlayer player, MagicOperationAdapter adapter, ProfileQuery query) {
        if (!OperationSwitches.enabled(adapter.id()) || OperationSwitches.excluded(query)) return false;
        // Automation (machines acting through fake players) keeps its exact native behavior and is never asked to perform.
        if (player instanceof net.minecraftforge.common.util.FakePlayer) return false;
        if (player != null && player.isCreative() && ServerConfig.get(ServerConfig.CREATIVE_BYPASS)) return false;
        return ProfileManager.resolve(adapter, query).profile().performs();
    }

    // ---- Admission -----------------------------------------------------------------------------------------------

    public static BeginResult begin(ServerPlayer player, OperationContext context, MagicOperationAdapter adapter) {
        long now = SessionClock.now();
        UUID uuid = player.getUUID();
        if (!ServerConfig.enabled()) return BeginResult.NATIVE;
        if (BY_PLAYER.containsKey(uuid)) return BeginResult.refused(Refusal.of("busy"));
        if (!player.isAlive() || player.isSpectator() || player.isSleeping()) return BeginResult.refused(Refusal.of("unavailable"));

        Inspection inspection;
        try {
            inspection = adapter.inspect(context);
        } catch (RuntimeException e) {
            ImmersiveSpellcraft.LOGGER.error("Inspection by {} failed; the station keeps its native behavior", adapter.id(), e);
            return BeginResult.refused(Refusal.of("error"));
        }
        if (!inspection.applicable()) return BeginResult.NOT_HANDLED;
        if (inspection.refusal() != null) return BeginResult.refused(inspection.refusal());
        ProfileQuery query = inspection.query();
        if (query == null || !requiresPerformance(player, adapter, query)) return BeginResult.NATIVE;
        Long last = LAST_START.put(uuid, now);
        if (last != null && now - last < ServerConfig.get(ServerConfig.START_COOLDOWN_MILLIS)) return BeginResult.refused(Refusal.of("too_fast"));
        if (BY_PLAYER.size() >= ServerConfig.get(ServerConfig.MAX_CONCURRENT_SESSIONS)) return BeginResult.refused(Refusal.of("server_busy"));

        ResolvedProfile resolved = ProfileManager.resolve(adapter, query);
        Admission admission;
        try {
            admission = adapter.begin(context, inspection);
        } catch (RuntimeException e) {
            ImmersiveSpellcraft.LOGGER.error("Admission by {} failed before any claim; nothing changed", adapter.id(), e);
            return BeginResult.refused(Refusal.of("error"));
        }
        if (admission.refusal() != null || admission.prepared() == null)
            return BeginResult.refused(admission.refusal() != null ? admission.refusal() : Refusal.of("error"));
        PreparedOperation op = admission.prepared();
        PerformanceSession other = BY_STATION.get(op.station);
        if (other != null) {
            safeInterrupt(adapter, op, InterruptionReason.CANCELLED);
            return BeginResult.refused(Refusal.of("station_busy", other.player.getDisplayName()));
        }

        GameSpec spec;
        try {
            spec = GameTypes.create(resolved.profile().game().type(), params(player, resolved.profile(), op));
        } catch (RuntimeException e) {
            safeInterrupt(adapter, op, InterruptionReason.ERROR);
            ImmersiveSpellcraft.LOGGER.error("Could not generate a performance for {} with profile {}", op.operation, resolved.profile().id(), e);
            return BeginResult.refused(Refusal.of("error"));
        }
        PerformanceSession s = new PerformanceSession(ThreadLocalRandom.current().nextLong(), player, adapter, op, resolved, spec, now);
        BY_PLAYER.put(uuid, s);
        BY_STATION.put(op.station, s);
        String instruction = resolved.profile().presentation().instructionKey() != null
                ? resolved.profile().presentation().instructionKey() : GameTypes.keyPrefix(spec.type()) + ".instruction";
        BlockPos stationPos = op.menu == null ? op.station.pos() : null;
        ModNetwork.sendTo(player, new Packets.Started(s.id, s.nonce, op.menuId, stationPos, op.operation.toString(), spec,
                op.title, op.costs, op.output, op.inputs, instruction, op.nativeWindowMs));
        ImmersiveSpellcraft.debug("session {} started for {}: {} via {} ({}), game {} seed {}, {} stage(s), assist {}, untimed {}",
                s.id, player.getGameProfile().getName(), op.operation, adapter.id(), resolved.reason(), spec.type(), spec.seed(),
                spec.stageCount(), spec.assist(), spec.untimed());
        return new BeginResult(Outcome.STARTED, null, s);
    }

    private static GameParams params(ServerPlayer player, OperationProfile profile, PreparedOperation op) {
        OperationProfile.Game g = profile.game();
        double durations = ServerConfig.get(ServerConfig.DURATION_MULTIPLIER);
        int target = (int) Math.round(Math.max(1000, Math.min(60_000, g.targetDurationSeconds() * 1000 * durations)));
        int max = (int) Math.round(Math.max(target, Math.min(120_000, g.maxDurationSeconds() * 1000 * durations)));
        int cap = ServerConfig.get(ServerConfig.MAX_ACTIONS);
        int maxActions = Math.max(1, Math.min(cap, g.maxActions()));
        int minActions = Math.max(1, Math.min(maxActions, g.minActions()));
        PlayerPreferences pref = PREFERENCES.getOrDefault(player.getUUID(), PlayerPreferences.DEFAULT);
        double assist = g.timingAssistAllowed()
                ? Math.max(1, Math.min(pref.assist(), ServerConfig.get(ServerConfig.MAX_CLIENT_TIMING_ASSIST))) : 1;
        boolean untimed = pref.untimed() && ServerConfig.get(ServerConfig.ALLOW_UNTIMED);
        if (op.nativeWindowMs > 0) max = Math.max(max, Math.min(120_000, op.nativeWindowMs + 15_000));
        return new GameParams(op.complexity, target, max, minActions, maxActions, ThreadLocalRandom.current().nextLong(),
                assist, untimed, ServerConfig.get(ServerConfig.WINDOW_MULTIPLIER), op.nativeWindowMs);
    }

    /** A client request to begin at its open menu: the native click it made, or a station's resume button. */
    public static void beginFromMenu(ServerPlayer player, Packets.Begin message) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != message.containerId() || !menu.stillValid(player)) {
            ModNetwork.sendTo(player, new Packets.Refused(message.containerId(), "changed", Refusal.of("changed").message()));
            return;
        }
        if (menu.getStateId() != message.stateId()) {
            menu.sendAllDataToRemote();
            ModNetwork.sendTo(player, new Packets.Refused(message.containerId(), "changed", Refusal.of("changed").message()));
            return;
        }
        ClickRequest click = null;
        if (message.slot() >= 0 && message.slot() < menu.slots.size() && message.clickType() >= 0
                && message.clickType() < ClickType.values().length)
            click = new ClickRequest(message.slot(), message.button(), ClickType.values()[message.clickType()]);
        Optional<MagicOperationAdapter> adapter = AdapterRegistry.forMenu(menu);
        BeginResult result = adapter.isPresent()
                ? begin(player, OperationContext.menu(player, menu, click), adapter.get()) : BeginResult.NOT_HANDLED;
        switch (result.outcome()) {
            case STARTED -> {}
            case REFUSED -> {
                Refusal r = result.refusal();
                ModNetwork.sendTo(player, new Packets.Refused(message.containerId(), r.code(), r.message()));
                menu.sendAllDataToRemote();
            }
            case NATIVE, NOT_HANDLED -> {
                // No performance applies: the player's own click, exactly as the station would have handled it.
                if (click != null) {
                    menu.clicked(click.slot(), click.button(), click.type(), player);
                    menu.broadcastChanges();
                }
            }
        }
    }

    // ---- Input ---------------------------------------------------------------------------------------------------

    public static void input(ServerPlayer player, Packets.Input message) {
        PerformanceSession s = BY_PLAYER.get(player.getUUID());
        if (s == null || s.player != player || !s.id.equals(message.session()) || s.nonce != message.nonce()) return;
        if (s.state != SessionState.RESERVED && s.state != SessionState.PERFORMING) return;
        try {
            long now = SessionClock.now();
            int elapsed = s.performElapsed(now);
            if (message.stageHint() > s.simulation.stageIndex() + 1) {
                violation(s, "impossible stage " + message.stageHint());
                return;
            }
            InputValidator.Verdict v = s.inputs.check(message.sequence(), message.events(), elapsed, s.simulation.now(),
                    ServerConfig.get(ServerConfig.MAX_INPUT_EVENTS_PER_SECOND), now);
            if (v.kind() == InputValidator.Kind.DUPLICATE) return;
            if (v.kind() == InputValidator.Kind.INVALID) {
                violation(s, v.why());
                return;
            }
            if (elapsed >= 0) s.state = SessionState.PERFORMING;
            for (InputEvent e : v.events()) if (s.simulation.apply(e)) s.applied.add(e);
            s.lastActivity = now;
            if (v.adjusted() && now - s.lastResyncAt >= RESYNC_INTERVAL_MS) {
                s.lastResyncAt = now;
                List<InputEvent> applied = s.applied.size() > 1024 ? s.applied.subList(s.applied.size() - 1024, s.applied.size()) : s.applied;
                ModNetwork.sendTo(player, new Packets.Resync(s.id, List.copyOf(applied)));
            }
            afterProgress(s);
        } catch (RuntimeException e) {
            ImmersiveSpellcraft.LOGGER.error("Input for session {} failed before any commit", s.id, e);
            end(s, InterruptionReason.ERROR, null);
        }
    }

    private static void violation(PerformanceSession s, String why) {
        s.violations++;
        ImmersiveSpellcraft.debug("session {} rejected input from {}: {}", s.id, s.player.getGameProfile().getName(), why);
        if (s.violations > MAX_VIOLATIONS) end(s, InterruptionReason.INVALID_INPUT, null);
    }

    private static void afterProgress(PerformanceSession s) {
        int stage = s.simulation.stageIndex();
        if (stage != s.lastStageSent && !s.simulation.complete()) {
            s.lastStageSent = stage;
            worldCue(s, false);
            ModNetwork.sendTo(s.player, new Packets.Update(s.id, Packets.Status.PROGRESS, stage, s.inputs.acknowledged(), "",
                    Component.empty(), null));
        }
        if (s.simulation.complete()) commit(s);
        else if (s.simulation.expired()) end(s, InterruptionReason.EXPIRED, null);
    }

    // ---- Ticking and interruption --------------------------------------------------------------------------------

    public static void tick() {
        if (BY_PLAYER.isEmpty()) return;
        long now = SessionClock.now();
        long idle = ServerConfig.get(ServerConfig.IDLE_TIMEOUT_SECONDS) * 1000L;
        for (PerformanceSession s : List.copyOf(BY_PLAYER.values())) {
            if (s.state.terminal() || s.state == SessionState.COMMITTING) continue;
            try {
                if (s.player.hasDisconnected()) { end(s, InterruptionReason.DISCONNECTED, null); continue; }
                if (s.profile.generation() != ProfileManager.generation()) { end(s, InterruptionReason.RELOAD, null); continue; }
                if (s.configRevision != ServerConfig.revision()) { end(s, InterruptionReason.CONFIG_CHANGED, null); continue; }
                if (s.ticks++ % VALIDATE_EVERY_TICKS == 0) {
                    Validation v = s.adapter.validate(s.operation, s.player);
                    if (!v.valid()) { end(s, v.reason(), v.refusal()); continue; }
                }
                if (now - s.lastActivity > idle) { end(s, InterruptionReason.IDLE, null); continue; }
                int elapsed = s.performElapsed(now);
                if (elapsed >= 0) {
                    s.state = SessionState.PERFORMING;
                    int watermark = elapsed - s.maxLagMs;
                    if (watermark > s.simulation.now()) s.simulation.advanceTo(watermark);
                    afterProgress(s);
                }
            } catch (RuntimeException e) {
                ImmersiveSpellcraft.LOGGER.error("Session {} invalidated before its commit", s.id, e);
                end(s, InterruptionReason.ERROR, null);
            }
        }
    }

    public static void cancelRequest(ServerPlayer player, Packets.Cancel message) {
        PerformanceSession s = BY_PLAYER.get(player.getUUID());
        if (s != null && s.player == player && s.id.equals(message.session()) && s.state != SessionState.COMMITTING)
            end(s, InterruptionReason.CANCELLED, null);
    }

    public static void preferences(ServerPlayer player, Packets.Preferences message) {
        double assist = Float.isFinite(message.timingAssist()) ? Math.max(1, Math.min(2, message.timingAssist())) : 1;
        PREFERENCES.put(player.getUUID(), new PlayerPreferences(assist, message.untimed()));
    }

    /** Ends {@code player}'s session for {@code reason}, if it has one that is not committing. */
    public static boolean interrupt(ServerPlayer player, InterruptionReason reason) {
        PerformanceSession s = BY_PLAYER.get(player.getUUID());
        if (s == null || s.state == SessionState.COMMITTING) return false;
        end(s, reason, null);
        return true;
    }

    public static void menuClosed(ServerPlayer player, AbstractContainerMenu menu) {
        PerformanceSession s = BY_PLAYER.get(player.getUUID());
        if (s != null && s.operation.menu == menu && s.state != SessionState.COMMITTING) end(s, InterruptionReason.MENU_CLOSED, null);
    }

    public static void logout(ServerPlayer player) {
        interrupt(player, InterruptionReason.DISCONNECTED);
        LAST_START.remove(player.getUUID());
        PREFERENCES.remove(player.getUUID());
    }

    public static void clear() {
        for (PerformanceSession s : List.copyOf(BY_PLAYER.values())) {
            safeInterrupt(s.adapter, s.operation, InterruptionReason.SERVER_STOPPING);
            s.state = SessionState.RELEASED;
        }
        BY_PLAYER.clear();
        BY_STATION.clear();
        LAST_START.clear();
        TERMINAL.clear();
        PREFERENCES.clear();
        committing = null;
        permitUsed = false;
    }

    // ---- Commit --------------------------------------------------------------------------------------------------

    private static void commit(PerformanceSession s) {
        s.state = SessionState.READY;
        Validation v;
        try {
            v = s.adapter.validate(s.operation, s.player);
        } catch (RuntimeException e) {
            ImmersiveSpellcraft.LOGGER.error("Final validation of session {} failed; nothing moved", s.id, e);
            end(s, InterruptionReason.ERROR, null);
            return;
        }
        if (!v.valid()) {
            end(s, v.reason(), v.refusal());
            return;
        }
        s.state = SessionState.COMMITTING;
        ScoreCard score = s.simulation.score();
        CommitResult result;
        committing = s;
        permitUsed = false;
        try {
            result = s.adapter.commit(s.operation, s.player, score);
        } catch (Throwable t) {
            ImmersiveSpellcraft.LOGGER.error("Commit of session {} ({}) threw; the actual inventory and station state are kept "
                    + "and nothing is reissued", s.id, s.operation.operation, t);
            result = CommitResult.held(Refusal.of("held"));
        } finally {
            committing = null;
            permitUsed = false;
        }
        switch (result.status()) {
            case COMMITTED -> {
                s.state = SessionState.COMPLETED;
                worldCue(s, true);
                finish(s, Packets.Status.COMPLETE, "", result.summary(), score);
            }
            case REFUSED -> {
                safeInterrupt(s.adapter, s.operation, InterruptionReason.STATION_CHANGED);
                s.state = SessionState.RELEASED;
                Refusal r = result.refusal() != null ? result.refusal() : Refusal.of("changed");
                finish(s, Packets.Status.CANCELLED, r.code(), r.message(), null);
            }
            case HELD -> {
                s.state = SessionState.HELD;
                Refusal r = result.refusal() != null ? result.refusal() : Refusal.of("held");
                ImmersiveSpellcraft.LOGGER.warn("Session {} held: {}", s.id, r.code());
                finish(s, Packets.Status.HELD, r.code(), r.message(), null);
            }
        }
    }

    /**
     * The commit permit: true exactly once, for the committing session's own player, menu and recorded click. The
     * menu click guard calls this; everything else in a locked menu stays blocked.
     */
    public static boolean permitsClick(Player player, AbstractContainerMenu menu, int slot, int button, ClickType type) {
        PerformanceSession s = committing;
        if (s == null || permitUsed || s.player != player || s.operation.menu != menu || s.operation.click == null) return false;
        ClickRequest c = s.operation.click;
        if (c.slot() != slot || c.button() != button || c.type() != type) return false;
        permitUsed = true;
        return true;
    }

    /** Whether the commit permit is in use right now (for adapters' own internal re-entry checks). */
    public static boolean committing(MagicOperationAdapter adapter) {
        return committing != null && committing.adapter == adapter;
    }

    private static void end(PerformanceSession s, InterruptionReason reason, @Nullable Refusal refusal) {
        if (s.state.terminal()) return;
        InterruptionResult r = safeInterrupt(s.adapter, s.operation, reason);
        s.state = SessionState.RELEASED;
        Component message = refusal != null ? refusal.message() : Refusal.of(reason.code()).message();
        if (r.note() != null) message = message.copy().append(" ").append(r.note());
        finish(s, reason == InterruptionReason.EXPIRED ? Packets.Status.EXPIRED : Packets.Status.CANCELLED,
                refusal != null ? refusal.code() : reason.code(), message, null);
    }

    private static InterruptionResult safeInterrupt(MagicOperationAdapter adapter, PreparedOperation op, InterruptionReason reason) {
        try {
            InterruptionResult r = adapter.interrupt(op, reason);
            return r == null ? InterruptionResult.RELEASED : r;
        } catch (RuntimeException e) {
            ImmersiveSpellcraft.LOGGER.error("Releasing {} for {} failed", op.operation, reason, e);
            return InterruptionResult.RELEASED;
        }
    }

    private static void finish(PerformanceSession s, Packets.Status status, String code, Component message, @Nullable ScoreCard score) {
        BY_PLAYER.remove(s.player.getUUID(), s);
        BY_STATION.remove(s.operation.station, s);
        TERMINAL.put(s.id, s.state.name());
        if (TERMINAL.size() > TERMINAL_LIMIT) TERMINAL.remove(TERMINAL.keySet().iterator().next());
        ModNetwork.sendTo(s.player, new Packets.Update(s.id, status, s.simulation.stageIndex(), s.inputs.acknowledged(), code,
                message, score));
        ImmersiveSpellcraft.debug("session {} {} ({}) after {} ms, {} input(s){}", s.id, status, code.isEmpty() ? "-" : code,
                s.simulation.now(), s.applied.size(), score == null ? "" : ", quality " + score.quality() + " " + score.grade());
    }

    private static void worldCue(PerformanceSession s, boolean complete) {
        if (!(s.player.level() instanceof ServerLevel level) || !level.dimension().equals(s.operation.station.dimension())) return;
        BlockPos pos = s.operation.station.pos();
        if (complete) {
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
            level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 18, 0.35, 0.25, 0.35, 0.6);
        } else {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.45f, 0.8f + 0.15f * s.simulation.stageIndex());
        }
    }

    // ---- Views ---------------------------------------------------------------------------------------------------

    public static Optional<PerformanceSession> session(Player player) {
        return Optional.ofNullable(BY_PLAYER.get(player.getUUID()));
    }

    @Nullable
    public static PerformanceSession atStation(StationKey key) {
        return BY_STATION.get(key);
    }

    /** Whether {@code menu} is locked because its own player is performing in it. */
    public static boolean locked(Player player, AbstractContainerMenu menu) {
        PerformanceSession s = BY_PLAYER.get(player.getUUID());
        return s != null && s.operation.menu == menu;
    }

    public static List<PerformanceSession> sessions() {
        return new ArrayList<>(BY_PLAYER.values());
    }

    private SessionManager() {}
}
