package dev.xyat.kineticcore.api.monitoring;

/** Public API type for server tick tracker. */
public final class ServerTickTracker {
    private static final int WINDOW_TICKS = 1200;

    private final long[] tickTimes = new long[WINDOW_TICKS];
    private int cursor;
    private int filled;

    /**
     * Adds tick.
     */
    public void addTick(long nanoTime) {
        if (nanoTime < 0L) throw new IllegalArgumentException("tick duration must be non-negative");
        tickTimes[cursor] = nanoTime;
        cursor = (cursor + 1) % WINDOW_TICKS;
        if (filled < WINDOW_TICKS) {
            filled++;
        }
    }

    /**
     * Returns stats.
     */
    public double getStats(int seconds, int mode) {
        // Multiply as long: Integer.MAX_VALUE seconds must not wrap to a negative sample count.
        int ticksToSample = (int) Math.min((long) seconds * 20L, filled);
        if (ticksToSample <= 0) return 0.0D;

        // A rolling long sum overflows on large (but valid) samples; a rolling
        // double sum accumulates cancellation error when those samples expire.
        // At most 1200 slots are examined, so compute from the current window.
        double totalNano = 0.0D;
        long maxNano = 0L;
        for (int i = 0; i < ticksToSample; i++) {
            int index = (cursor - 1 - i + WINDOW_TICKS) % WINDOW_TICKS;
            long time = tickTimes[index];
            totalNano += time;
            if (time > maxNano) {
                maxNano = time;
            }
        }

        if (mode == 1) return maxNano * 1.0E-6D;
        return totalNano / (double) ticksToSample * 1.0E-6D;
    }

    /**
     * Returns latest mspt.
     */
    public double getLatestMspt() {
        if (filled == 0) return 0.0D;
        return tickTimes[(cursor - 1 + WINDOW_TICKS) % WINDOW_TICKS] * 1.0E-6D;
    }

    /**
     * Performs the tps API operation.
     */
    public static double tps(double mspt) {
        // Preserve the historical cap for finite negative inputs, but do not
        // treat negative infinity as a healthy, zero-duration tick.
        if (!Double.isFinite(mspt)) return 0.0D;
        return Math.min(20.0D, 1000.0D / Math.max(mspt, 0.001D));
    }
}
