package dev.xyat.kineticcore.feature.startup.config;

import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

/** Client-only settings for the title-screen startup overlay. */
public final class StartupConfig {
    public static final KTClientConfigSpec SPEC;

    private static final KTClientConfigSpec.BooleanValue SHOW_STARTUP_TIME;
    private static final KTClientConfigSpec.BooleanValue SHOW_LOGIN_INFO;
    private static final KTClientConfigSpec.IntValue ANCHOR_X;
    private static final KTClientConfigSpec.IntValue ANCHOR_Y;

    static {
        KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
        SHOW_STARTUP_TIME = builder
                .comment(
                        "是否显示游戏启动耗时。",
                        "Whether to show the game startup time."
                )
                .translation("cfg.kineticcore.startup.show_startup_time")
                .defineBoolean("show_startup_time", true);
        SHOW_LOGIN_INFO = builder
                .comment(
                        "是否显示当前登录账号信息。",
                        "Whether to show the current login account information."
                )
                .translation("cfg.kineticcore.startup.show_login_info")
                .defineBoolean("show_login_info", true);
        ANCHOR_X = builder
                .comment(
                        "整体信息显示的左侧 X 坐标。",
                        "The left X coordinate of the information overlay."
                )
                .translation("cfg.kineticcore.startup.anchor_x")
                .defineInt("anchor_x", 2, -10000, 10000);
        ANCHOR_Y = builder
                .comment(
                        "整体信息显示的顶部 Y 坐标。",
                        "The top Y coordinate of the information overlay."
                )
                .translation("cfg.kineticcore.startup.anchor_y")
                .defineInt("anchor_y", 2, -10000, 10000);
        SPEC = builder.build();
    }

    private StartupConfig() {
    }

    public static boolean showStartupTime() {
        return SHOW_STARTUP_TIME.get();
    }

    public static boolean showLoginInfo() {
        return SHOW_LOGIN_INFO.get();
    }

    public static int anchorX() {
        return ANCHOR_X.get();
    }

    public static int anchorY() {
        return ANCHOR_Y.get();
    }

    public static void save() {
        SPEC.save();
    }
}
