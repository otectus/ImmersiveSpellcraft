package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.config.ClientConfig;
import com.otectus.immersivespellcraft.minigame.Cue;
import com.otectus.immersivespellcraft.minigame.GameTypes;
import com.otectus.immersivespellcraft.minigame.Simulation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Turns simulation cues into sound and captions. Every cue has a caption (shape and text, not only color or sound);
 * sounds are vanilla UI sounds at the player's chosen volume.
 */
public final class CueFeed {
    public record Caption(Component text, Cue.Kind kind, long shownAt) {}

    private static final long CAPTION_MS = 1800;
    private final Deque<Caption> captions = new ArrayDeque<>();
    private int serial;

    public void poll(Simulation sim, String game) {
        for (Cue cue : sim.cuesAfter(serial)) {
            sound(cue.kind());
            if (ClientConfig.get(ClientConfig.CAPTIONS)) {
                String key = "immersive_spellcraft.cue." + cue.kind().name().toLowerCase(java.util.Locale.ROOT);
                if (cue.kind() == Cue.Kind.STAGE_COMPLETE) continue;
                captions.addLast(new Caption(Component.translatable(key), cue.kind(), Util.getMillis()));
                while (captions.size() > 3) captions.removeFirst();
            }
        }
        serial = sim.cueSerial();
        long now = Util.getMillis();
        while (!captions.isEmpty() && now - captions.peekFirst().shownAt() > CAPTION_MS) captions.removeFirst();
    }

    public Iterable<Caption> captions() {
        return captions;
    }

    public void reset(Simulation sim) {
        serial = sim.cueSerial();
        captions.clear();
    }

    private static void sound(Cue.Kind kind) {
        float volume = (float) ClientConfig.get(ClientConfig.SOUND_VOLUME);
        if (volume <= 0) return;
        SoundEvent event;
        float pitch = 1;
        switch (kind) {
            case ALIGNED -> { event = SoundEvents.AMETHYST_BLOCK_HIT; pitch = 1.4f; }
            case MISALIGNED, TRIGGER_MISSED, EXPIRED -> { event = SoundEvents.NOTE_BLOCK_BASS.get(); pitch = 0.7f; }
            case BAND_ENTERED -> { event = SoundEvents.AMETHYST_CLUSTER_STEP; pitch = 1.2f; }
            case BAND_HIGH -> { event = SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(); pitch = 1.3f; }
            case BAND_LOW -> { return; }
            case TRIGGER_HIT -> { event = SoundEvents.ANVIL_LAND; pitch = 1.8f; volume *= 0.35f; }
            case STROKE_DONE -> { event = SoundEvents.VILLAGER_WORK_CARTOGRAPHER; pitch = 1.3f; }
            case STROKE_LIFTED -> { event = SoundEvents.BOOK_PAGE_TURN; pitch = 1.5f; }
            case STROKE_BLEED -> { event = SoundEvents.HONEY_BLOCK_SLIDE; pitch = 0.8f; }
            case STAGE_COMPLETE -> { event = SoundEvents.AMETHYST_BLOCK_CHIME; pitch = 1.1f; }
            case COMPLETE -> { event = SoundEvents.ENCHANTMENT_TABLE_USE; pitch = 1.2f; }
            default -> { return; }
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume * 0.8f));
    }

    /** The game's own instruction prefix, e.g. {@code immersive_spellcraft.game.glyph_embossing}. */
    public static String prefix(String game) {
        return GameTypes.keyPrefix(game);
    }
}
