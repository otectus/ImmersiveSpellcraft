package com.otectus.immersivespellcraft.test;

import com.otectus.immersivespellcraft.minigame.Autoplay;
import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

import static com.otectus.immersivespellcraft.test.TestSupport.check;

/** N01/N02 Glyph Press: the native job gated at completion only. Runs only with Ars 'n Spells and Iron's active. */
final class AnsScenarios {
    static final String FIREBOLT = "irons_spellbooks:firebolt";

    /** Held past its native duration until embossed; then the Press completes once with exactly its own costs. */
    static void heldUntilEmbossed(GameTestHelper h) {
        AnsFixtures.Press p = AnsFixtures.press(h, FIREBOLT, 1, 1);
        ServerPlayer player = TestSupport.player(h, "ism_press_held", AnsFixtures.PRESS.south());
        AnsFixtures.open(p, player);
        check("started".equals(AnsFixtures.start(p, player)), "the Press starts its own job");
        check(AnsFixtures.gated(p), "a player's job is gated");
        check(SessionManager.session(player).isPresent(), "the embossing begins with the job");
        int duration = AnsFixtures.duration(p);
        h.startSequence()
                .thenIdle(duration + 40)
                .thenExecute(() -> {
                    check(AnsFixtures.progress(p) >= duration, "the native window has run in full");
                    check(AnsFixtures.working(p) && AnsFixtures.output(p).isEmpty(), "the job is held, not finished");
                    check(AnsFixtures.count(p, AnsFixtures.slotScroll()) == 1, "nothing is consumed while held");
                    p.driver = new TestSupport.Driver(player, Autoplay.Style.CLEAN);
                })
                .thenWaitUntil(() -> {
                    p.driver.tick();
                    check(p.driver.finished(), "embossing");
                })
                .thenExecute(() -> check(TestSupport.terminal(player).status() == Packets.Status.COMPLETE, "embossed: " + TestSupport.terminal(player)))
                .thenWaitUntil(() -> check(!AnsFixtures.output(p).isEmpty(), "the Press completes"))
                .thenExecute(() -> {
                    check(AnsFixtures.output(p).getCount() == 1, "exactly one glyph");
                    check(AnsFixtures.count(p, AnsFixtures.slotScroll()) == 0, "one scroll consumed");
                    check(AnsFixtures.count(p, AnsFixtures.slotPlate()) == p.platesBefore - 1, "one plate consumed");
                    check(AnsFixtures.count(p, AnsFixtures.slotCatalyst()) == p.gemsBefore - 1, "the tier's one gem consumed");
                    check(p.sourceBefore - AnsFixtures.source(p) == 500, "exactly the tier's 500 Source: " + (p.sourceBefore - AnsFixtures.source(p)));
                    check(!AnsFixtures.working(p), "the job is done");
                })
                .thenSucceed();
    }

    /** Embossing finished early never shortens the Press's own processing window. */
    static void neverShortensTheNativeWindow(GameTestHelper h) {
        AnsFixtures.Press p = AnsFixtures.press(h, FIREBOLT, 1, 1);
        ServerPlayer player = TestSupport.player(h, "ism_press_window", AnsFixtures.PRESS.south());
        AnsFixtures.open(p, player);
        check("started".equals(AnsFixtures.start(p, player)), "started");
        p.driver = new TestSupport.Driver(player, Autoplay.Style.CLEAN);
        long started = h.getLevel().getGameTime();
        int duration = AnsFixtures.duration(p);
        h.startSequence()
                .thenWaitUntil(() -> {
                    p.driver.tick();
                    check(!AnsFixtures.output(p).isEmpty(), "waiting for the Press");
                })
                .thenExecute(() -> check(h.getLevel().getGameTime() - started >= duration,
                        "the glyph appeared after " + (h.getLevel().getGameTime() - started) + " ticks; the window is " + duration))
                .thenSucceed();
    }

    /** The Press's own Cancel ends the session; inputs and the reserved Source come back untouched. */
    static void nativeCancelEndsTheSession(GameTestHelper h) {
        AnsFixtures.Press p = AnsFixtures.press(h, FIREBOLT, 1, 1);
        ServerPlayer player = TestSupport.player(h, "ism_press_cancel", AnsFixtures.PRESS.south());
        AnsFixtures.open(p, player);
        check("started".equals(AnsFixtures.start(p, player)), "started");
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> AnsFixtures.cancel(p))
                .thenWaitUntil(() -> check(TestSupport.terminal(player) != null, "the session notices the cancel"))
                .thenExecute(() -> {
                    check(TestSupport.terminal(player).status() == Packets.Status.CANCELLED, "released");
                    check(!AnsFixtures.working(p) && AnsFixtures.output(p).isEmpty(), "a cancelled job never finishes");
                    check(AnsFixtures.count(p, AnsFixtures.slotScroll()) == 1 && AnsFixtures.count(p, AnsFixtures.slotCatalyst()) == 1, "inputs untouched");
                    check(AnsFixtures.source(p) == p.sourceBefore, "the reservation returned to the buffer");
                })
                .thenIdle(120)
                .thenExecute(() -> check(AnsFixtures.output(p).isEmpty(), "still nothing made"))
                .thenSucceed();
    }

    /** Only the job's starter may emboss it; the starter can resume after stopping. */
    static void onlyTheStarterEmbosses(GameTestHelper h) {
        AnsFixtures.Press p = AnsFixtures.press(h, FIREBOLT, 1, 1);
        ServerPlayer starter = TestSupport.player(h, "ism_press_starter", AnsFixtures.PRESS.south());
        ServerPlayer other = TestSupport.player(h, "ism_press_other", AnsFixtures.PRESS.north());
        var menu = AnsFixtures.open(p, starter);
        check("started".equals(AnsFixtures.start(p, starter)), "started");
        var session = SessionManager.session(starter).orElseThrow();
        SessionManager.cancelRequest(starter, new Packets.Cancel(session.id));
        var otherMenu = AnsFixtures.open(p, other);
        SessionManager.beginFromMenu(other, new Packets.Begin(otherMenu.containerId, otherMenu.getStateId(), -1, 0, -1));
        check(SessionManager.session(other).isEmpty(), "another player cannot emboss this job");
        check(TestSupport.sent(other, Packets.Refused.class).stream().anyMatch(r -> r.code().equals("not_your_job")), "refused: not their job");
        h.startSequence()
                .thenIdle(6)
                .thenExecute(() -> {
                    SessionManager.beginFromMenu(starter, new Packets.Begin(menu.containerId, menu.getStateId(), -1, 0, -1));
                    check(SessionManager.session(starter).isPresent(), "the starter resumes");
                    check(AnsFixtures.gateSurvivesReload(p), "the gate is saved with the press, under the same job");
                })
                .thenSucceed();
    }

    /** Automation (a fake player) keeps the native Press exactly: no gate, completion at the native time. */
    static void automationIsNative(GameTestHelper h) {
        AnsFixtures.Press p = AnsFixtures.press(h, FIREBOLT, 1, 1);
        FakePlayer automaton = TestSupport.automaton(h, AnsFixtures.PRESS.south());
        AnsFixtures.open(p, automaton);
        check("started".equals(AnsFixtures.start(p, automaton)), "the Press itself decides automation may start here");
        check(!AnsFixtures.gated(p), "automation is never gated");
        h.startSequence()
                .thenWaitUntil(() -> check(!AnsFixtures.output(p).isEmpty(), "native completion"))
                .thenExecute(() -> check(AnsFixtures.output(p).getCount() == 1, "one glyph"))
                .thenSucceed();
    }

    private AnsScenarios() {}
}
