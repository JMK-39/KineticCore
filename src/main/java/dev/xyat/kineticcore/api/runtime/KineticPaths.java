package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticPlatformRuntime;

import java.nio.file.Path;

public final class KineticPaths {
    private KineticPaths() {
    }

    public static Path configDirectory() {
        return KineticPlatformRuntime.configDirectory();
    }

    public static Path gameDirectory() {
        return KineticPlatformRuntime.gameDirectory();
    }
}
