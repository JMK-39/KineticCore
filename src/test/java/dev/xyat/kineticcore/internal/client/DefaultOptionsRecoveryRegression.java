package dev.xyat.kineticcore.internal.client;

import java.nio.file.Files;
import java.nio.file.Path;

/** Verifies default client options are copied only for a missing or damaged options file. */
public final class DefaultOptionsRecoveryRegression {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("kineticcore-options-recovery");
        try {
            Path defaults = directory.resolve("defaultoptions.txt");
            Path options = directory.resolve("options.txt");
            byte[] defaultsBytes = new byte[4096];
            for (int i = 0; i < defaultsBytes.length; i++) defaultsBytes[i] = (byte) (i % 127);
            Files.write(defaults, defaultsBytes);

            check(DefaultOptionsRecovery.restore(defaults, options)
                            == DefaultOptionsRecovery.Result.RESTORED_MISSING,
                    "a missing options file is restored from defaults");
            check(java.util.Arrays.equals(Files.readAllBytes(options), defaultsBytes),
                    "restored options match the configured defaults");

            Files.write(options, new byte[] { 1, 2, 3 });
            check(DefaultOptionsRecovery.restore(defaults, options)
                            == DefaultOptionsRecovery.Result.RESTORED_DAMAGED,
                    "a damaged short options file is restored from defaults");
            check(java.util.Arrays.equals(Files.readAllBytes(options), defaultsBytes),
                    "damaged options are replaced completely");

            byte[] playerOptions = new byte[4096];
            java.util.Arrays.fill(playerOptions, (byte) 42);
            Files.write(options, playerOptions);
            check(DefaultOptionsRecovery.restore(defaults, options)
                            == DefaultOptionsRecovery.Result.PRESERVED,
                    "a valid options file is preserved");
            check(java.util.Arrays.equals(Files.readAllBytes(options), playerOptions),
                    "valid player options are not overwritten");

            Path missingDefaults = directory.resolve("missing-defaults.txt");
            Files.delete(options);
            check(DefaultOptionsRecovery.restore(missingDefaults, options)
                            == DefaultOptionsRecovery.Result.DEFAULTS_MISSING,
                    "missing defaults are reported without creating options");
            check(!Files.exists(options), "no options file is created when defaults are missing");

            System.out.println("PASS: " + checks + " default options recovery checks");
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }
}
