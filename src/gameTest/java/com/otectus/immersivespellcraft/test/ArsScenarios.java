package com.otectus.immersivespellcraft.test;

import com.otectus.immersivespellcraft.compat.ars.ScribeGate;
import com.otectus.immersivespellcraft.minigame.Autoplay;
import com.otectus.immersivespellcraft.minigame.InputEvent;
import com.otectus.immersivespellcraft.minigame.Control;
import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.session.PerformanceSession;
import com.otectus.immersivespellcraft.session.SessionClock;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.otectus.immersivespellcraft.test.TestSupport.check;

/** A01 Scribe's Table glyph crafting: native parity, interruption and abuse. Runs only with Ars Nouveau active. */
final class ArsScenarios {
    static final String GATED = "ars_nouveau:glyph_light", NATIVE = "ars_nouveau:glyph_break";

    /** XP once at selection, the glyph held past the native window, then exactly one native drop after the sigil. */
    static void heldUntilInscribed(GameTestHelper h) {
        ArsFixtures.Table t = ArsFixtures.job(h, GATED, "ism_scribe_held");
        h.startSequence()
                .thenWaitUntil(() -> check(ArsFixtures.awaiting(t), "the gathered job awaits its inscription"))
                .thenIdle(130)
                .thenExecute(() -> {
                    check(ArsFixtures.glyphs(t) == 0, "no glyph before the inscription, however long the job waits");
                    check(ArsFixtures.crafting(t), "the native job is still running");
                    check(ArsFixtures.xp(t) == t.xpAfterCharge, "waiting never charges or refunds XP");
                    check(ArsFixtures.begin(t, t.player).outcome() == SessionManager.Outcome.STARTED, "right-click begins the inscription");
                    t.driver = new TestSupport.Driver(t.player, Autoplay.Style.CLEAN);
                })
                .thenWaitUntil(() -> {
                    t.driver.tick();
                    check(t.driver.finished(), "performing");
                })
                .thenExecute(() -> check(TestSupport.terminal(t.player).status() == Packets.Status.COMPLETE,
                        "the inscription completes: " + TestSupport.terminal(t.player)))
                .thenWaitUntil(() -> check(ArsFixtures.glyphs(t) == 1, "the table drops the glyph"))
                .thenIdle(20)
                .thenExecute(() -> {
                    check(ArsFixtures.glyphs(t) == 1, "exactly one glyph");
                    check(!ArsFixtures.crafting(t) && ArsFixtures.gate(t) == ScribeGate.NONE, "the job is finished and the gate reset");
                    check(ArsFixtures.xp(t) == t.xpAfterCharge, "no XP is charged or refunded by the performance");
                    check(ArsFixtures.drops(t, ArsFixtures.ingredient(t, 0)) == 0, "ingredients stay consumed");
                    check(ArsFixtures.orbXp(t) == 0, "no XP orbs");
                })
                .thenSucceed();
    }

    /** Native parity reference: a recipe kept native by an exact recipe override drops after the native window. */
    static void preservedRecipeIsNative(GameTestHelper h) {
        ArsFixtures.Table t = ArsFixtures.job(h, NATIVE, "ism_scribe_native");
        h.startSequence()
                .thenWaitUntil(() -> check(t.tile.crafting && ArsFixtures.gate(t) != ScribeGate.NONE, "the native job runs"))
                .thenExecute(() -> check(ArsFixtures.gate(t) == ScribeGate.EXEMPT, "an excluded recipe is never gated, got " + ArsFixtures.gate(t)))
                .thenWaitUntil(() -> check(ArsFixtures.glyphs(t) == 1, "the native glyph drops on its own"))
                .thenIdle(10)
                .thenExecute(() -> {
                    check(ArsFixtures.glyphs(t) == 1, "exactly one glyph, natively");
                    check(ArsFixtures.xp(t) == t.xpAfterCharge, "the native XP charge is the only one");
                })
                .thenSucceed();
    }

    /** Stopping keeps the job: nothing drops or refunds, and the inscription can begin again. */
    static void cancelKeepsTheJob(GameTestHelper h) {
        ArsFixtures.Table t = ArsFixtures.job(h, GATED, "ism_scribe_cancel");
        h.startSequence()
                .thenWaitUntil(() -> check(ArsFixtures.awaiting(t), "awaiting"))
                .thenExecute(() -> {
                    check(ArsFixtures.begin(t, t.player).outcome() == SessionManager.Outcome.STARTED, "started");
                    PerformanceSession s = SessionManager.session(t.player).orElseThrow();
                    SessionManager.cancelRequest(t.player, new Packets.Cancel(s.id));
                    check(TestSupport.terminal(t.player).status() == Packets.Status.CANCELLED, "stopped");
                })
                .thenIdle(150)
                .thenExecute(() -> {
                    check(ArsFixtures.awaiting(t), "the job still waits");
                    check(ArsFixtures.glyphs(t) == 0 && ArsFixtures.orbXp(t) == 0, "nothing dropped or refunded");
                    check(ArsFixtures.drops(t, ArsFixtures.ingredient(t, 0)) == 0, "ingredients stay in the job");
                    check(ArsFixtures.begin(t, t.player).outcome() == SessionManager.Outcome.STARTED, "the inscription can resume");
                })
                .thenSucceed();
    }

