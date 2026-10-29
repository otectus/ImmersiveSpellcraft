package com.otectus.immersivespellcraft.mixin.compat.ars;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.otectus.immersivespellcraft.compat.ars.ScribeGate;
import com.otectus.immersivespellcraft.compat.ars.ScribeGateHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The Scribe's Table glyph gate. {@code tick} and {@code getTooltip} are Ars's own methods; the save and load
 * overrides of vanilla's BlockEntity methods are named in both dev ({@code saveAdditional}/{@code load}) and production
 * ({@code m_183515_}/{@code m_142466_}) form with remapping off, so neither environment depends on the refmap here.
 * ScribesTile does not call {@code super.saveAdditional}, so Forge's persistent data would not survive; the gate is
 * saved under its own key instead.
 */
@Mixin(value = ScribesTile.class, remap = false)
public abstract class ScribesTileMixin implements ScribeGateHolder {
    @Unique private int immersivespellcraft$state;
    @Unique private long immersivespellcraft$job;
    @Unique private String immersivespellcraft$recipe = "";

    @Override public int immersivespellcraft$gateState() { return immersivespellcraft$state; }
    @Override public long immersivespellcraft$gateJob() { return immersivespellcraft$job; }
    @Override public String immersivespellcraft$gateRecipe() { return immersivespellcraft$recipe; }

    @Override
    public void immersivespellcraft$setGate(int state, long job, String recipe) {
        immersivespellcraft$state = state;
        immersivespellcraft$job = job;
        immersivespellcraft$recipe = recipe == null ? "" : recipe;
        ((ScribesTile) (Object) this).setChanged();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void immersivespellcraft$gate(CallbackInfo ci) {
        ScribeGate.beforeTick((ScribesTile) (Object) this);
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"))
    private void immersivespellcraft$save(CompoundTag tag, CallbackInfo ci) {
        if (immersivespellcraft$state == ScribeGate.NONE) return;
        CompoundTag gate = new CompoundTag();
        gate.putInt("state", immersivespellcraft$state);
        gate.putLong("job", immersivespellcraft$job);
        gate.putString("recipe", immersivespellcraft$recipe);
        tag.put("immersive_spellcraft", gate);
    }

    @Inject(method = {"load", "m_142466_"}, at = @At("TAIL"))
    private void immersivespellcraft$load(CompoundTag tag, CallbackInfo ci) {
        CompoundTag gate = tag.getCompound("immersive_spellcraft");
        immersivespellcraft$state = gate.getInt("state");
        immersivespellcraft$job = gate.getLong("job");
        immersivespellcraft$recipe = gate.getString("recipe");
    }

    @Inject(method = "getTooltip", at = @At("TAIL"))
    private void immersivespellcraft$tooltip(List<Component> tooltip, CallbackInfo ci) {
        ScribeGate.tooltip((ScribesTile) (Object) this, tooltip);
    }
}
