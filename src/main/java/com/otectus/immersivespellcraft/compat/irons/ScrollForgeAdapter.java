package com.otectus.immersivespellcraft.compat.irons;

import com.otectus.immersivespellcraft.config.OperationSwitches;
import com.otectus.immersivespellcraft.config.ServerConfig;
import com.otectus.immersivespellcraft.core.AdapterCapability;
import com.otectus.immersivespellcraft.core.Admission;
import com.otectus.immersivespellcraft.core.ClickRequest;
import com.otectus.immersivespellcraft.core.CommitResult;
import com.otectus.immersivespellcraft.core.Inspection;
import com.otectus.immersivespellcraft.core.InterruptionReason;
import com.otectus.immersivespellcraft.core.InterruptionResult;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.MenuGuard;
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
import com.otectus.immersivespellcraft.session.SessionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Scroll Forging at the Iron's Scroll Forge (I01). An immediate-commit adapter: the forge's preview scroll stays a
 * preview until the performance completes, and the commit is the player's own result click, replayed once under the
 * permit, so Iron's own {@code onTake} consumes one ink, one paper and one focus per scroll exactly as it always does.
 * Shift-click keeps its native meaning (repeat while inputs and space last) under that one performance.
 *
 * <p>The menu is identical in Iron's 1.20.1-3.15.0 and 3.16.3 (verified bytecode), so one adapter serves both.
 */
public final class ScrollForgeAdapter implements MagicOperationAdapter, MenuGuard {
    public static final int VERSION = 1;
    private static final Set<ClickType> COMMIT_CLICKS = EnumSet.of(ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.SWAP, ClickType.THROW);
    private static final ResourceLocation BLOCK = new ResourceLocation(Operations.IRONS, "scroll_forge");

    /** The adapter's own snapshot: the exact menu and station instance and the four stacks it was planned from. */
    record Snapshot(ScrollForgeMenu menu, ScrollForgeTile forge, ItemStack ink, ItemStack paper, ItemStack focus, ItemStack result) {}

    @Override public ResourceLocation id() { return Operations.IRONS_SCROLL_FORGE_ADAPTER; }
    @Override public int version() { return VERSION; }
    @Override public ResourceLocation operation() { return Operations.IRONS_SCROLL_FORGE; }
    @Override public String integration() { return Operations.IRONS; }
    @Override public AdapterCapability capability() { return AdapterCapability.IMMEDIATE_COMMIT; }
    @Override public String defaultGame() { return GameTypes.SCROLL_FORGING; }
    @Override public boolean acceptsMenu(AbstractContainerMenu menu) { return menu instanceof ScrollForgeMenu; }

    // ---- MenuGuard -------------------------------------------------------------------------------------------------

    @Override public boolean handles(AbstractContainerMenu menu) { return menu instanceof ScrollForgeMenu; }

    @Override
    public boolean isCommitClick(AbstractContainerMenu menu, int slot, int button, ClickType type) {
        if (!(menu instanceof ScrollForgeMenu m) || slot != m.getResultSlot().index || !COMMIT_CLICKS.contains(type)) return false;
        return m.getResultSlot().hasItem();
    }

    @Override
    public boolean requiresPerformance(ServerPlayer player, AbstractContainerMenu menu) {
        if (!(menu instanceof ScrollForgeMenu m) || !OperationSwitches.enabled(id())) return false;
        return SessionManager.requiresPerformance(player, this, query(m));
    }

    @Override
    public @Nullable StationKey station(AbstractContainerMenu menu) {
        if (!(menu instanceof ScrollForgeMenu m) || m.blockEntity == null || m.blockEntity.getLevel() == null) return null;
        return new StationKey(m.blockEntity.getLevel().dimension(), m.blockEntity.getBlockPos());
    }

    @Override
    public boolean isStationSlot(AbstractContainerMenu menu, int slot) {
        if (!(menu instanceof ScrollForgeMenu m)) return false;
        Slot s = m.getSlot(slot);
        return s == m.getInkSlot() || s == m.getBlankScrollSlot() || s == m.getFocusSlot() || s == m.getResultSlot();
    }

    // ---- Adapter ---------------------------------------------------------------------------------------------------

    private ProfileQuery query(ScrollForgeMenu m) {
        List<ItemStack> items = List.of(m.getResultSlot().getItem(), m.getInkSlot().getItem(), m.getBlankScrollSlot().getItem(),
                m.getFocusSlot().getItem());
        return new ProfileQuery(operation(), id(), BLOCK, null, null, items);
    }

    @Override
    public Inspection inspect(OperationContext context) {
        if (!(context.menu() instanceof ScrollForgeMenu m) || m.blockEntity == null) return Inspection.NOT_APPLICABLE;
        ProfileQuery query = query(m);
        ItemStack result = m.getResultSlot().getItem();
        if (result.isEmpty()) return Inspection.refused(Refusal.of("nothing_to_forge"), query);
        ClickRequest click = context.click();
        if (click == null || !isCommitClick(m, click.slot(), click.button(), click.type()))
            return Inspection.refused(Refusal.of("changed"), query);
        if (!hasCapacity(context.player(), m, click, result)) return Inspection.refused(Refusal.of("inventory"), query);
        if (spell(result) == null) return Inspection.refused(Refusal.of("nothing_to_forge"), query);
        return Inspection.eligible(query);
    }

    @Nullable
    private static SpellData spell(ItemStack scroll) {
        if (!ISpellContainer.isSpellContainer(scroll)) return null;
        ISpellContainer container = ISpellContainer.get(scroll);
        if (container == null || container.getActiveSpellCount() < 1) return null;
        SpellData data = container.getSpellAtIndex(0);
        return data == null || data == SpellData.EMPTY ? null : data;
    }

