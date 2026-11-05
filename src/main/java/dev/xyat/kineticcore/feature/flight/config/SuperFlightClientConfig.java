package dev.xyat.kineticcore.feature.flight.config;

import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

public final class SuperFlightClientConfig {
    public static final double DEFAULT_SELECTED_SPEED = 20.0D;
    public static final double MIN_SELECTED_SPEED = 1.0D;
    public static final double MAX_SELECTED_SPEED = 100.0D;
    public static final KTClientConfigSpec SPEC;

    private static final KTClientConfigSpec.DoubleValue SELECTED_SPEED;
    public static final KTClientConfigSpec.BooleanValue EFFECTS;
    public static final KTClientConfigSpec.BooleanValue WIND_SOUND;
    public static final KTClientConfigSpec.DoubleValue WIND_VOLUME;
    public static final KTClientConfigSpec.DoubleValue PARTICLES;
    public static final KTClientConfigSpec.BooleanValue SPEED_LINES;
    public static final KTClientConfigSpec.IntValue BARRIER_SPEED;
    public static final KTClientConfigSpec.BooleanValue VAPOR_CONE;
    public static final KTClientConfigSpec.BooleanValue SONIC_BOOM;
    public static final KTClientConfigSpec.DoubleValue BOOM_VOLUME;
    public static final KTClientConfigSpec.BooleanValue TAKEOFF_BOOM;
    public static final KTClientConfigSpec.IntValue TAKEOFF_SPEED;
    public static final KTClientConfigSpec.BooleanValue PLASMA;
    public static final KTClientConfigSpec.IntValue VAPOR_COLOR;
    public static final KTClientConfigSpec.IntValue PLASMA_COLOR;

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

        builder.comment(
                "超人飞行的音效与特效（只影响本客户端）。速度单位为米/秒（格/秒）。",
                "Super-flight sounds and effects (this client only). Speeds are in metres (blocks) per second."
        ).translation("cfg.kineticcore.super_flight_effects.title").push("SuperFlightEffects");
        EFFECTS = builder.translation("cfg.kineticcore.super_flight_effects.enabled").defineBoolean("enabled", true);
        WIND_SOUND = builder.translation("cfg.kineticcore.super_flight_effects.wind_sound").defineBoolean("windSound", true);
        WIND_VOLUME = builder.translation("cfg.kineticcore.super_flight_effects.wind_volume").defineDouble("windVolume", 0.8D, 0.0D, 1.0D);
        PARTICLES = builder.translation("cfg.kineticcore.super_flight_effects.particles").defineDouble("particles", 1.0D, 0.0D, 2.0D);
        SPEED_LINES = builder.translation("cfg.kineticcore.super_flight_effects.speed_lines").defineBoolean("speedLines", true);
        BARRIER_SPEED = builder.translation("cfg.kineticcore.super_flight_effects.barrier_speed").defineInt("barrierSpeed", 320, 50, 2000);
        VAPOR_CONE = builder.translation("cfg.kineticcore.super_flight_effects.vapor_cone").defineBoolean("vaporCone", true);
        SONIC_BOOM = builder.translation("cfg.kineticcore.super_flight_effects.sonic_boom").defineBoolean("sonicBoom", true);
        BOOM_VOLUME = builder.translation("cfg.kineticcore.super_flight_effects.boom_volume").defineDouble("boomVolume", 1.0D, 0.0D, 2.0D);
        TAKEOFF_BOOM = builder.translation("cfg.kineticcore.super_flight_effects.takeoff_boom").defineBoolean("takeoffBoom", true);
        TAKEOFF_SPEED = builder.translation("cfg.kineticcore.super_flight_effects.takeoff_speed").defineInt("takeoffSpeed", 60, 20, 400);
        PLASMA = builder.translation("cfg.kineticcore.super_flight_effects.plasma").defineBoolean("plasma", true);
        VAPOR_COLOR = builder.translation("cfg.kineticcore.super_flight_effects.vapor_color").defineInt("vaporColor", 0xE6F2FF, 0, 0xFFFFFF);
        PLASMA_COLOR = builder.translation("cfg.kineticcore.super_flight_effects.plasma_color").defineInt("plasmaColor", 0xFF7A2E, 0, 0xFFFFFF);
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
