package dev.xyat.kineticcore.api.monitoring;

/** TPS should never display healthy performance for an invalid MSPT sample. */
public final class ServerTickTrackerInvalidMsptRegression {
    private static int checks;
    public static void main(String[] args) {
        check(ServerTickTracker.tps(Double.NEGATIVE_INFINITY) == 0D, "negative infinity");
        check(ServerTickTracker.tps(-100D) == 20D, "finite negative MSPT retains capped behavior");
        check(ServerTickTracker.tps(Double.POSITIVE_INFINITY) == 0D, "positive infinity");
        check(ServerTickTracker.tps(Double.NaN) == 0D, "NaN");
        check(ServerTickTracker.tps(50D) == 20D, "normal 50ms tick");
        check(ServerTickTracker.tps(100D) == 10D, "normal 100ms tick");
        check(ServerTickTracker.tps(0D) == 20D, "zero MSPT retains existing behavior");
        System.out.println("PASS: " + checks + " invalid MSPT checks");
    }
    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }
}
