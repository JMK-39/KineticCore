package dev.xyat.kineticcore.api.monitoring;

import java.util.Objects;

/**
 * Rolling window of the last 1200 server tick durations (one minute at 20 TPS).
 *
 * <p>Not thread-safe: the server records ticks on the server thread, so read it from the server thread too, for
 * example inside a command.
 */
public final class ServerTickTracker {
    /** Statistic returned by {@link #getStats(int, Stat)}. */
    public enum Stat {
        /** Mean tick duration. */
        AVERAGE,
        /** Longest tick duration. */
        MAXIMUM
    }

    private static final int WINDOW_TICKS = 1200;

    private final long[] tickTimes = new long[WINDOW_TICKS];
    private int cursor;
    private int filled;

    /**
     * Records one tick duration, replacing the oldest sample once the window is full.
     *
     * @param nanoTime tick duration in nanoseconds
     * @throws IllegalArgumentException if {@code nanoTime} is negative
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
     * Returns a statistic over the most recent samples.
     *
     * @param seconds how far back to look; capped at the recorded window of 60 seconds
     * @param stat whether to return the average or the longest tick
     * @return milliseconds per tick, or {@code 0} when there are no samples
     * @throws NullPointerException if {@code stat} is {@code null}
     */
    public double getStats(int seconds, Stat stat) {
        Objects.requireNonNull(stat, "stat");
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

        if (stat == Stat.MAXIMUM) return maxNano * 1.0E-6D;
        return totalNano / (double) ticksToSample * 1.0E-6D;
    }

    /** Returns the duration of the most recent tick in milliseconds, or {@code 0} before the first tick. */
    public double getLatestMspt() {
        if (filled == 0) return 0.0D;
        return tickTimes[(cursor - 1 + WINDOW_TICKS) % WINDOW_TICKS] * 1.0E-6D;
    }

    /**
     * Converts milliseconds per tick to ticks per second.
     *
     * @param mspt average milliseconds per tick
     * @return {@code 1000 / mspt} capped at 20; {@code 0} for non-finite input
     */
    public static double tps(double mspt) {
        // Preserve the historical cap for finite negative inputs, but do not
        // treat negative infinity as a healthy, zero-duration tick.
        if (!Double.isFinite(mspt)) return 0.0D;
        return Math.min(20.0D, 1000.0D / Math.max(mspt, 0.001D));
    }
}
