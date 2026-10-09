package dev.xyat.kineticcore.feature.datapack.recovery.client;

/** Internal bridge implemented on the vanilla failure screen. */
public interface FailureCallbacks {
    Runnable kineticcore$retry();
    Runnable kineticcore$cancel();
}
