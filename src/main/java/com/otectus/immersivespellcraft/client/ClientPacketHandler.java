package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.network.ClientTarget;
import com.otectus.immersivespellcraft.network.Packets;
import net.minecraft.client.Minecraft;

/** Receives every server message on the client thread. */
final class ClientPacketHandler implements ClientTarget {
    @Override public void started(Packets.Started m) { ClientSession.started(m); }
    @Override public void update(Packets.Update m) { ClientSession.update(m); }
    @Override public void resync(Packets.Resync m) { ClientSession.resync(m); }
    @Override public void refused(Packets.Refused m) { ClientSession.refused(m); }
    @Override public void notice(Packets.Notice m) { ClientSession.notice(m); }

    @Override
    public void openPractice(Packets.OpenPractice m) {
        Minecraft.getInstance().setScreen(new PracticeScreen(m.game()));
    }
}
