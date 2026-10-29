package com.otectus.immersivespellcraft.test;

import com.otectus.immersivespellcraft.minigame.Autoplay;
import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import static com.otectus.immersivespellcraft.test.TestSupport.check;

/** I01 Scroll Forge: guarded takes, native parity, batching, interruption. Runs only with Iron's active. */
final class IronsScenarios {
    static final BlockPos A = new BlockPos(1, 2, 2), B = new BlockPos(3, 2, 2);

    /** Every direct route to the result is refused without a performance, and nothing is consumed. */
    static void directTakeIsGuarded(GameTestHelper h) {
        ServerPlayer p = TestSupport.player(h, "ism_forge_direct", A.south());
        IronsFixtures.Forge f = IronsFixtures.forge(h, A, p, 1);
        int slot = IronsFixtures.result(f);
        for (ClickType type : new ClickType[]{ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.SWAP, ClickType.THROW})
            f.menu.clicked(slot, type == ClickType.SWAP ? 3 : 0, type, p);
        check(f.menu.getCarried().isEmpty() && IronsFixtures.scrollsIn(p) == 0, "no scroll reached the player");
        check(IronsFixtures.count(f, 0) == 1 && IronsFixtures.count(f, 1) == 1 && IronsFixtures.count(f, 2) == 1, "nothing consumed");
        check(f.menu.getResultSlot().hasItem(), "the preview is still a preview");
        h.succeed();
    }

    /** The performed take yields exactly the scroll and consumption of the native take made by automation. */
    static void performedTakeMatchesNative(GameTestHelper h) {
        TestSupport.floor(h);
        ServerPlayer p = TestSupport.player(h, "ism_forge_performer", A.south());
        FakePlayer automaton = TestSupport.automaton(h, B.south());
        IronsFixtures.Forge performed = IronsFixtures.forge(h, A, p, 2);
        IronsFixtures.Forge reference = IronsFixtures.forge(h, B, automaton, 2);
        ItemStack preview = IronsFixtures.preview(performed);
        reference.menu.clicked(IronsFixtures.result(reference), 0, ClickType.PICKUP, automaton);
        ItemStack nativeScroll = reference.menu.getCarried().copy();
        check(IronsFixtures.isFirebolt(nativeScroll), "automation takes the native scroll: " + nativeScroll);
        IronsFixtures.begin(performed, ClickType.PICKUP);
        check(SessionManager.session(p).isPresent(), "the click began a performance: " + TestSupport.sent(p, Packets.Refused.class));
        performed.driver = new TestSupport.Driver(p, Autoplay.Style.CLEAN);
        h.startSequence()
                .thenWaitUntil(() -> {
                    performed.driver.tick();
                    check(performed.driver.finished(), "performing");
                })
                .thenExecute(() -> {
                    check(TestSupport.terminal(p).status() == Packets.Status.COMPLETE, "complete: " + TestSupport.terminal(p));
                    ItemStack scroll = performed.menu.getCarried();
                    check(ItemStack.matches(scroll, nativeScroll), "the same scroll as the native take: " + scroll + " vs " + nativeScroll);
                    check(ItemStack.matches(scroll, preview), "the preview became the result, unchanged");
                    for (int slot = 0; slot < 3; slot++)
                        check(IronsFixtures.count(performed, slot) == IronsFixtures.count(reference, slot),
                                "slot " + slot + " consumed exactly as natively");
                    check(IronsFixtures.count(performed, 2) == 1, "the focus is consumed, as natively");
                })
                .thenSucceed();
    }

