package com.otectus.immersivespellcraft.mixin.core.client;

import com.otectus.immersivespellcraft.client.MenuIntercepts;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turns a click that would take a gated native result into a request to begin its performance. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void immersivespellcraft$intercept(int container, int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        if (MenuIntercepts.intercept(container, slot, button, type)) ci.cancel();
    }
}
