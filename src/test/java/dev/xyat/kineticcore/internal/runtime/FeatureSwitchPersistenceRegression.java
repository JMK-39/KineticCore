package dev.xyat.kineticcore.internal.runtime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs one case per JVM to leave static startup feature state isolated. */
public final class FeatureSwitchPersistenceRegression {
    private static final Path CONFIG = Path.of("config", "kineticcore", "startup_features.toml");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("case");
        switch (args[0]) {
            case "invalid" -> invalidBooleanMustKeepDefault();
            case "saveFailure" -> failedRegistrationMustRollbackInMemory();
            default -> throw new IllegalArgumentException(args[0]);
        }
        System.out.println("PASS: " + args[0]);
    }

    private static void invalidBooleanMustKeepDefault() throws Exception {
        Files.createDirectories(CONFIG.getParent());
        Files.writeString(CONFIG, "movement.flight_server = typo\n"
                + "movement.flight_client = FALSE\n"
                + "player.crawling = true # user comment\n", StandardCharsets.UTF_8);
        FeatureSwitchRuntime.initialize();
        check(FeatureSwitchRuntime.isEnabled("movement.flight_server"),
                "malformed boolean must not disable a default-enabled startup feature");
        check(!FeatureSwitchRuntime.isEnabled("movement.flight_client"),
                "valid FALSE must still disable a feature");
        check(FeatureSwitchRuntime.isEnabled("player.crawling"),
                "a valid true with a trailing TOML comment must remain enabled");
    }

    private static void failedRegistrationMustRollbackInMemory() throws Exception {
        FeatureSwitchRuntime.initialize();
        Files.delete(CONFIG);
        Files.createDirectories(CONFIG);
        Files.writeString(CONFIG.resolve("blocker.txt"), "prevent file replacement");
        boolean rejected = false;
        try {
            FeatureSwitchRuntime.register("custom.failure", "custom", false,
                    "custom.section", "custom.old", "custom.tooltip");
        } catch (java.io.UncheckedIOException failure) {
            rejected = true;
        }
        check(rejected, "save to a nonempty directory should fail");
        check(FeatureSwitchRuntime.descriptors().stream().noneMatch(
                        entry -> "custom.failure".equals(entry.id())),
                "a failed registration must not remain registered in memory");
        Files.delete(CONFIG.resolve("blocker.txt"));
        Files.delete(CONFIG);
        FeatureSwitchRuntime.register("custom.failure", "custom", true,
                "custom.section", "custom.new", "custom.tooltip");
        check(FeatureSwitchRuntime.isEnabled("custom.failure"),
                "a successful retry must use its own default, not the failed registration's value");
        check(Files.readString(CONFIG).contains("custom.failure = true"),
                "the successful retry must actually be persisted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
