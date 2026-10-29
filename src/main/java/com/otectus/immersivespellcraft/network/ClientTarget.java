package com.otectus.immersivespellcraft.network;

/**
 * The client's receiving side, installed by client setup. Common code only ever calls through this interface, so a
 * dedicated server never loads a client class while handling (or registering) a message.
 */
public interface ClientTarget {
    void started(Packets.Started message);
    void update(Packets.Update message);
    void resync(Packets.Resync message);
    void refused(Packets.Refused message);
    void notice(Packets.Notice message);
    void openPractice(Packets.OpenPractice message);
}
