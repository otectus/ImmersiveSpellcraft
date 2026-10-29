package com.otectus.immersivespellcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Presentation and accessibility preferences ({@code config/immersive_spellcraft-client.toml}). Only timingAssist and
 * untimedMode reach the server, as requests it caps; none of these can change what a station makes. Keys are ordinary
 * key mappings (Options > Controls > Immersive Spellcraft).
 */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    public enum HoldMode { HOLD, TOGGLE }

    public static final ForgeConfigSpec.EnumValue<HoldMode> HOLD_MODE;
    public static final ForgeConfigSpec.BooleanValue POINTER_ALIGNMENT;
    public static final ForgeConfigSpec.DoubleValue TIMING_ASSIST;
    public static final ForgeConfigSpec.BooleanValue UNTIMED_MODE;
    public static final ForgeConfigSpec.BooleanValue REDUCED_MOTION;
    public static final ForgeConfigSpec.BooleanValue HIGH_CONTRAST;
    public static final ForgeConfigSpec.BooleanValue COLORBLIND_SAFE;
    public static final ForgeConfigSpec.DoubleValue UI_SCALE;
    public static final ForgeConfigSpec.BooleanValue CAPTIONS;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec.DoubleValue BACKGROUND_DIM;
    public static final ForgeConfigSpec.BooleanValue SHOW_QUALITY;
    public static final ForgeConfigSpec.IntValue RESULT_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("controls");
        HOLD_MODE = b.comment("HOLD: keep the primary action held. TOGGLE: press once to start holding, again to release.")
                .defineEnum("holdMode", HoldMode.HOLD);
        POINTER_ALIGNMENT = b.comment("Let the mouse position steer alignments (keys always work too).").define("pointerAlignment", true);
        b.pop();

        b.comment("Accessibility").push("accessibility");
        TIMING_ASSIST = b.comment("Widen windows and bands by up to this factor. The server caps it (maxClientTimingAssist).")
                .defineInRange("timingAssist", 1.0, 1.0, 2.0);
        UNTIMED_MODE = b.comment("Markers wait on their marks and nothing expires, if the server allows it.").define("untimedMode", false);
        REDUCED_MOTION = b.comment("No pulses, sweeps drawn as steps, no particles in the panel.").define("reducedMotion", false);
        HIGH_CONTRAST = b.comment("White marks with dark outlines and solid bands.").define("highContrast", false);
        COLORBLIND_SAFE = b.comment("Okabe-Ito colors for good/warning cues. Cues always differ by shape and text as well.")
                .define("colorblindSafe", true);
        UI_SCALE = b.comment("Scale of the performance panel.").defineInRange("uiScale", 1.0, 0.75, 1.5);
        CAPTIONS = b.comment("Show a caption for every cue (aligned, too much pressure, junction missed...).").define("captions", true);
        SOUND_VOLUME = b.defineInRange("soundVolume", 1.0, 0.0, 1.0);
        b.pop();

        b.push("display");
        BACKGROUND_DIM = b.comment("How strongly the station screen is dimmed behind the panel.").defineInRange("backgroundDim", 0.45, 0.0, 1.0);
        SHOW_QUALITY = b.comment("Show the numeric workmanship score beside the grade.").define("showQuality", true);
        RESULT_SECONDS = b.comment("How long the result panel stays up.").defineInRange("resultSeconds", 4, 1, 15);
        b.pop();
        SPEC = b.build();
    }

    public static boolean get(ForgeConfigSpec.BooleanValue v) { return SPEC.isLoaded() ? v.get() : v.getDefault(); }
    public static double get(ForgeConfigSpec.DoubleValue v) { return SPEC.isLoaded() ? v.get() : v.getDefault(); }
    public static int get(ForgeConfigSpec.IntValue v) { return SPEC.isLoaded() ? v.get() : v.getDefault(); }
    public static HoldMode holdMode() { return SPEC.isLoaded() ? HOLD_MODE.get() : HOLD_MODE.getDefault(); }

    private ClientConfig() {}
}
