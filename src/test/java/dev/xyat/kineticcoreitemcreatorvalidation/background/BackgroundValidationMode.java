package dev.xyat.kineticcoreitemcreatorvalidation.background;

import java.util.concurrent.atomic.AtomicInteger;

/** Properties and diagnostics only: no native window, cursor, focus or input calls live here. */
public final class BackgroundValidationMode {
    private static final AtomicInteger MOUSE_WARPS_SKIPPED = new AtomicInteger();
    private static final AtomicInteger CURSOR_MODES_SKIPPED = new AtomicInteger();

    private BackgroundValidationMode() {}

    public static boolean enabled() { return Boolean.getBoolean("kineticcore.guiValidation.background"); }
    public static boolean hidden() { return Boolean.parseBoolean(System.getProperty("kineticcore.guiValidation.background.hidden", "true")); }
    public static int width() { return Math.max(320, Integer.getInteger("kineticcore.guiValidation.background.width", 1920)); }
    /** The one language capture fixtures render in, or null to keep each fixture's own choice. */
    public static String language() {
        String value = System.getProperty("kineticcore.guiValidation.language", "").trim();
        return value.isEmpty() ? null : value;
    }
    public static int height() { return Math.max(240, Integer.getInteger("kineticcore.guiValidation.background.height", 1080)); }

    public static void mouseWarpSkipped() {
        if (MOUSE_WARPS_SKIPPED.incrementAndGet() == 1) System.out.println("KINETIC_BACKGROUND_VALIDATION nativeCursorWarpSkipped=true");
    }

    public static void cursorModeSkipped() {
        if (CURSOR_MODES_SKIPPED.incrementAndGet() == 1) System.out.println("KINETIC_BACKGROUND_VALIDATION nativeCursorModeSkipped=true");
    }
}
