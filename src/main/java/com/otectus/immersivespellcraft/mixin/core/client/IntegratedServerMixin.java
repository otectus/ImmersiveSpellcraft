package com.otectus.immersivespellcraft.mixin.core.client;

import com.otectus.immersivespellcraft.session.SessionClock;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the integrated server's own pause flag on its thread, so a paused world never expires a performance. */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
    @Shadow private boolean paused;

    @Inject(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/server/IntegratedServer;paused:Z",
            opcode = 181, shift = At.Shift.AFTER))
    private void immersivespellcraft$pause(CallbackInfo ci) {
        SessionClock.paused(paused);
    }
}
