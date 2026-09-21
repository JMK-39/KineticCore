package dev.xyat.kineticcore.internal.client.input;

/** Internal screen-space camera input transforms shared by public client input APIs. */
public final class KineticCameraInputRuntime {
    private KineticCameraInputRuntime() {
    }

    public static Delta rotateForRoll(double horizontal, double vertical, float rollDegrees) {
        if (!Double.isFinite(horizontal) || !Double.isFinite(vertical) || !Float.isFinite(rollDegrees)) {
            return new Delta(horizontal, vertical);
        }
        if (Math.abs(rollDegrees) < 0.0001F) {
            return new Delta(horizontal, vertical);
        }

        double radians = Math.toRadians(rollDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Delta(
                horizontal * cos - vertical * sin,
                horizontal * sin + vertical * cos
        );
    }

    public record Delta(double horizontal, double vertical) {
    }
}
