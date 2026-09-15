package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticModLifecycleRuntime;

import java.util.Objects;

public final class KineticModLifecycle {
    private KineticModLifecycle() {
    }

    public static void onCommonSetup(Runnable action) {
        KineticModLifecycleRuntime.onCommonSetup(Objects.requireNonNull(action, "action"));
    }

    public static void onClientSetup(Runnable action) {
        KineticModLifecycleRuntime.onClientSetup(Objects.requireNonNull(action, "action"));
    }

    public static void onLoadComplete(Runnable action) {
        KineticModLifecycleRuntime.onLoadComplete(Objects.requireNonNull(action, "action"));
    }
}
