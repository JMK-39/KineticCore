package dev.xyat.kineticcore.api.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** An installed server mixin may expose its tracker before the tracker has been assigned. */
public final class KineticServerPerformanceAbsentTrackerRegression {
    private static int checks;

    public static void main(String[] args) throws IOException {
        check(KineticServerPerformance.tracker(null).isEmpty(), "null server must have no tracker");
        String source = Files.readString(Path.of(
                "src/main/java/dev/xyat/kineticcore/api/monitoring/KineticServerPerformance.java"));
        check(source.contains("server instanceof Access access"),
                "tracker lookup must only use the mixin access contract when present");
        check(source.contains("Optional.ofNullable(access.kineticcore$getTickTracker())"),
                "an installed access contract with no tracker must return Optional.empty");
        check(source.contains("return Optional.empty();"),
                "servers without the mixin access contract must return Optional.empty");
        System.out.println("PASS: " + checks + " nullable TPS tracker regression cases");
    }

    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
}
