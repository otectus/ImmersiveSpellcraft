package com.otectus.immersivespellcraft.mixin.core;

import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a player performs in a menu, its container packets are refused after the thread check and before any slot,
 * button or creative-slot mutation; the client is resynchronized instead.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = {"handleContainerClick", "handleContainerButtonClick", "handleSetCreativeModeSlot"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER), cancellable = true)
    private void immersivespellcraft$lockMenu(CallbackInfo ci) {
        if (SessionManager.locked(player, player.containerMenu)) {
            player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
