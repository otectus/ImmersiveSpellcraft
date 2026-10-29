package com.otectus.immersivespellcraft.session;

import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.PreparedOperation;
import com.otectus.immersivespellcraft.data.ResolvedProfile;
import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.minigame.Simulation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One server-side performance. Held only in memory and only on the server thread; it is never saved, and it holds a
 * live player only while that player is connected (disconnect releases it at once).
 */
public final class PerformanceSession {
    public final UUID id = UUID.randomUUID();
    public final long nonce;
    public final ServerPlayer player;
    public final MagicOperationAdapter adapter;
    public final PreparedOperation operation;
    public final ResolvedProfile profile;
    public final GameSpec spec;
    public final Simulation simulation;
    public final long createdAt;
    public final int maxLagMs;
    /** The server config revision this session was planned under; any reload releases it. */
    public final int configRevision = com.otectus.immersivespellcraft.config.ServerConfig.revision();
    final InputValidator inputs = new InputValidator();
    final List<InputEvent> applied = new ArrayList<>();
    SessionState state = SessionState.RESERVED;
    long lastActivity;
    int ticks;
    int violations;
    int lastStageSent;
    long lastResyncAt;

    PerformanceSession(long nonce, ServerPlayer player, MagicOperationAdapter adapter, PreparedOperation operation,
                       ResolvedProfile profile, GameSpec spec, long now) {
        this.nonce = nonce;
        this.player = player;
        this.adapter = adapter;
        this.operation = operation;
        this.profile = profile;
        this.spec = spec;
        this.simulation = new Simulation(spec);
        this.createdAt = now;
        this.lastActivity = now;
        // Inputs that arrive within this lag are applied at exactly the time the client recorded them.
        this.maxLagMs = Math.max(300, Math.min(1500, player.latency * 2 + 200));
    }

    /** Milliseconds since the performance began; negative during the count-in. */
    public int performElapsed(long now) {
        return (int) Math.max(Integer.MIN_VALUE / 2, Math.min(Integer.MAX_VALUE / 2, now - createdAt - spec.countInMs()));
    }

    public SessionState state() {
        return state;
    }

    public int appliedInputs() {
        return applied.size();
    }
}
