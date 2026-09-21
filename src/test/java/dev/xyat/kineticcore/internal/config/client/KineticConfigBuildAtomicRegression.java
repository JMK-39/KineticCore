package dev.xyat.kineticcore.internal.config.client;

import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** A failed native config build must not publish provisional value bindings. */
public final class KineticConfigBuildAtomicRegression {
    private static int cases;

    private static void check(boolean condition, String description) {
        cases++;
        if (!condition) throw new AssertionError(description);
    }

    public static void main(String[] args) throws IOException {
        KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
        KTClientConfigSpec.IntValue value = builder.defineInt("itemLimit", 5, 0, 100);
        builder.build();
        value.set(6);
        check(value.get() == 6, "unbound local fallback remains writable before native binding");

        String source = Files.readString(Path.of(
                "src/main/java/dev/xyat/kineticcore/internal/config/client/KineticClientConfigSpecRuntime.java"));
        int pending = source.indexOf("pendingValues.put(entry.value(), nativeValue)");
        int built = source.indexOf("ForgeConfigSpec built = builder.build()");
        int publish = source.indexOf("VALUES.putAll(pendingValues)");
        check(pending >= 0, "native values must stage into pending bindings");
        check(built > pending, "Forge specification must finish building after all provisional bindings are staged");
        check(publish > built, "native bindings must only publish after the whole Forge specification builds successfully");
        System.out.println("PASS: " + cases + " atomic native config build regression cases");
    }
}