    /** Shift-click keeps its native meaning under one performance: repeat while inputs and space last. */
    static void shiftClickRepeatsNatively(GameTestHelper h) {
        TestSupport.floor(h);
        ServerPlayer p = TestSupport.player(h, "ism_forge_batch", A.south());
        FakePlayer automaton = TestSupport.automaton(h, B.south());
        IronsFixtures.Forge performed = IronsFixtures.forge(h, A, p, 3);
        IronsFixtures.Forge reference = IronsFixtures.forge(h, B, automaton, 3);
        reference.menu.clicked(IronsFixtures.result(reference), 0, ClickType.QUICK_MOVE, automaton);
        int nativeCount = IronsFixtures.scrollsIn(automaton);
        IronsFixtures.begin(performed, ClickType.QUICK_MOVE);
        performed.driver = new TestSupport.Driver(p, Autoplay.Style.CLEAN);
        h.startSequence()
                .thenWaitUntil(() -> {
                    performed.driver.tick();
                    check(performed.driver.finished(), "performing");
                })
                .thenExecute(() -> {
                    check(TestSupport.terminal(p).status() == Packets.Status.COMPLETE, "complete");
                    check(nativeCount == 3, "natively, shift-click forges all three");
                    check(IronsFixtures.scrollsIn(p) == nativeCount, "the performed shift-click forges as many: " + IronsFixtures.scrollsIn(p));
                    for (int slot = 0; slot < 3; slot++) check(IronsFixtures.count(performed, slot) == 0, "each unit paid its own inputs");
                })
                .thenSucceed();
    }

    /** A hopper-style change to the inputs releases the session; nothing is taken or consumed. */
    static void changedInputsRelease(GameTestHelper h) {
        ServerPlayer p = TestSupport.player(h, "ism_forge_changed", A.south());
        IronsFixtures.Forge f = IronsFixtures.forge(h, A, p, 2);
        IronsFixtures.begin(f, ClickType.PICKUP);
        check(SessionManager.session(p).isPresent(), "began");
        f.tile.getItemHandler().extractItem(0, 1, false);
        h.startSequence()
                .thenWaitUntil(() -> check(TestSupport.terminal(p) != null, "the session notices the change"))
                .thenExecute(() -> {
                    Packets.Update end = TestSupport.terminal(p);
                    check(end.status() == Packets.Status.CANCELLED && "inputs_changed".equals(end.code()), "released: " + end);
                    check(f.menu.getCarried().isEmpty(), "no scroll");
                    check(IronsFixtures.count(f, 1) == 2 && IronsFixtures.count(f, 2) == 2, "nothing consumed");
                })
                .thenSucceed();
    }

    /** A begin request against a stale menu revision changes nothing. */
    static void staleBeginChangesNothing(GameTestHelper h) {
        ServerPlayer p = TestSupport.player(h, "ism_forge_stale", A.south());
        IronsFixtures.Forge f = IronsFixtures.forge(h, A, p, 1);
        SessionManager.beginFromMenu(p, new Packets.Begin(f.menu.containerId, f.menu.getStateId() + 7, IronsFixtures.result(f), 0,
                ClickType.PICKUP.ordinal()));
        check(SessionManager.session(p).isEmpty(), "no session from a stale request");
        check(TestSupport.sent(p, Packets.Refused.class).stream().anyMatch(r -> r.code().equals("changed")), "refused as changed");
        check(f.menu.getCarried().isEmpty() && IronsFixtures.count(f, 0) == 1, "nothing moved");
        h.succeed();
    }

    /** While one player performs, a second viewer of the same forge cannot touch its slots. */
    static void secondViewerIsLockedOut(GameTestHelper h) {
        ServerPlayer p = TestSupport.player(h, "ism_forge_owner", A.south());
        ServerPlayer other = TestSupport.player(h, "ism_forge_other", A.north());
        IronsFixtures.Forge f = IronsFixtures.forge(h, A, p, 2);
        IronsFixtures.begin(f, ClickType.PICKUP);
        check(SessionManager.session(p).isPresent(), "began");
        var otherMenu = f.tile.createMenu(f.menu.containerId + 50, other.getInventory(), other);
        other.containerMenu = otherMenu;
        otherMenu.clicked(36, 0, ClickType.PICKUP, other);
        check(otherMenu.getCarried().isEmpty() && IronsFixtures.count(f, 0) == 2, "the second viewer cannot take the ink");
        h.succeed();
    }

    private IronsScenarios() {}
}