    /** Breaking the table mid-performance takes the one native refund path and never makes the glyph. */
    static void breakingRefundsOnce(GameTestHelper h) {
        ArsFixtures.Table t = ArsFixtures.job(h, GATED, "ism_scribe_break");
        h.startSequence()
                .thenWaitUntil(() -> check(ArsFixtures.awaiting(t), "awaiting"))
                .thenExecute(() -> {
                    check(ArsFixtures.begin(t, t.player).outcome() == SessionManager.Outcome.STARTED, "started");
                    ArsFixtures.breakTable(t);
                })
                .thenWaitUntil(() -> check(TestSupport.terminal(t.player) != null, "the session ends with the station"))
                .thenIdle(5)
                .thenExecute(() -> {
                    check(TestSupport.terminal(t.player).status() == Packets.Status.CANCELLED, "released, not completed");
                    check(ArsFixtures.glyphs(t) == 0, "no glyph from a broken table");
                    check(ArsFixtures.drops(t, ArsFixtures.ingredient(t, 0)) == 1, "each gathered ingredient is returned once");
                    check(ArsFixtures.drops(t, ArsFixtures.ingredient(t, 1)) == 1, "each gathered ingredient is returned once");
                    check(ArsFixtures.orbXp(t) == t.recipe.exp, "Ars refunds its XP once, natively: " + ArsFixtures.orbXp(t));
                })
                .thenSucceed();
    }

    /** One operator per station: a second player is refused while the first performs. */
    static void secondPlayerIsRefused(GameTestHelper h) {
        ArsFixtures.Table t = ArsFixtures.job(h, GATED, "ism_scribe_first");
        ServerPlayer second = TestSupport.player(h, "ism_scribe_second", ArsFixtures.FOOT.south());
        h.startSequence()
                .thenWaitUntil(() -> check(ArsFixtures.awaiting(t), "awaiting"))
                .thenExecute(() -> {
                    check(ArsFixtures.begin(t, t.player).outcome() == SessionManager.Outcome.STARTED, "first player starts");
                    SessionManager.BeginResult r = ArsFixtures.begin(t, second);
                    check(r.outcome() == SessionManager.Outcome.REFUSED && "station_busy".equals(r.refusal().code()),
                            "the second player is refused at the same table (either half): " + r);
                })
                .thenSucceed();
    }

    /** Forged, replayed and impossible inputs never complete anything and end the session after repeated abuse. */
    static void forgedInputsNeverComplete(GameTestHelper h) {
        ArsFixtures.Table t = ArsFixtures.job(h, GATED, "ism_scribe_forged");
        h.startSequence()
                .thenWaitUntil(() -> check(ArsFixtures.awaiting(t), "awaiting"))
                .thenExecute(() -> check(ArsFixtures.begin(t, t.player).outcome() == SessionManager.Outcome.STARTED, "started"))
                .thenIdle(30)
                .thenExecute(() -> {
                    PerformanceSession s = SessionManager.session(t.player).orElseThrow();
                    int elapsed = s.performElapsed(SessionClock.now());
                    List<InputEvent> perfect = Autoplay.play(s.spec, Autoplay.Style.CLEAN);
                    // Every input of a perfect run, claimed at once from the future: rejected without effect.
                    SessionManager.input(t.player, new Packets.Input(s.id, s.nonce, 0, 0, perfect.subList(0, Math.min(60, perfect.size()))));
                    check(!s.simulation.complete(), "a batch from the future completes nothing");
                    // A wrong nonce is ignored outright.
                    SessionManager.input(t.player, new Packets.Input(s.id, s.nonce + 1, 1, 0, List.of(InputEvent.down(elapsed, Control.PRIMARY))));
                    check(s.appliedInputs() == 0, "a forged nonce is ignored");
                    for (int i = 2; i < 6; i++)
                        SessionManager.input(t.player, new Packets.Input(s.id, s.nonce, i, 7, List.of(InputEvent.down(elapsed, Control.PRIMARY))));
                })
                .thenExecute(() -> {
                    Packets.Update end = TestSupport.terminal(t.player);
                    check(end != null && end.status() == Packets.Status.CANCELLED && "invalid_input".equals(end.code()),
                            "repeated impossible stage jumps end the session: " + end);
                    check(ArsFixtures.awaiting(t) && ArsFixtures.glyphs(t) == 0, "the job is untouched");
                })
                .thenSucceed();
    }

    private ArsScenarios() {}
}
