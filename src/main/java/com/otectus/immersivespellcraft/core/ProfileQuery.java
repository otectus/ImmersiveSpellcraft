package com.otectus.immersivespellcraft.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The registry identities an operation profile selector matches on: the operation and adapter, the station block,
 * the native recipe and recipe type, and the items involved (output first). All from registries, never display names.
 */
public record ProfileQuery(ResourceLocation operation, ResourceLocation adapter, @Nullable ResourceLocation block,
                           @Nullable ResourceLocation recipe, @Nullable ResourceLocation recipeType, List<ItemStack> items) {
    public ProfileQuery {
        items = items.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
    }
}
