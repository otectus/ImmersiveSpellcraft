package com.otectus.immersivespellcraft.core;

import net.minecraft.world.inventory.ClickType;

/** The exact native menu click a player asked for; the commit replays exactly this click, once. */
public record ClickRequest(int slot, int button, ClickType type) {
}
