package dev.xyat.kineticcore.feature.flight.config;

import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

public final class SuperFlightClientConfig {
    public static final double DEFAULT_SELECTED_SPEED = 20.0D;
    public static final double MIN_SELECTED_SPEED = 1.0D;
    public static final double MAX_SELECTED_SPEED = 100.0D;
    public static final KTClientConfigSpec SPEC;

    private static final KTClientConfigSpec.DoubleValue SELECTED_SPEED;

    static {
        KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
        builder.comment(
                "超人飞行记忆的目标极速倍率，由游戏内 Shift + 滚轮调整。",
                "Remembered super-flight target speed multiplier, adjusted in-game with Shift + mouse wheel."
        ).push("SuperFlight");

        SELECTED_SPEED = builder.defineDouble(
                "selectedSpeed",
                DEFAULT_SELECTED_SPEED,
                MIN_SELECTED_SPEED,
                MAX_SELECTED_SPEED
        );

        builder.pop();
        SPEC = builder.build();
    }

    private SuperFlightClientConfig() {
    }

    public static double selectedSpeed() {
        Double value = SELECTED_SPEED.get();
        if (value == null || !Double.isFinite(value)) return DEFAULT_SELECTED_SPEED;
        return Math.max(MIN_SELECTED_SPEED, Math.min(MAX_SELECTED_SPEED, value));
    }

    public static void setSelectedSpeed(double value) {
        double sanitized = Math.max(MIN_SELECTED_SPEED, Math.min(MAX_SELECTED_SPEED, value));
        SELECTED_SPEED.set(sanitized);
        SPEC.save();
    }
}
