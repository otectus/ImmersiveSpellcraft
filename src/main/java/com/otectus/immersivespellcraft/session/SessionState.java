package com.otectus.immersivespellcraft.session;

/**
 * The lifecycle of a performance. Not every adapter passes through every state: a native job adapter is never
 * Suspended here, because its station already saves the job itself and an interrupted session is simply released.
 */
public enum SessionState {
    /** Native checks passed and the station is locked; the count-in is running. */
    RESERVED,
    PERFORMING,
    /** Essential work complete; revalidating before the commit. */
    READY,
    /** The one native transition is running under the one-use permit. */
    COMMITTING,
    COMPLETED,
    /** Ended before the commit: claims released, nothing native undone. */
    RELEASED,
    /** The commit's outcome could not be reconciled: the actual state is kept and nothing is reissued. */
    HELD;

    public boolean terminal() {
        return this == COMPLETED || this == RELEASED || this == HELD;
    }
}
