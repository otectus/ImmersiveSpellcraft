package com.otectus.immersivespellcraft.client;

import com.otectus.immersivespellcraft.minigame.GameSpec;
import com.otectus.immersivespellcraft.minigame.ScoreCard;
import com.otectus.immersivespellcraft.minigame.Simulation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** What the performance panel shows: a live session (server-judged) or an isolated practice run. */
public interface PanelModel {
    enum Phase { COUNT_IN, PERFORMING, FINISHING, RESULT, ENDED }

    GameSpec spec();
    Simulation simulation();
    Phase phase();
    /** Performance time in milliseconds; negative during the count-in. */
    int now();
    Component title();
    List<Component> costs();
    ItemStack output();
    boolean practice();
    @Nullable ScoreCard result();
    /** The result summary, or why the session ended. */
    Component message();
    String instructionKey();
    int nativeWindowMs();
    CueFeed cues();
}
