package dev.xyat.kineticcore.api.client.gpu;

import dev.xyat.kineticcore.internal.client.gpu.GpuMemLeakFixHandler;

/**
 * Installs Kinetic's client-side cleanup for leaked render-target resources.
 */
public final class KineticGpuCleanup {
    private KineticGpuCleanup() {
    }

    /** Registers the cleanup runtime once for the current client process. */
    public static void register() {
        GpuMemLeakFixHandler.register();
    }
}
