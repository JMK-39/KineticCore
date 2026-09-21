package dev.xyat.kineticcore.internal.client.config;

import java.nio.file.Files;
import java.nio.file.Path;

/** Initialization is all-or-nothing; a failed second listener must not strand the first. */
public final class ServerConfigSubscriptionRegression {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/xyat/kineticcore/internal/client/config/ServerConfigClientRuntime.java"));
        int start = source.indexOf("public static synchronized void initialize()");
        int end = source.indexOf("private record State(", start);
        require(start >= 0 && end > start, "synchronized initializer missing");
        String method = source.substring(start, end);
        require(method.contains("var loginSubscription = KineticClientEventRuntime.registerLogin"), "login subscription handle not retained");
        require(method.contains("KineticClientEventRuntime.registerLogout"), "logout listener missing");
        require(method.indexOf("initialized = true;") > method.indexOf("registerLogout"), "initialization marked complete before both listeners registered");
        require(method.contains("loginSubscription.close();"), "partial registration not cleaned up");
        require(method.contains("if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure)"), "cleanup failure masks original error or self-suppresses");
        System.out.println("PASS: 5 config subscription contracts");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
