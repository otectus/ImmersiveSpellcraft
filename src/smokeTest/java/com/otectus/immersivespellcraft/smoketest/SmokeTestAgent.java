package com.otectus.immersivespellcraft.smoketest;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/** Test-only client agent mod; loaded only by {@code runClient -PsmokeTest}. Never shipped. */
@Mod("immersive_spellcraft_smoketest")
public class SmokeTestAgent {
    public SmokeTestAgent() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(ClientSmokeTest.class));
    }
}
