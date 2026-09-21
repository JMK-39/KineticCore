package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticEnvironmentRuntime;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for environment. */
public final class KineticEnvironment {
    private KineticEnvironment() {
    }

    /**
     * Returns whether client.
     */
    public static boolean isClient() {
        return KineticEnvironmentRuntime.isClient();
    }

    /**
     * Returns whether dedicated server.
     */
    public static boolean isDedicatedServer() {
        return KineticEnvironmentRuntime.isDedicatedServer();
    }

    /**
     * Performs the run on client API operation.
     */
    public static void runOnClient(Supplier<Runnable> action) {
        KineticEnvironmentRuntime.runOnClient(Objects.requireNonNull(action, "action"));
    }

    /**
     * Performs the call on client API operation.
     */
    public static <T> T callOnClient(Supplier<Supplier<T>> action, T fallback) {
        return KineticEnvironmentRuntime.callOnClient(Objects.requireNonNull(action, "action"), fallback);
    }

    /**
     * Performs the run on dedicated server API operation.
     */
    public static void runOnDedicatedServer(Supplier<Runnable> action) {
        KineticEnvironmentRuntime.runOnDedicatedServer(Objects.requireNonNull(action, "action"));
    }
}
