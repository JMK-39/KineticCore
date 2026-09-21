package dev.xyat.kineticcore.api.client.input;

import dev.xyat.kineticcore.internal.client.input.KineticCameraInputRuntime;

/** Public camera-input helpers for keeping controls aligned with a rolled viewport. */
public final class KineticCameraInput {
    private KineticCameraInput() {
    }

    /**
     * Rotates horizontal/vertical look input into the current rolled screen axes.
     * The returned deltas can be passed to the normal player camera turn path.
     */
    public static MouseDelta rotateForRoll(double horizontal, double vertical, float rollDegrees) {
        KineticCameraInputRuntime.Delta delta = KineticCameraInputRuntime.rotateForRoll(horizontal, vertical, rollDegrees);
        return new MouseDelta(delta.horizontal(), delta.vertical());
    }

    public record MouseDelta(double horizontal, double vertical) {
    }
}
