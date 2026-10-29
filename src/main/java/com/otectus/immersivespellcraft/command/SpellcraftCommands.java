package com.otectus.immersivespellcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.compat.CompatMixinPlugin;
import com.otectus.immersivespellcraft.compat.CompatibilityBootstrap;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.Inspection;
import com.otectus.immersivespellcraft.core.InterruptionReason;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.OperationContext;
import com.otectus.immersivespellcraft.data.CoverageCatalog;
import com.otectus.immersivespellcraft.data.CoverageExporter;
import com.otectus.immersivespellcraft.data.ProfileManager;
import com.otectus.immersivespellcraft.data.ResolvedProfile;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.network.ModNetwork;
import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.session.PerformanceSession;
import com.otectus.immersivespellcraft.session.SessionClock;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.Optional;

/**
 * {@code /immersivespellcraft}: integrations, coverage, inspect, sessions, cancel, release and practice. Reading
 * commands are open to every player; anything that touches another player's session or a held job needs permission
 * level 2. Recovery never moves an item itself: {@code release} only lets a held native job finish natively, and is
 * logged.
 */
public final class SpellcraftCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("immersivespellcraft")
                .then(Commands.literal("integrations").executes(SpellcraftCommands::integrations))
                .then(Commands.literal("coverage").executes(SpellcraftCommands::coverage)
                        .then(Commands.literal("export").requires(s -> s.hasPermission(2)).executes(SpellcraftCommands::export)))
                .then(Commands.literal("inspect").requires(s -> s.hasPermission(2)).executes(SpellcraftCommands::inspect))
                .then(Commands.literal("sessions").requires(s -> s.hasPermission(2)).executes(SpellcraftCommands::sessions))
                .then(Commands.literal("cancel").executes(c -> cancel(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                                .executes(c -> cancel(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("release").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(SpellcraftCommands::release)))
                .then(Commands.literal("practice").executes(c -> practice(c, ""))
                        .then(Commands.argument("game", StringArgumentType.greedyString())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(GameTypes.ALL, b))
                                .executes(c -> practice(c, StringArgumentType.getString(c, "game"))))));
    }

    private static int integrations(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> Component.translatable("immersive_spellcraft.command.integrations.header").withStyle(ChatFormatting.GOLD), false);
        CompatibilityBootstrap.integrations().values().forEach(i -> s.sendSuccess(() -> Component.literal(" " + i.modId() + " ")
                .append(Component.literal(i.status().loaded() ? i.status().version() : "absent").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(i.active() ? " active" : " dormant").withStyle(i.active() ? ChatFormatting.GREEN : ChatFormatting.YELLOW))
                .append(Component.literal(" - " + i.detail() + " (supported " + i.status().range() + ")").withStyle(ChatFormatting.GRAY)), false));
        for (MagicOperationAdapter a : AdapterRegistry.all())
            s.sendSuccess(() -> Component.literal(" " + a.operation() + " -> " + a.id() + " (" + a.capability().name().toLowerCase() + ")"), false);
        if (AdapterRegistry.all().isEmpty())
            s.sendSuccess(() -> Component.translatable("immersive_spellcraft.command.integrations.none").withStyle(ChatFormatting.YELLOW), false);
        CompatMixinPlugin.decisions().forEach((m, applied) -> s.sendSuccess(() -> Component.literal(" mixin " + m + ": "
                + (applied ? "applied" : "not applied")).withStyle(ChatFormatting.DARK_GRAY), false));
        AdapterRegistry.diagnostics().forEach(d -> s.sendSuccess(() -> Component.literal(" ! " + d).withStyle(ChatFormatting.RED), false));
        return AdapterRegistry.all().size();
    }

    private static int coverage(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> Component.translatable("immersive_spellcraft.command.coverage.summary",
                CoverageCatalog.ROWS.size(), CoverageCatalog.count(CoverageCatalog.Status.PERFORMED),
                CoverageCatalog.count(CoverageCatalog.Status.ATTUNED), CoverageCatalog.count(CoverageCatalog.Status.NATIVE_PRESERVED),
                CoverageCatalog.count(CoverageCatalog.Status.UNSUPPORTED)), false);
        CoverageCatalog.ROWS.stream().filter(r -> r.status() != CoverageCatalog.Status.UNSUPPORTED)
                .forEach(r -> s.sendSuccess(() -> Component.literal(" " + r.id() + " " + r.status().name().toLowerCase() + ": " + r.system())
                        .withStyle(ChatFormatting.GRAY), false));
        return CoverageCatalog.ROWS.size();
    }

    private static int export(CommandContext<CommandSourceStack> c) {
        Path target = c.getSource().getServer().getServerDirectory().toPath().resolve(ImmersiveSpellcraft.MOD_ID).resolve("coverage.json");
        try {
            Path out = CoverageExporter.export(c.getSource().getServer(), target);
            c.getSource().sendSuccess(() -> Component.translatable("immersive_spellcraft.command.coverage.exported", out.toString()), true);
            return 1;
        } catch (Exception e) {
            c.getSource().sendFailure(Component.literal(e.toString()));
            return 0;
        }
    }

    private static int inspect(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        CommandSourceStack s = c.getSource();
        Optional<MagicOperationAdapter> byMenu = AdapterRegistry.forMenu(player.containerMenu);
        OperationContext context = null;
        MagicOperationAdapter adapter = null;
        if (byMenu.isPresent()) {
            adapter = byMenu.get();
            context = OperationContext.menu(player, player.containerMenu, null);
        } else {
            Vec3 eye = player.getEyePosition();
            BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(6)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockEntity be = player.level().getBlockEntity(hit.getBlockPos());
                if (be != null) {
                    adapter = AdapterRegistry.forBlock(be).orElse(null);
                    context = OperationContext.block(player, hit.getBlockPos());
                }
            }
        }
        if (adapter == null) {
            s.sendSuccess(() -> Component.translatable("immersive_spellcraft.command.inspect.nothing"), false);
            return 0;
        }
        MagicOperationAdapter a = adapter;
        Inspection inspection = a.inspect(context);
        s.sendSuccess(() -> Component.literal(a.operation() + " via " + a.id() + " (" + a.capability().name().toLowerCase() + ")")
                .withStyle(ChatFormatting.GOLD), false);
        if (!inspection.applicable()) s.sendSuccess(() -> Component.translatable("immersive_spellcraft.command.inspect.not_applicable"), false);
        else if (inspection.refusal() != null) s.sendSuccess(() -> Component.literal(" refused (" + inspection.refusal().code() + "): ")
                .append(inspection.refusal().message()), false);
        if (inspection.query() != null) {
            ResolvedProfile r = ProfileManager.resolve(a, inspection.query());
            boolean required = SessionManager.requiresPerformance(player, a, inspection.query());
            s.sendSuccess(() -> Component.literal(" profile " + r.profile().id() + ": " + r.reason() + ", treatment "
                    + r.profile().treatment().id() + (r.profile().game() != null ? ", game " + r.profile().game().type() : "")
                    + ", performance " + (required ? "required" : "not required")), false);
            s.sendSuccess(() -> Component.literal(" recipe " + inspection.query().recipe() + ", type " + inspection.query().recipeType()
                    + ", block " + inspection.query().block()).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int sessions(CommandContext<CommandSourceStack> c) {
        long now = SessionClock.now();
        var all = SessionManager.sessions();
        c.getSource().sendSuccess(() -> Component.translatable("immersive_spellcraft.command.sessions.header", all.size()), false);
        for (PerformanceSession s : all)
            c.getSource().sendSuccess(() -> Component.literal(" " + s.player.getGameProfile().getName() + " " + s.operation.operation
                    + " at " + s.operation.station + ": " + s.state().name().toLowerCase() + ", stage " + (s.simulation.stageIndex() + 1)
                    + "/" + s.spec.stageCount() + ", " + Math.max(0, s.performElapsed(now)) + " ms, " + s.appliedInputs() + " input(s)"), false);
        return all.size();
    }

    private static int cancel(CommandContext<CommandSourceStack> c, ServerPlayer target) {
        boolean done = SessionManager.interrupt(target, InterruptionReason.ADMIN);
        if (done) ImmersiveSpellcraft.LOGGER.info("{} cancelled the performance of {}", c.getSource().getTextName(), target.getGameProfile().getName());
        c.getSource().sendSuccess(() -> Component.translatable(done ? "immersive_spellcraft.command.cancel.done"
                : "immersive_spellcraft.command.cancel.none", target.getDisplayName()), done);
        return done ? 1 : 0;
    }

    private static int release(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
        for (MagicOperationAdapter a : AdapterRegistry.all()) {
            Component result = a.releaseJob(c.getSource().getLevel(), pos);
            if (result != null) {
                ImmersiveSpellcraft.LOGGER.info("{} released the held {} job at {}: {}", c.getSource().getTextName(), a.operation(),
                        pos.toShortString(), result.getString());
                c.getSource().sendSuccess(() -> result, true);
                return 1;
            }
        }
        c.getSource().sendFailure(Component.translatable("immersive_spellcraft.command.release.none"));
        return 0;
    }

    private static int practice(CommandContext<CommandSourceStack> c, String game) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        if (!game.isEmpty() && !GameTypes.known(game)) {
            c.getSource().sendFailure(Component.translatable("immersive_spellcraft.command.practice.unknown", game));
            return 0;
        }
        ModNetwork.sendTo(player, new Packets.OpenPractice(game));
        return 1;
    }

    private SpellcraftCommands() {}
}
