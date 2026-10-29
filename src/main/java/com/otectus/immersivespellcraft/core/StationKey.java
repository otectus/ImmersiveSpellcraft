package com.otectus.immersivespellcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Where an operation happens: the dimension and the normalized master position (the logic half of a multi-block).
 * The lock is keyed by this; the station's identity (its exact block entity instance) is checked separately, so a
 * replaced block at the same coordinates never inherits a plan.
 */
public record StationKey(ResourceKey<Level> dimension, BlockPos pos) {
    public StationKey {
        pos = pos.immutable();
    }

    @Override
    public String toString() {
        return dimension.location() + "@" + pos.toShortString();
    }
}
