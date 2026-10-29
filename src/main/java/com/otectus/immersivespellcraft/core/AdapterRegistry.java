package com.otectus.immersivespellcraft.core;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Active adapters and the one owner of each operation, recorded at startup. A second adapter claiming an owned
 * operation is reported with an actionable diagnostic and resolved deterministically (the lexically smaller adapter
 * id keeps it); an unknown third-party handler is never intercepted.
 */
public final class AdapterRegistry {
    private static final Map<ResourceLocation, MagicOperationAdapter> ADAPTERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, MagicOperationAdapter> OWNERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, String> DORMANT = new LinkedHashMap<>();
    private static final List<String> DIAGNOSTICS = new ArrayList<>();
    private static volatile boolean frozen;

    public static synchronized void register(MagicOperationAdapter adapter) {
        if (frozen) throw new IllegalStateException("adapter registry is frozen; register during common setup");
        if (ADAPTERS.putIfAbsent(adapter.id(), adapter) != null) {
            DIAGNOSTICS.add("Duplicate adapter id " + adapter.id() + " ignored");
            return;
        }
        DORMANT.remove(adapter.id());
        MagicOperationAdapter owner = OWNERS.get(adapter.operation());
        if (owner == null) {
            OWNERS.put(adapter.operation(), adapter);
        } else {
            MagicOperationAdapter keep = owner.id().compareTo(adapter.id()) <= 0 ? owner : adapter;
            String message = "Operation " + adapter.operation() + " is claimed by both " + owner.id() + " and " + adapter.id()
                    + "; " + keep.id() + " keeps it. Disable one integration in immersive_spellcraft-server.toml to silence this.";
            DIAGNOSTICS.add(message);
            ImmersiveSpellcraft.LOGGER.warn(message);
            OWNERS.put(adapter.operation(), keep);
        }
    }

    /** Records a known adapter that is inactive here, and why (mod absent, unsupported version, disabled). */
    public static synchronized void dormant(ResourceLocation adapter, String reason) {
        if (!ADAPTERS.containsKey(adapter)) DORMANT.put(adapter, reason);
    }

    public static void freeze() {
        frozen = true;
    }

    public static Optional<MagicOperationAdapter> byId(ResourceLocation id) {
        return Optional.ofNullable(ADAPTERS.get(id));
    }

    public static Optional<MagicOperationAdapter> owner(ResourceLocation operation) {
        return Optional.ofNullable(OWNERS.get(operation));
    }

    public static Collection<MagicOperationAdapter> all() {
        return Collections.unmodifiableCollection(ADAPTERS.values());
    }

    public static Map<ResourceLocation, String> dormant() {
        return Collections.unmodifiableMap(DORMANT);
    }

    public static List<String> diagnostics() {
        return List.copyOf(DIAGNOSTICS);
    }

    /** Known means registered here or a recognized adapter that is merely dormant in this installation. */
    public static boolean known(ResourceLocation adapter) {
        return ADAPTERS.containsKey(adapter) || Operations.known(adapter) != null;
    }

    public static Optional<MagicOperationAdapter> forMenu(AbstractContainerMenu menu) {
        for (MagicOperationAdapter a : OWNERS.values()) if (a.acceptsMenu(menu)) return Optional.of(a);
        return Optional.empty();
    }

    public static Optional<MagicOperationAdapter> forBlock(BlockEntity blockEntity) {
        for (MagicOperationAdapter a : OWNERS.values()) if (a.acceptsBlock(blockEntity)) return Optional.of(a);
        return Optional.empty();
    }

    private AdapterRegistry() {}
}
