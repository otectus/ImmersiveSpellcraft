package com.otectus.immersivespellcraft.compat.ans;

import com.otectus.arsnspells.block.GlyphPressBlockEntity;
import com.otectus.arsnspells.glyph.recipe.GlyphPressPlan;
import com.otectus.arsnspells.menu.GlyphPressMenu;
import com.otectus.immersivespellcraft.config.ServerConfig;
import com.otectus.immersivespellcraft.core.AdapterCapability;
import com.otectus.immersivespellcraft.core.Admission;
import com.otectus.immersivespellcraft.core.CommitResult;
import com.otectus.immersivespellcraft.core.Inspection;
import com.otectus.immersivespellcraft.core.InterruptionReason;
import com.otectus.immersivespellcraft.core.InterruptionResult;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.OperationContext;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.core.PreparedOperation;
import com.otectus.immersivespellcraft.core.ProfileQuery;
import com.otectus.immersivespellcraft.core.Refusal;
import com.otectus.immersivespellcraft.core.StationKey;
import com.otectus.immersivespellcraft.core.Validation;
import com.otectus.immersivespellcraft.data.ProfileManager;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Glyph Embossing at the Ars 'n Spells Glyph Press (N01, N02). An existing-native-job adapter: the performance begins
 * when the player's own Inscribe Glyph starts a native job, runs inside that job's processing window, and committing
 * marks the job embossed; the Press then completes it in its own single mutation when its duration is up. Only the
 * job's starter, whose knowledge admitted it, may emboss it.
 */
public final class GlyphPressAdapter implements MagicOperationAdapter {
    public static final int VERSION = 1;
    private static final ResourceLocation BLOCK = new ResourceLocation(Operations.ANS, "glyph_press");

    record Snapshot(GlyphPressMenu menu, GlyphPressBlockEntity press, UUID job, ItemStack glyph) {}

    @Override public ResourceLocation id() { return Operations.ANS_GLYPH_PRESS_ADAPTER; }
    @Override public int version() { return VERSION; }
    @Override public ResourceLocation operation() { return Operations.ANS_GLYPH_PRESS; }
    @Override public String integration() { return Operations.ANS; }
    @Override public AdapterCapability capability() { return AdapterCapability.EXISTING_NATIVE_JOB; }
    @Override public String defaultGame() { return GameTypes.GLYPH_EMBOSSING; }
    @Override public boolean acceptsMenu(AbstractContainerMenu menu) { return menu instanceof GlyphPressMenu; }

    ProfileQuery query(GlyphPressBlockEntity press) {
        GlyphPressPlan plan = press.plan(null);
        List<ItemStack> items = new ArrayList<>();
        if (plan.output() != null && !plan.output().isEmpty()) items.add(plan.output());
        for (int slot = GlyphPressBlockEntity.SLOT_SCROLL; slot <= GlyphPressBlockEntity.SLOT_CATALYST; slot++)
            items.add(press.getItems().getStackInSlot(slot));
        return new ProfileQuery(operation(), id(), BLOCK, null, null, items);
    }

    @Override
    public Inspection inspect(OperationContext context) {
        if (!(context.menu() instanceof GlyphPressMenu menu) || menu.getBlockEntity() == null) return Inspection.NOT_APPLICABLE;
        GlyphPressBlockEntity press = menu.getBlockEntity();
        ProfileQuery query = query(press);
        if (!press.working()) return Inspection.refused(Refusal.of("no_press_job"), query);
        PressGate.Gate gate = PressGate.current(press);
        if (gate == null) return Inspection.refused(Refusal.of("press_not_gated"), query);
        if (gate.performed()) return Inspection.refused(Refusal.of("already_embossed"), query);
        if (!gate.starter().equals(context.player().getUUID())) return Inspection.refused(Refusal.of("not_your_job"), query);
        return Inspection.eligible(query);
    }

