package com.otectus.immersivespellcraft.compat.ans;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.config.OperationSwitches;
import com.otectus.immersivespellcraft.core.OperationContext;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.session.SessionManager;
import com.otectus.arsnspells.block.GlyphPressBlockEntity;
import com.otectus.arsnspells.menu.GlyphPressMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Glyph Press gate. The Press owns its job: the start (and every native refusal before it), the Source
 * reservation, the input locks, the progress, cancellation, saving and reload handling. The gate only records, for a
 * job started while embossing applies, which job it is and who started it, and holds that job's {@code complete()}
 * until the starter has embossed it. The native duration always runs in full; a cancelled job is gone, so the gate
 * can never make one finish.
 *
 * <p>Stored in the press's Forge persistent data (the press calls {@code super.saveAdditional}), keyed by job id.
 */
public final class PressGate {
    static final String KEY = "immersive_spellcraft";
    static GlyphPressAdapter adapter;

    record Gate(UUID job, UUID starter, boolean performed) {}

    @Nullable
    static Gate read(GlyphPressBlockEntity press) {
        CompoundTag data = press.getPersistentData();
        if (!data.contains(KEY)) return null;
        CompoundTag g = data.getCompound(KEY);
        if (!g.hasUUID("job") || !g.hasUUID("starter")) return null;
        return new Gate(g.getUUID("job"), g.getUUID("starter"), g.getBoolean("performed"));
    }

    static void write(GlyphPressBlockEntity press, Gate gate) {
        CompoundTag g = new CompoundTag();
        g.putUUID("job", gate.job());
        g.putUUID("starter", gate.starter());
        g.putBoolean("performed", gate.performed());
        press.getPersistentData().put(KEY, g);
        press.setChanged();
    }

    static void clear(GlyphPressBlockEntity press) {
        if (press.getPersistentData().contains(KEY)) {
            press.getPersistentData().remove(KEY);
            press.setChanged();
        }
    }

    /** The current job's gate, or null when the job is not gated (or there is no job). */
    @Nullable
    static Gate current(GlyphPressBlockEntity press) {
        UUID job = press.jobId();
        Gate g = read(press);
        return job != null && g != null && g.job().equals(job) ? g : null;
    }

    /** After the Press's own {@code start(player)}: gate a fresh job if embossing applies, and begin it. */
    public static void started(GlyphPressBlockEntity press, ServerPlayer player, String result) {
        if (adapter == null || !GlyphPressBlockEntity.STARTED.equals(result) || press.getLevel() == null || press.getLevel().isClientSide()) return;
        UUID job = press.jobId();
        if (job == null) return;
        if (!SessionManager.requiresPerformance(player, adapter, adapter.query(press))) {
            clear(press);
            return;
        }
        write(press, new Gate(job, player.getUUID(), false));
        ImmersiveSpellcraft.debug("glyph press job {} at {} gated for {}", job, press.getBlockPos().toShortString(), player.getGameProfile().getName());
        if (player.containerMenu instanceof GlyphPressMenu menu && menu.getBlockEntity() == press) {
            SessionManager.BeginResult r = SessionManager.begin(player, OperationContext.menu(player, menu, null), adapter);
            if (r.outcome() == SessionManager.Outcome.REFUSED) {
                ModNetwork.sendTo(player, new Packets.Refused(menu.containerId, r.refusal().code(), r.refusal().message()));
                adapter.menuOpened(player, menu);
            } else if (r.outcome() == SessionManager.Outcome.NATIVE) {
                write(press, new Gate(job, player.getUUID(), true));
            }
        }
    }

    /** At the head of the Press's {@code complete()}: true holds a gated job that is not yet embossed. */
    public static boolean hold(GlyphPressBlockEntity press) {
        if (adapter == null) return false;
        Gate g = current(press);
        if (g == null || g.performed()) return false;
        // Turning embossing off lets a job already waiting finish natively rather than leaving it stranded.
        return OperationSwitches.enabled(adapter.id());
    }

    private PressGate() {}
}
