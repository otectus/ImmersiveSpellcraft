package com.otectus.immersivespellcraft.mixin.compat.ans;

import com.otectus.arsnspells.block.GlyphPressBlockEntity;
import com.otectus.immersivespellcraft.compat.ans.PressGate;
import com.otectus.immersivespellcraft.compat.ans.PressGateHolder;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Glyph Press gate: observes the Press's own {@code start(player)} result and holds {@code complete()} for a gated
 * job until it is embossed. Both are Ars 'n Spells' own methods, so no remapping is involved.
 */
@Mixin(value = GlyphPressBlockEntity.class, remap = false)
public abstract class GlyphPressBlockEntityMixin implements PressGateHolder {
    @Inject(method = "start", at = @At("RETURN"))
    private void immersivespellcraft$started(ServerPlayer player, CallbackInfoReturnable<String> cir) {
        PressGate.started((GlyphPressBlockEntity) (Object) this, player, cir.getReturnValue());
    }

    @Inject(method = "complete", at = @At("HEAD"), cancellable = true)
    private void immersivespellcraft$hold(CallbackInfo ci) {
        if (PressGate.hold((GlyphPressBlockEntity) (Object) this)) ci.cancel();
    }
}
