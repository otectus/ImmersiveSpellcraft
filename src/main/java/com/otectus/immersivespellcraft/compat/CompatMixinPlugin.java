package com.otectus.immersivespellcraft.compat;

import com.otectus.immersivespellcraft.core.Operations;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a compat mixin only when its target mod is installed in a supported version, so an absent mod never sees a
 * missing-target error and an untested version keeps its native code untouched. Which mixins were applied is
 * recorded for {@code /immersivespellcraft integrations}.
 */
public final class CompatMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, Boolean> DECISIONS = new ConcurrentHashMap<>();

    public static Map<String, Boolean> decisions() {
        return Map.copyOf(DECISIONS);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String mod = mixinClassName.contains(".compat.ars.") ? Operations.ARS
                : mixinClassName.contains(".compat.ans.") ? Operations.ANS : null;
        boolean apply = mod == null || IntegrationVersions.supportedAtLaunch(mod);
        DECISIONS.put(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1), apply);
        return apply;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
