package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticFeatureSwitchRuntime;

/**
 * Provides the runtime facade for querying named Kinetic feature switches without exposing their implementation owner.
 */
public final class KineticFeatures {
    private KineticFeatures() {
    }

    /** Returns whether the named Kinetic feature switch is currently enabled. */
    public static boolean isEnabled(String featureId) {
        return KineticFeatureSwitchRuntime.isEnabled(featureId);
    }
}
