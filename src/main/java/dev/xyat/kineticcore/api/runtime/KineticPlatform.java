package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticEnvironmentRuntime;
import dev.xyat.kineticcore.internal.runtime.KineticPlatformRuntime;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Provides the single platform facade for physical-side checks, game paths, and loaded-mod metadata.
 */
public final class KineticPlatform {
    /**
     * Immutable metadata for one mod currently present in the runtime.
     *
     * @param id mod identifier reported by the loader
     * @param sourcePath source JAR or classes directory when the loader exposes it; may be {@code null}
     */
    public record LoadedMod(String id, Path sourcePath) {
        /** Validates the immutable loaded-mod identifier while preserving the optional source path. */
        public LoadedMod {
            id = Objects.requireNonNull(id, "id");
        }
    }

    private KineticPlatform() {
    }

    /** Returns whether the current physical runtime is a client. */
    public static boolean isClient() {
        return KineticEnvironmentRuntime.isClient();
    }

    /** Returns whether the current physical runtime is a dedicated server. */
    public static boolean isDedicatedServer() {
        return KineticEnvironmentRuntime.isDedicatedServer();
    }

    /** Runs the supplied action only when the current physical runtime is a client. */
    public static void runOnClient(Supplier<Runnable> action) {
        KineticEnvironmentRuntime.runOnClient(Objects.requireNonNull(action, "action"));
    }

    /**
     * Evaluates the supplied client-only action on a physical client and returns {@code fallback} elsewhere.
     * The nested supplier keeps client-only classes from being resolved on a dedicated server.
     */
    public static <T> T callOnClient(Supplier<Supplier<T>> action, T fallback) {
        return KineticEnvironmentRuntime.callOnClient(Objects.requireNonNull(action, "action"), fallback);
    }

    /** Runs the supplied action only when the current physical runtime is a dedicated server. */
    public static void runOnDedicatedServer(Supplier<Runnable> action) {
        KineticEnvironmentRuntime.runOnDedicatedServer(Objects.requireNonNull(action, "action"));
    }

    /** Returns the canonical configuration directory. */
    public static Path configDirectory() {
        return KineticPlatformRuntime.configDirectory();
    }

    /** Returns the canonical game directory. */
    public static Path gameDirectory() {
        return KineticPlatformRuntime.gameDirectory();
    }

    /** Returns whether the supplied mod id is currently loaded. */
    public static boolean isModLoaded(String modId) {
        return KineticPlatformRuntime.isModLoaded(Objects.requireNonNull(modId, "modId"));
    }

    /** Returns the loader-provided display name for a mod id, falling back to the id when unavailable. */
    public static String displayName(String modId) {
        return KineticPlatformRuntime.displayName(Objects.requireNonNull(modId, "modId"));
    }

    /** Returns an immutable snapshot of mods currently reported as loaded by the platform. */
    public static List<LoadedMod> loadedMods() {
        return KineticPlatformRuntime.loadedMods();
    }
}