    @Override
    public Admission begin(OperationContext context, Inspection inspection) {
        GlyphPressMenu menu = (GlyphPressMenu) context.menu();
        GlyphPressBlockEntity press = menu.getBlockEntity();
        UUID job = press.jobId();
        if (job == null) return Admission.refused(Refusal.of("no_press_job"));
        GlyphPressPlan plan = press.plan(null);
        ItemStack glyph = plan.output() == null ? ItemStack.EMPTY : plan.output().copy();
        int tier = Math.max(1, Math.min(3, plan.tier()));
        List<Component> costs = new ArrayList<>();
        costs.add(Component.translatable("immersive_spellcraft.cost.source_reserved", press.jobSourceCost()));
        costs.add(Component.translatable("immersive_spellcraft.cost.consumes", 1, press.getItems().getStackInSlot(GlyphPressBlockEntity.SLOT_SCROLL).getHoverName()));
        costs.add(Component.translatable("immersive_spellcraft.cost.consumes", 1, press.getItems().getStackInSlot(GlyphPressBlockEntity.SLOT_PLATE).getHoverName()));
        if (plan.terms() != null)
            costs.add(Component.translatable("immersive_spellcraft.cost.consumes", plan.terms().catalystCount(),
                    press.getItems().getStackInSlot(GlyphPressBlockEntity.SLOT_CATALYST).getHoverName()));
        costs.add(Component.translatable("immersive_spellcraft.cost.press_window", press.duration() / 20.0));
        StationKey station = new StationKey(context.level().dimension(), press.getBlockPos());
        PreparedOperation op = PreparedOperation.builder(this, context.player().getUUID(), station)
                .selection(plan.definition() == null ? "" : plan.definition().glyphId().toString())
                .generations(ProfileManager.generation(), ServerConfig.revision())
                .menu(menu, null)
                .nativeJob(job.toString())
                .inputs(List.of(press.getItems().getStackInSlot(GlyphPressBlockEntity.SLOT_SCROLL).copy(),
                        press.getItems().getStackInSlot(GlyphPressBlockEntity.SLOT_PLATE).copy(),
                        press.getItems().getStackInSlot(GlyphPressBlockEntity.SLOT_CATALYST).copy()))
                .output(glyph)
                .title(Component.translatable("immersive_spellcraft.operation.ans_glyph_press.title", glyph.getHoverName()))
                .costs(costs)
                .complexity((tier - 1) / 2.0)
                .nativeWindowMs(Math.max(0, press.duration() - press.progress()) * 50)
                .payload(new Snapshot(menu, press, job, glyph))
                .build();
        return Admission.admitted(op);
    }

    @Override
    public Validation validate(PreparedOperation op, ServerPlayer player) {
        Snapshot s = (Snapshot) op.payload;
        if (player.containerMenu != s.menu() || !s.menu().stillValid(player))
            return Validation.invalid(InterruptionReason.MENU_CLOSED, Refusal.of("menu_closed"));
        GlyphPressBlockEntity press = s.press();
        if (press.isRemoved() || press.getLevel() == null || press.getLevel().getBlockEntity(press.getBlockPos()) != press)
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("station_changed"));
        if (!s.job().equals(press.jobId())) return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("job_changed"));
        PressGate.Gate gate = PressGate.current(press);
        if (gate == null || gate.performed() || !gate.starter().equals(player.getUUID()))
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("job_changed"));
        return Validation.OK;
    }

    @Override
    public CommitResult commit(PreparedOperation op, ServerPlayer player, ScoreCard performance) {
        Snapshot s = (Snapshot) op.payload;
        PressGate.write(s.press(), new PressGate.Gate(s.job(), player.getUUID(), true));
        int remaining = Math.max(0, s.press().duration() - s.press().progress());
        Component summary = remaining > 0
                ? Component.translatable("immersive_spellcraft.result.ans_glyph_press.waiting", s.glyph().getHoverName(), remaining / 20.0)
                : Component.translatable("immersive_spellcraft.result.ans_glyph_press", s.glyph().getHoverName());
        return CommitResult.committed(summary);
    }

    @Override
    public InterruptionResult interrupt(PreparedOperation op, InterruptionReason reason) {
        Snapshot s = (Snapshot) op.payload;
        if (reason == InterruptionReason.STATION_CHANGED || reason == InterruptionReason.SERVER_STOPPING) return InterruptionResult.RELEASED;
        if (reason != InterruptionReason.MENU_CLOSED && ServerLifecycleHooks.getCurrentServer() != null) {
            ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(op.player);
            if (p != null && p.containerMenu == s.menu()) menuOpened(p, s.menu());
        }
        return new InterruptionResult(Component.translatable("immersive_spellcraft.note.press_waits"));
    }

    /** A press job that waits for this player's embossing: the panel offers to resume it. */
    @Override
    public void menuOpened(ServerPlayer player, AbstractContainerMenu menu) {
        if (!(menu instanceof GlyphPressMenu m) || m.getBlockEntity() == null) return;
        PressGate.Gate gate = PressGate.current(m.getBlockEntity());
        if (gate == null || gate.performed()) return;
        boolean mine = gate.starter().equals(player.getUUID());
        ModNetwork.sendTo(player, new Packets.Notice(m.containerId, mine, operation().toString(),
                Component.translatable(mine ? "immersive_spellcraft.notice.press_pending" : "immersive_spellcraft.notice.press_pending_other")));
    }

    @Override
    public Component releaseJob(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof GlyphPressBlockEntity press)) return null;
        PressGate.Gate gate = PressGate.current(press);
        if (gate == null || gate.performed()) return null;
        PressGate.write(press, new PressGate.Gate(gate.job(), gate.starter(), true));
        return Component.translatable("immersive_spellcraft.command.release.done", pos.toShortString());
    }
}
