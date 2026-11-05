package dev.xyat.kineticcore.internal.client.gui.theme;

/**
 * The breathing of hover and selection outlines: the colour brightens towards white and back, so the state reads at a
 * glance without the outline ever fading out.
 */
public final class SelectionPulse {
    /** One breath, in milliseconds. */
    private static final long PERIOD = 1400L;

    private SelectionPulse() {}

    /** 0 at rest, 1 at the brightest point; eased in and out. */
    public static float level(long millis) {
        return (float) (0.5D - 0.5D * Math.cos(Math.floorMod(millis, PERIOD) * Math.PI * 2D / PERIOD));
    }

    /** The colour at this moment of the breath: up to 55 % of the way to white. */
    public static int color(int argb, long millis) {
        float t = 0.55F * level(millis);
        int r = argb >>> 16 & 255, g = argb >>> 8 & 255, b = argb & 255;
        r = Math.round(r + (255 - r) * t);
        g = Math.round(g + (255 - g) * t);
        b = Math.round(b + (255 - b) * t);
        return argb & 0xff000000 | r << 16 | g << 8 | b;
    }
}