    /** The click's own destination must have room, or the native take would refuse after the performance. */
    private static boolean hasCapacity(ServerPlayer player, AbstractContainerMenu menu, ClickRequest click, ItemStack result) {
        Inventory inv = player.getInventory();
        return switch (click.type()) {
            case PICKUP -> menu.getCarried().isEmpty();
            case QUICK_MOVE -> inv.getFreeSlot() >= 0 || inv.getSlotWithRemainingSpace(result) >= 0;
            case SWAP -> click.button() == 40 ? inv.offhand.get(0).isEmpty()
                    : click.button() >= 0 && click.button() < 9 && inv.getItem(click.button()).isEmpty();
            case THROW -> true;
            default -> false;
        };
    }

    @Override
    public Admission begin(OperationContext context, Inspection inspection) {
        ScrollForgeMenu m = (ScrollForgeMenu) context.menu();
        ServerPlayer player = context.player();
        ItemStack ink = m.getInkSlot().getItem().copy(), paper = m.getBlankScrollSlot().getItem().copy(),
                focus = m.getFocusSlot().getItem().copy(), result = m.getResultSlot().getItem().copy();
        SpellData data = spell(result);
        if (data == null) return Admission.refused(Refusal.of("nothing_to_forge"));
        AbstractSpell spell = data.getSpell();
        double complexity = ink.getItem() instanceof InkItem inkItem ? inkItem.getRarity().getValue() / 4.0 : 0.5;
        Component name = spell.getDisplayName(player).copy().append(" " + data.getLevel());
        List<Component> costs = new ArrayList<>();
        costs.add(Component.translatable("immersive_spellcraft.cost.consumes", 1, ink.getHoverName()));
        costs.add(Component.translatable("immersive_spellcraft.cost.consumes", 1, paper.getHoverName()));
        costs.add(Component.translatable("immersive_spellcraft.cost.consumes", 1, focus.getHoverName()));
        costs.add(Component.translatable("immersive_spellcraft.cost.school", spell.getSchoolType().getDisplayName()));
        if (context.click().type() == ClickType.QUICK_MOVE) costs.add(Component.translatable("immersive_spellcraft.cost.shift_click_repeats"));
        StationKey station = station(m);
        PreparedOperation op = PreparedOperation.builder(this, player.getUUID(), station)
                .selection(spell.getSpellId() + "@" + data.getLevel())
                .generations(ProfileManager.generation(), ServerConfig.revision())
                .menu(m, context.click())
                .inputs(List.of(ink, paper, focus))
                .output(result)
                .title(Component.translatable("immersive_spellcraft.operation.irons_scroll_forge.title", name))
                .costs(costs)
                .complexity(complexity)
                .payload(new Snapshot(m, m.blockEntity, ink, paper, focus, result))
                .build();
        return Admission.admitted(op);
    }

    @Override
    public Validation validate(PreparedOperation op, ServerPlayer player) {
        Snapshot s = (Snapshot) op.payload;
        if (player.containerMenu != s.menu() || !s.menu().stillValid(player))
            return Validation.invalid(InterruptionReason.MENU_CLOSED, Refusal.of("menu_closed"));
        if (s.forge().isRemoved() || s.forge().getLevel() == null || s.forge().getLevel().getBlockEntity(s.forge().getBlockPos()) != s.forge())
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("station_changed"));
        if (!ItemStack.matches(s.menu().getInkSlot().getItem(), s.ink()) || !ItemStack.matches(s.menu().getBlankScrollSlot().getItem(), s.paper())
                || !ItemStack.matches(s.menu().getFocusSlot().getItem(), s.focus()))
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("inputs_changed"));
        if (!ItemStack.matches(s.menu().getResultSlot().getItem(), s.result()))
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("result_changed"));
        if (!hasCapacity(player, s.menu(), op.click, s.result()))
            return Validation.invalid(InterruptionReason.STATION_CHANGED, Refusal.of("inventory"));
        return Validation.OK;
    }

    @Override
    public CommitResult commit(PreparedOperation op, ServerPlayer player, ScoreCard performance) {
        Snapshot s = (Snapshot) op.payload;
        ClickRequest click = op.click;
        int ink = s.menu().getInkSlot().getItem().getCount(), paper = s.menu().getBlankScrollSlot().getItem().getCount(),
                focus = s.menu().getFocusSlot().getItem().getCount();
        // The player's own click, once: Iron's onTake consumes the inputs, plays its sound and hands over the scroll.
        s.menu().clicked(click.slot(), click.button(), click.type(), player);
        s.menu().broadcastChanges();
        int usedInk = ink - s.menu().getInkSlot().getItem().getCount();
        int usedPaper = paper - s.menu().getBlankScrollSlot().getItem().getCount();
        int usedFocus = focus - s.menu().getFocusSlot().getItem().getCount();
        if (usedInk == 0 && usedPaper == 0 && usedFocus == 0) return CommitResult.refused(Refusal.of("native_refused"));
        if (usedInk != usedPaper || usedPaper != usedFocus || usedInk < 0)
            return CommitResult.held(Refusal.of("unreconciled"));
        return CommitResult.committed(Component.translatable("immersive_spellcraft.result.irons_scroll_forge", usedInk, s.result().getHoverName()));
    }

    @Override
    public InterruptionResult interrupt(PreparedOperation op, InterruptionReason reason) {
        // Nothing native happened before the commit; the forge, its inputs and its preview are exactly as they were.
        return InterruptionResult.RELEASED;
    }

    static String key(ItemStack stack) {
        ResourceLocation k = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return k == null ? "?" : k.toString();
    }
}
