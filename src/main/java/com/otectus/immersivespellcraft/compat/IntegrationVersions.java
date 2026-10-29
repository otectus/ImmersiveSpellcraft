package com.otectus.immersivespellcraft.compat;

import com.otectus.immersivespellcraft.core.Operations;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * The versions each integration was built and tested against. A mod outside its range stays fully native: its
 * adapters are dormant and its mixins are not applied. Ranges are checked here rather than in mods.toml so an
 * unsupported version is a clear diagnostic instead of a refusal to launch.
 */
public final class IntegrationVersions {
    /** Inclusive minimum, exclusive maximum; Iron's versions are compared without their "1.20.1-" prefix. */
    public record Range(String modId, String name, int[] min, int[] max, String text) {}

    public static final Map<String, Range> RANGES = Map.of(
            Operations.ARS, new Range(Operations.ARS, "Ars Nouveau", new int[]{4, 12, 7}, new int[]{4, 13, 0}, "[4.12.7, 4.13)"),
            Operations.IRONS, new Range(Operations.IRONS, "Iron's Spells 'n Spellbooks", new int[]{3, 15, 0}, new int[]{3, 17, 0},
                    "[1.20.1-3.15.0, 1.20.1-3.17) (tested: 3.15.0, 3.16.3)"),
            Operations.ANS, new Range(Operations.ANS, "Ars 'n Spells", new int[]{3, 4, 0}, new int[]{3, 5, 0}, "[3.4.0, 3.5)"));

    public record Status(String modId, boolean loaded, @Nullable String version, boolean supported, String range) {}

    /** At runtime (after mod construction). */
    public static Status status(String modId) {
        Range range = RANGES.get(modId);
        Optional<String> version = ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getVersion().toString());
        return status(modId, range, version.orElse(null));
    }

    /** During mixin configuration, before mods are constructed. */
    public static boolean supportedAtLaunch(String modId) {
        LoadingModList list = LoadingModList.get();
        if (list == null) return false;
        ModFileInfo file = list.getModFileById(modId);
        if (file == null) return false;
        for (IModInfo info : file.getMods()) {
            if (info.getModId().equals(modId)) return status(modId, RANGES.get(modId), info.getVersion().toString()).supported();
        }
        return false;
    }

    private static Status status(String modId, @Nullable Range range, @Nullable String version) {
        if (range == null) return new Status(modId, version != null, version, false, "unknown");
        if (version == null) return new Status(modId, false, null, false, range.text());
        int[] v = parse(modId.equals(Operations.IRONS) && version.startsWith("1.20.1-") ? version.substring(7) : version);
        boolean ok = v != null && compare(v, range.min()) >= 0 && compare(v, range.max()) < 0;
        return new Status(modId, true, version, ok, range.text());
    }

    @Nullable
    static int[] parse(String version) {
        String[] parts = version.split("[.\\-+]");
        int[] out = new int[3];
        int n = 0;
        for (String p : parts) {
            if (n == 3) break;
            if (p.isEmpty() || !Character.isDigit(p.charAt(0))) break;
            int end = 0;
            while (end < p.length() && Character.isDigit(p.charAt(end))) end++;
            try {
                out[n++] = Integer.parseInt(p.substring(0, end));
            } catch (NumberFormatException e) {
                return null;
            }
            if (end < p.length()) break;
        }
        return n == 0 ? null : out;
    }

    private static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        return 0;
    }

    private IntegrationVersions() {}
}
