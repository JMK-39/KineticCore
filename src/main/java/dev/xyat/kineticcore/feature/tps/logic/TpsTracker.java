package dev.xyat.kineticcore.feature.tps.logic;

public final class TpsTracker {
    private static final int WINDOW_TICKS = 1200;

    private final long[] tickTimes = new long[WINDOW_TICKS];
    private int cursor;
    private int filled;
    private long rollingSumNano;

    public void addTick(long nanoTime) {
        if (filled == WINDOW_TICKS) {
            rollingSumNano -= tickTimes[cursor];
        }

        tickTimes[cursor] = nanoTime;
        rollingSumNano += nanoTime;
        cursor = (cursor + 1) % WINDOW_TICKS;
        if (filled < WINDOW_TICKS) {
            filled++;
        }
    }

    public double getStats(int seconds, int mode) {
        int ticksToSample = Math.min(seconds * 20, filled);
        if (ticksToSample <= 0) return 0.0D;

        if (mode == 0 && ticksToSample == WINDOW_TICKS) {
            return rollingSumNano / (double) WINDOW_TICKS * 1.0E-6D;
        }

        long totalNano = 0L;
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

    public double getLatestMspt() {
        if (filled == 0) return 0.0D;
        return tickTimes[(cursor - 1 + WINDOW_TICKS) % WINDOW_TICKS] * 1.0E-6D;
    }

    public static double tps(double mspt) {
        return Math.min(20.0D, 1000.0D / Math.max(mspt, 0.001D));
    }
}
