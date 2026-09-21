package dev.xyat.kineticcore.api.monitoring;

/** Guard the shared tick monitor against cumulative long overflow. */
public final class ServerTickTrackerSumOverflowRegression {
    public static void main(String[] args) {
        ServerTickTracker tracker = new ServerTickTracker();
        tracker.addTick(Long.MAX_VALUE);
        tracker.addTick(Long.MAX_VALUE);
        double average = tracker.getStats(1, 0);
        check(Double.isFinite(average) && average > 0, "two positive ticks must have a positive mean");
        check(Math.abs(average / (Long.MAX_VALUE * 1e-6D) - 1D) < 1e-12D,
                "mean must agree with each equal sample");
        for (int i = 2; i < 1200; i++) tracker.addTick(Long.MAX_VALUE);
        check(tracker.getStats(60, 0) > 0, "rolling-window total cannot overflow");
        tracker.addTick(50_000_000L);
        check(tracker.getStats(60, 0) > 0, "replacing a huge tick cannot poison sum");
        // A running double sum loses precision when enormous values leave the
        // ring buffer. The fully replaced window must recover exact normal MSPT.
        for (int i = 1; i < 1200; i++) tracker.addTick(50_000_000L);
        check(Math.abs(tracker.getStats(60, 0) - 50.0D) < 1e-9D,
                "fully replaced window retained rounding residue from huge samples");
        System.out.println("PASS: 5 tick-sum overflow cases");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
