package com.otectus.immersivespellcraft.data;

/** The profile chosen for one operation, and in words why (shown by {@code /immersivespellcraft inspect}). */
public record ResolvedProfile(OperationProfile profile, String reason, int generation) {
}
