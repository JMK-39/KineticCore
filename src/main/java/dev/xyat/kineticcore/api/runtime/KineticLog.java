package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticLogRuntime;

/**
 * Provides implementation-independent runtime logging for Kinetic business modules.
 */
public final class KineticLog {
    private KineticLog() {
    }

    /** Records an error message together with its cause. */
    public static void error(String message, Throwable throwable) {
        KineticLogRuntime.error(message, throwable);
    }
}
