package dev.xyat.kineticcore.feature.firstjoin.event;

public final class FirstJoinWorldEligibilityRegression {
    public static void main(String[] args) {
        check(FirstJoinWorldEligibility.isNewWorld(0, 0, false), "fresh world");
        check(FirstJoinWorldEligibility.isNewWorld(100, 60_000, false), "recent world");
        check(!FirstJoinWorldEligibility.isNewWorld(0, 0, true), "existing player data");
        check(!FirstJoinWorldEligibility.isNewWorld(12_001, 0, false), "played world");
        check(!FirstJoinWorldEligibility.isNewWorld(0, 600_001, false), "aged world");
        check(!FirstJoinWorldEligibility.isNewWorld(0, -1, false), "unknown age");
        check(FirstJoinWorldEligibility.isStillNew(1_000, 200_000, 600_000), "within first-run window");
        check(!FirstJoinWorldEligibility.isStillNew(1_000, 600_001, 600_000), "expired first-run window");
        check(!FirstJoinWorldEligibility.isStillNew(12_001, 200_000, 600_000), "played beyond first-run window");
    }

    private static void check(boolean value, String caseName) {
        if (!value) throw new AssertionError(caseName);
    }
}
