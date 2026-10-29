package com.otectus.immersivespellcraft.test;

import com.otectus.immersivespellcraft.ImmersiveSpellcraft;
import com.otectus.immersivespellcraft.core.Operations;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The vertical slice's native parity, interruption and abuse tests. Each self-skips (succeeds at once) when its
 * integration is not active in the run profile; {@link CoreGameTests#profileMatchesTheRequestedMods} separately fails
 * a profile whose requested mods did not actually load. The scenarios live in classes that reference the optional mods,
 * reached only after the check, so this holder loads in every profile.
 */
@GameTestHolder(ImmersiveSpellcraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class StationGameTests {
    private static final int LONG = 900;

    private static boolean ars() { return TestSupport.active(Operations.ARS_SCRIBE_ADAPTER); }
    private static boolean irons() { return TestSupport.active(Operations.IRONS_SCROLL_FORGE_ADAPTER); }
    private static boolean press() { return TestSupport.active(Operations.ANS_GLYPH_PRESS_ADAPTER) && irons(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void scribeGlyphIsHeldUntilInscribed(GameTestHelper h) { if (ars()) ArsScenarios.heldUntilInscribed(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void scribePreservedRecipeIsNative(GameTestHelper h) { if (ars()) ArsScenarios.preservedRecipeIsNative(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void scribeCancelKeepsTheJob(GameTestHelper h) { if (ars()) ArsScenarios.cancelKeepsTheJob(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void scribeBreakingRefundsOnce(GameTestHelper h) { if (ars()) ArsScenarios.breakingRefundsOnce(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void scribeSecondPlayerIsRefused(GameTestHelper h) { if (ars()) ArsScenarios.secondPlayerIsRefused(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void scribeForgedInputsNeverComplete(GameTestHelper h) { if (ars()) ArsScenarios.forgedInputsNeverComplete(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void forgeDirectTakeIsGuarded(GameTestHelper h) { if (irons()) IronsScenarios.directTakeIsGuarded(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void forgePerformedTakeMatchesNative(GameTestHelper h) { if (irons()) IronsScenarios.performedTakeMatchesNative(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void forgeShiftClickRepeatsNatively(GameTestHelper h) { if (irons()) IronsScenarios.shiftClickRepeatsNatively(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void forgeChangedInputsRelease(GameTestHelper h) { if (irons()) IronsScenarios.changedInputsRelease(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void forgeStaleBeginChangesNothing(GameTestHelper h) { if (irons()) IronsScenarios.staleBeginChangesNothing(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void forgeSecondViewerIsLockedOut(GameTestHelper h) { if (irons()) IronsScenarios.secondViewerIsLockedOut(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void pressJobIsHeldUntilEmbossed(GameTestHelper h) { if (press()) AnsScenarios.heldUntilEmbossed(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void pressNeverShortensTheNativeWindow(GameTestHelper h) { if (press()) AnsScenarios.neverShortensTheNativeWindow(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void pressNativeCancelEndsTheSession(GameTestHelper h) { if (press()) AnsScenarios.nativeCancelEndsTheSession(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void pressOnlyTheStarterEmbosses(GameTestHelper h) { if (press()) AnsScenarios.onlyTheStarterEmbosses(h); else h.succeed(); }

    @GameTest(template = "empty", timeoutTicks = LONG)
    public static void pressAutomationIsNative(GameTestHelper h) { if (press()) AnsScenarios.automationIsNative(h); else h.succeed(); }
}
