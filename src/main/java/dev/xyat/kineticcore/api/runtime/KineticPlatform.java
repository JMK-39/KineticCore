package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticPlatformRuntime;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class KineticPlatform {
    public record LoadedMod(String id, Path sourcePath) {
        public LoadedMod {
            id = Objects.requireNonNull(id, "id");
        }
    }

    private KineticPlatform() {
    }

    public static boolean isModLoaded(String modId) {
        return KineticPlatformRuntime.isModLoaded(Objects.requireNonNull(modId, "modId"));
    }

    public static String displayName(String modId) {
        return KineticPlatformRuntime.displayName(Objects.requireNonNull(modId, "modId"));
    }

    public static List<LoadedMod> loadedMods() {
        return KineticPlatformRuntime.loadedMods();
    }
}
