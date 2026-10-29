package com.otectus.immersivespellcraft.core;

/**
 * How an adapter's native operation relates to the performance. The three kinds interrupt differently and never
 * share a rollback implementation:
 *
 * <ul>
 *   <li>{@link #IMMEDIATE_COMMIT}: nothing native happens until the commit invokes the native action once. Interrupting
 *       releases the claim; there is nothing to undo.</li>
 *   <li>{@link #EXISTING_NATIVE_JOB}: the station already runs (and saves) its own job. The adapter gates only that
 *       job's successful finish; interrupting leaves the job, its costs and its native cancel path untouched.</li>
 *   <li>{@link #NATIVE_ONGOING_EFFECT}: the native operation is an effect that keeps running (rituals). Its own
 *       lifecycle governs after activation; no refund while it continues. No 0.1.0 adapter uses it.</li>
 * </ul>
 */
public enum AdapterCapability {
    IMMEDIATE_COMMIT, EXISTING_NATIVE_JOB, NATIVE_ONGOING_EFFECT
}
