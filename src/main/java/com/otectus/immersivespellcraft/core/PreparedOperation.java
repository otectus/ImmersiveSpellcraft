package com.otectus.immersivespellcraft.core;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * An admitted operation: everything that identifies it and everything the commit will recheck. The output stack is a
 * copied preview for display and comparison, never an owned item. {@code payload} is the adapter's own snapshot
 * (station instance, native job id, input copies) and is never persisted: sessions are short and an interrupted one
 * is released, while native jobs keep their own saved state.
 */
public final class PreparedOperation {
    public final ResourceLocation operation;
    public final ResourceLocation adapter;
    public final int adapterVersion;
    /** Recipe or selection identity, e.g. a glyph recipe id or {@code spell@level}. */
    public final String selection;
    public final int dataGeneration;
    public final int configRevision;
    public final UUID player;
    public final StationKey station;
    @Nullable public final AbstractContainerMenu menu;
    public final int menuId;
    @Nullable public final ClickRequest click;
    @Nullable public final String nativeJob;
    public final List<ItemStack> inputs;
    public final ItemStack output;
    public final Component title;
    public final List<Component> costs;
    public final double complexity;
    public final int nativeWindowMs;
    public final Object payload;

    private PreparedOperation(Builder b) {
        operation = b.operation;
        adapter = b.adapter;
        adapterVersion = b.adapterVersion;
        selection = b.selection;
        dataGeneration = b.dataGeneration;
        configRevision = b.configRevision;
        player = b.player;
        station = b.station;
        menu = b.menu;
        menuId = b.menu == null ? -1 : b.menu.containerId;
        click = b.click;
        nativeJob = b.nativeJob;
        inputs = b.inputs.stream().map(ItemStack::copy).toList();
        output = b.output.copy();
        title = b.title;
        costs = List.copyOf(b.costs);
        complexity = Math.max(0, Math.min(1, b.complexity));
        nativeWindowMs = Math.max(0, b.nativeWindowMs);
        payload = b.payload;
    }

    public static Builder builder(MagicOperationAdapter adapter, java.util.UUID player, StationKey station) {
        return new Builder(adapter, player, station);
    }

    public static final class Builder {
        private final ResourceLocation operation, adapter;
        private final int adapterVersion;
        private final UUID player;
        private final StationKey station;
        private String selection = "";
        private int dataGeneration, configRevision;
        private AbstractContainerMenu menu;
        private ClickRequest click;
        private String nativeJob;
        private List<ItemStack> inputs = List.of();
        private ItemStack output = ItemStack.EMPTY;
        private Component title = Component.empty();
        private List<Component> costs = List.of();
        private double complexity;
        private int nativeWindowMs;
        private Object payload;

        private Builder(MagicOperationAdapter a, UUID player, StationKey station) {
            operation = a.operation();
            adapter = a.id();
            adapterVersion = a.version();
            this.player = player;
            this.station = station;
        }

        public Builder selection(String v) { selection = v; return this; }
        public Builder generations(int data, int config) { dataGeneration = data; configRevision = config; return this; }
        public Builder menu(AbstractContainerMenu v, @Nullable ClickRequest c) { menu = v; click = c; return this; }
        public Builder nativeJob(String v) { nativeJob = v; return this; }
        public Builder inputs(List<ItemStack> v) { inputs = v; return this; }
        public Builder output(ItemStack v) { output = v; return this; }
        public Builder title(Component v) { title = v; return this; }
        public Builder costs(List<Component> v) { costs = v; return this; }
        public Builder complexity(double v) { complexity = v; return this; }
        public Builder nativeWindowMs(int v) { nativeWindowMs = v; return this; }
        public Builder payload(Object v) { payload = v; return this; }
        public PreparedOperation build() { return new PreparedOperation(this); }
    }
}
