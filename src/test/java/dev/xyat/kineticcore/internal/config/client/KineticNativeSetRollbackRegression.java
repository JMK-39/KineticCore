package dev.xyat.kineticcore.internal.config.client;

import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Native config writes must preserve the previous value when Forge reports a late setter failure. */
public final class KineticNativeSetRollbackRegression {
    private static int checks;

    private static void check(boolean result, String description) {
        checks++;
        if (!result) throw new AssertionError(description);
    }

    public static void main(String[] args) throws IOException {
        var builder = KTClientConfigSpec.builder();
        var value = builder.defineInt("limit", 10, 0, 100);
        builder.build();
        value.set(20);
        check(value.get() == 20, "unbound local fallback remains functional");

        String source = Files.readString(Path.of(
                "src/main/java/dev/xyat/kineticcore/internal/config/client/KineticClientConfigSpecRuntime.java"));
        int previous = source.indexOf("Object previous = nativeValue.get()");
        int write = source.indexOf("nativeValue.set(next)", previous);
        int rollback = source.indexOf("nativeValue.set(previous)", write);
        int rethrow = source.indexOf("throw failure", rollback);
        check(previous >= 0, "native setter must capture the previous value");
        check(write > previous, "native setter must attempt the requested value after capturing the previous value");
        check(rollback > write, "late native failure must attempt to restore the previous value");
        check(rethrow > rollback, "original native failure must propagate after rollback attempt");
        System.out.println("PASS: " + checks + " native setter rollback checks");
    }
}
