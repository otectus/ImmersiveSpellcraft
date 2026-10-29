package com.otectus.immersivespellcraft.minigame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTest {
    static Stream<String> games() {
        return GameTypes.ALL.stream().sorted();
    }

    static GameParams params(long seed, double complexity, boolean untimed, double assist, int nativeWindow) {
        return new GameParams(complexity, 8000, 20_000, 3, 6, seed, assist, untimed, 1.0, nativeWindow);
    }

    static ScoreCard replay(GameSpec spec, List<InputEvent> events) {
        Simulation s = new Simulation(spec);
        for (InputEvent e : events) assertTrue(s.apply(e), "replayed input must be accepted: " + e);
        return s.score();
    }

    @ParameterizedTest
    @MethodSource("games")
    void cleanPlayCompletesWellAndReplaysIdentically(String type) {
        for (long seed = 1; seed <= 40; seed++) {
            for (double c : new double[]{0, 0.5, 1}) {
                GameSpec spec = GameTypes.create(type, params(seed, c, false, 1, 0));
                List<InputEvent> events = Autoplay.play(spec, Autoplay.Style.CLEAN);
                ScoreCard live = replay(spec, events);
                assertTrue(live.complete(), type + " seed " + seed + " c " + c + " must complete");
                assertTrue(live.quality() >= 85, type + " clean quality " + live.quality());
                assertEquals(0, live.corrections(), type + " clean play needs no corrections");
                assertEquals(live, replay(spec, events), "the same inputs must give the same result");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("games")
    void sloppyPlayIsCorrectableAndScoresLower(String type) {
        for (long seed = 1; seed <= 20; seed++) {
            GameSpec spec = GameTypes.create(type, params(seed, 0.5, false, 1, 0));
            ScoreCard clean = replay(spec, Autoplay.play(spec, Autoplay.Style.CLEAN));
            ScoreCard sloppy = replay(spec, Autoplay.play(spec, Autoplay.Style.SLOPPY));
            assertTrue(sloppy.complete(), type + " seed " + seed + ": mistakes are always correctable");
            assertTrue(sloppy.corrections() > 0, type + " sloppy play must need corrections");
            assertTrue(sloppy.quality() < clean.quality(), type + " sloppy " + sloppy.quality() + " vs clean " + clean.quality());
        }
    }

    @ParameterizedTest
    @MethodSource("games")
    void noInputExpiresIncomplete(String type) {
        GameSpec spec = GameTypes.create(type, params(7, 0.5, false, 1, 0));
        Simulation s = new Simulation(spec);
        s.advanceTo(spec.maxDurationMs() + 100);
        assertTrue(s.expired());
        assertFalse(s.complete());
        assertFalse(s.score().complete());
        assertFalse(s.apply(InputEvent.down(spec.maxDurationMs() + 200, Control.PRIMARY)), "no input after expiry");
    }

    @ParameterizedTest
    @MethodSource("games")
    void untimedNeverExpiresAndStillCompletes(String type) {
        GameSpec spec = GameTypes.create(type, params(11, 1, true, 1, 0));
        assertEquals(0, spec.maxDurationMs());
        Simulation idle = new Simulation(spec);
        idle.advanceTo(100_000);
        assertFalse(idle.expired());
        ScoreCard card = replay(spec, Autoplay.play(spec, Autoplay.Style.CLEAN));
        assertTrue(card.complete());
        assertTrue(card.assisted());
    }

    /** The demonstration plays like a person: well under the server's default limit of 120 input events a second. */
    @ParameterizedTest
    @MethodSource("games")
    void demonstrationInputRateIsHumanlike(String type) {
        for (long seed = 1; seed <= 20; seed++) {
            for (Autoplay.Style style : Autoplay.Style.values()) {
                List<InputEvent> events = Autoplay.play(GameTypes.create(type, params(seed, 0.5, false, 1, 7000)), style);
                for (int i = 0, j = 0; i < events.size(); i++) {
                    while (events.get(i).timeMs() - events.get(j).timeMs() >= 1000) j++;
                    assertTrue(i - j + 1 <= 40, type + " " + style + " sends " + (i - j + 1) + " events within one second");
                }
            }
        }
    }

    @Test
    void cleanDurationsFitTheirTargets() {
        for (long seed = 1; seed <= 20; seed++) {
            int sigil = durationOf(GameTypes.SIGIL_INSCRIPTION, seed, 0);
            assertTrue(sigil >= 4000 && sigil <= 12_000, "sigil inscription took " + sigil);
            int forging = durationOf(GameTypes.SCROLL_FORGING, seed, 0);
            assertTrue(forging >= 4000 && forging <= 12_000, "scroll forging took " + forging);
            for (int window : new int[]{5000, 7000, 10_000}) {
                int emboss = durationOf(GameTypes.GLYPH_EMBOSSING, seed, window);
                assertTrue(emboss >= window - 2000 && emboss <= window + 3000, "embossing took " + emboss + " for window " + window);
            }
        }
    }

    private static int durationOf(String type, long seed, int window) {
        GameSpec spec = GameTypes.create(type, params(seed, 0.5, false, 1, window));
        return replay(spec, Autoplay.play(spec, Autoplay.Style.CLEAN)).elapsedMs();
    }

    @Test
    void assistanceWidensWindowsWithinCaps() {
        GameSpec plain = GameTypes.create(GameTypes.SCROLL_FORGING, params(3, 0.5, false, 1, 0));
        GameSpec assisted = GameTypes.create(GameTypes.SCROLL_FORGING, params(3, 0.5, false, 2, 0));
        StageSpec.TimedTrigger a = (StageSpec.TimedTrigger) plain.stage(2), b = (StageSpec.TimedTrigger) assisted.stage(2);
        assertTrue(b.halfWidth() > a.halfWidth());
        assertTrue(b.halfWidth() <= 0.3);
        StageSpec.Align x = (StageSpec.Align) plain.stage(0), y = (StageSpec.Align) assisted.stage(0);
        assertTrue(y.tolerance() > x.tolerance() && y.tolerance() <= 0.35);
    }

    @Test
    void pastAndDuplicateTimesAreRejected() {
        GameSpec spec = GameTypes.create(GameTypes.SIGIL_INSCRIPTION, params(5, 0, false, 1, 0));
        Simulation s = new Simulation(spec);
        assertTrue(s.apply(InputEvent.down(500, Control.PRIMARY)));
        assertFalse(s.apply(InputEvent.up(400, Control.PRIMARY)), "time cannot run backwards");
        assertTrue(s.apply(InputEvent.up(500, Control.PRIMARY)), "the same instant is allowed");
    }

    @Test
    void pointerAlignmentIsRateLimited() {
        GameSpec spec = GameTypes.create(GameTypes.SCROLL_FORGING, params(9, 0, false, 1, 0));
        StageSpec.Align align = (StageSpec.Align) spec.stage(0);
        Simulation s = new Simulation(spec);
        s.apply(new InputEvent(0, Control.POINTER, 0));
        s.apply(InputEvent.down(10, Control.PRIMARY));
        assertFalse(s.view(0).done(), "a pointer cannot teleport the focus onto its mark");
        double distance = Math.abs(align.start() - align.target()) - align.tolerance();
        int earliest = (int) (distance / (align.speed() * 1.5) * 1000);
        s.apply(InputEvent.up(earliest + 60, Control.PRIMARY));
        s.apply(InputEvent.down(earliest + 80, Control.PRIMARY));
        assertTrue(s.view(0).done(), "pointer alignment works at its bounded rate");
    }

    @Test
    void bleedNeedsOnlyItsSegmentRepaired() {
        GameSpec spec = GameTypes.create(GameTypes.SIGIL_INSCRIPTION, params(13, 0, false, 1, 0));
        StageSpec.Strokes strokes = (StageSpec.Strokes) spec.stage(0);
        Simulation s = new Simulation(spec);
        int len = strokes.length(0);
        s.apply(InputEvent.down(0, Control.PRIMARY));
        s.advanceTo(len + strokes.junctionHalfMs() + 50);
        assertTrue(s.current().needsRepair(), "overshooting the junction bleeds");
        assertEquals(0, s.current().index(), "the same stroke is repaired, not the whole sigil");
        assertTrue(s.current().value() > 0.4, "only the tail must be inked again");
        assertEquals(1, s.corrections());
    }

    @Test
    void gradeBands() {
        assertEquals(Grade.CRUDE, Grade.of(0));
        assertEquals(Grade.CRUDE, Grade.of(34));
        assertEquals(Grade.STANDARD, Grade.of(35));
        assertEquals(Grade.STANDARD, Grade.of(69));
        assertEquals(Grade.FINE, Grade.of(70));
        assertEquals(Grade.FINE, Grade.of(89));
        assertEquals(Grade.MASTERWORK, Grade.of(90));
        assertEquals(Grade.MASTERWORK, Grade.of(100));
    }

    @Test
    void weightsMustTotalOne() {
        List<StageSpec> stages = List.of(new StageSpec.TimedTrigger("seal", "seal", StageSpec.Trigger.PRESS, 1000, 0.5, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new GameSpec("x", 1, 0, 0, 0, false, 1, stages, List.of(new Weight("seal", 0.9))));
        assertDoesNotThrow(() -> new GameSpec("x", 1, 0, 0, 0, false, 1, stages, List.of(new Weight("seal", 1))));
    }

    @Test
    void essentialWorkIsSeparateFromQuality() {
        GameSpec spec = GameTypes.create(GameTypes.GLYPH_EMBOSSING, params(21, 0.5, false, 1, 5000));
        List<InputEvent> all = Autoplay.play(spec, Autoplay.Style.CLEAN);
        Simulation s = new Simulation(spec);
        List<InputEvent> withoutSeal = new ArrayList<>(all.subList(0, all.size() - 1));
        withoutSeal.forEach(s::apply);
        assertFalse(s.complete(), "a run that never releases on the seal is not complete, however clean");
    }
}
