package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticEnvironmentRuntime;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticEnvironment {
    private KineticEnvironment() {
    }

    public static boolean isClient() {
        return KineticEnvironmentRuntime.isClient();
    }

    public static boolean isDedicatedServer() {
        return KineticEnvironmentRuntime.isDedicatedServer();
    }

    public static void runOnClient(Supplier<Runnable> action) {
        KineticEnvironmentRuntime.runOnClient(Objects.requireNonNull(action, "action"));
    }

    public static <T> T callOnClient(Supplier<Supplier<T>> action, T fallback) {
        return KineticEnvironmentRuntime.callOnClient(Objects.requireNonNull(action, "action"), fallback);
    }

    public static void runOnDedicatedServer(Supplier<Runnable> action) {
        KineticEnvironmentRuntime.runOnDedicatedServer(Objects.requireNonNull(action, "action"));
    }
}
