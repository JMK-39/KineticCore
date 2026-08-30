package dev.xyat.kineticcore.feature.tps.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TpsClientConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    private static final ForgeConfigSpec.ConfigValue<Double> HUD_SCALE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                "TPS/MSPT HUD 客户端显示设置。",
                "Client-side TPS/MSPT HUD settings."
        ).translation("cfg.kineticcore.tps.title").push("TpsHud");

        HUD_ENABLED = builder.comment(
                "是否显示 TPS/MSPT HUD；关闭时服务器会停止发送采样数据。",
                "Whether to show the TPS/MSPT HUD. Disabling it also stops server samples."
        ).translation("cfg.kineticcore.tps.enabled").define("enabled", true);
        HUD_OFFSET_X = builder.comment(
                "横向位置偏移，正数向左移动，负数向右移动。",
                "Horizontal position offset. Positive values move left; negative values move right."
        ).translation("cfg.kineticcore.hud.offset_x").defineInRange("offsetX", 0, -10000, 10000);
        HUD_OFFSET_Y = builder.comment(
                "纵向位置偏移，正数向上移动，负数向下移动。",
                "Vertical position offset. Positive values move up; negative values move down."
        ).translation("cfg.kineticcore.hud.offset_y").defineInRange("offsetY", 0, -10000, 10000);
        HUD_SCALE = builder.comment(
                "HUD 缩放比例；编辑界面可用鼠标滚轮调整。",
                "HUD scale; use the mouse wheel in the editor to adjust it."
        ).translation("cfg.kineticcore.hud.scale").define("scale", 1.0D, TpsClientConfig::isValidScale);

        builder.pop();
        SPEC = builder.build();
    }

    private TpsClientConfig() {
    }

    public static boolean isHudEnabled() {
        return HUD_ENABLED.get();
    }

    public static int getHudOffsetX() {
        return HUD_OFFSET_X.get();
    }

    public static int getHudOffsetY() {
        return HUD_OFFSET_Y.get();
    }

    public static double getHudScale() {
        Double value = HUD_SCALE.get();
        return value == null || !Double.isFinite(value) ? 1.0D : Math.max(0.5D, value);
    }

    public static void setHudLayout(int offsetX, int offsetY, double scale) {
        HUD_OFFSET_X.set(clamp(offsetX));
        HUD_OFFSET_Y.set(clamp(offsetY));
        HUD_SCALE.set(sanitizeScale(scale));
        SPEC.save();
    }

    public static void save() {
        SPEC.save();
    }

    private static boolean isValidScale(Object value) {
        if (!(value instanceof Number number)) return false;
        double scale = number.doubleValue();
        return Double.isFinite(scale) && scale >= 0.5D;
    }

    private static double sanitizeScale(double value) {
        if (!Double.isFinite(value)) return 1.0D;
        return Math.max(0.5D, value);
    }

    private static int clamp(int value) {
        return Math.max(-10000, Math.min(10000, value));
    }
}
