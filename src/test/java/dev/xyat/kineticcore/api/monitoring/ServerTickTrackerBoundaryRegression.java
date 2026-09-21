package dev.xyat.kineticcore.api.monitoring;

/** Headless regression checks for tick duration boundaries and sampling-window overflow. */
public final class ServerTickTrackerBoundaryRegression {
    private static int checks;
    private static void check(boolean okay, String message) {
        checks++;
        if (!okay) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ServerTickTracker tracker = new ServerTickTracker();
        tracker.addTick(50_000_000L);
        tracker.addTick(100_000_000L);
        check(tracker.getStats(Integer.MAX_VALUE, 0) == 75.0D, "large seconds must not overflow to zero");
        check(tracker.getStats(Integer.MAX_VALUE, 1) == 100.0D, "large seconds must keep peak sample");
        check(tracker.getStats(-1, 0) == 0.0D, "negative interval has no sample");
        check(tracker.getLatestMspt() == 100.0D, "latest sample remains correct");
        check(Double.isFinite(ServerTickTracker.tps(Double.NaN)), "NaN MSPT must not produce NaN TPS");
        check(ServerTickTracker.tps(Double.POSITIVE_INFINITY) == 0.0D, "infinite MSPT is zero TPS");
        check(ServerTickTracker.tps(-1.0D) == 20.0D, "negative MSPT must not exceed capped TPS");
        try {
            tracker.addTick(-1L);
            throw new AssertionError("negative tick duration accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
        check(tracker.getLatestMspt() == 100.0D, "rejected sample must not mutate recent sample");
        System.out.println("PASS: " + checks + " tick tracking boundary checks");
    }
}
