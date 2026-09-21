package dev.xyat.kineticcore.internal.runtime;

/** Internal adapter for the core-owned feature-switch implementation. */
public final class KineticFeatureSwitchRuntime {
    private KineticFeatureSwitchRuntime() {
    }

    /** Returns the current state of the supplied core feature switch. */
    public static boolean isEnabled(String featureId) {
        return FeatureSwitchRuntime.isEnabled(featureId);
    }
}
