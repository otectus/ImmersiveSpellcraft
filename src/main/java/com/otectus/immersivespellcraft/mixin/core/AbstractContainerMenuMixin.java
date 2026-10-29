package com.otectus.immersivespellcraft.mixin.core;

import com.otectus.immersivespellcraft.session.MenuGuards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every server-side menu click passes {@link MenuGuards}; client prediction is never touched. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void immersivespellcraft$guardClick(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer p && MenuGuards.blockClick(p, (AbstractContainerMenu) (Object) this, slot, button, type)) ci.cancel();
    }
}
