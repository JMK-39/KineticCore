package dev.xyat.kineticcore.api.config.server;

import java.util.Map;

/** Headless regression for partial server-config transactions. Run main with Java 17. */
public final class KTServerConfigPartialSaveRegression {
    public static void main(String[] args) throws Throwable {
        partialSaveIgnoresUnrelatedReader();
        failedPartialSaveDoesNotRewriteUnrelatedEntry();
        rejectedUpdateDoesNotReadUnrelatedEntries();
        System.out.println("PASS: 3 partial server-config save regression cases");
    }

    private static void partialSaveIgnoresUnrelatedReader() throws Throwable {
        int[] changed = { 10 };
        int[] unrelatedReads = { 0 };
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:partial")
                .intValue("changed", () -> changed[0], value -> changed[0] = value, 0, 100)
                .intValue("unrelated", () -> {
                    unrelatedReads[0]++;
                    throw new IllegalStateException("unrelated reader is unavailable");
                }, ignored -> { }, 0, 100)
                .build();
        spec.applyAndSave(null, Map.of("changed", 20));
        check(changed[0] == 20 && unrelatedReads[0] == 0,
                "partial save must not read unrelated entries");
    }

    private static void failedPartialSaveDoesNotRewriteUnrelatedEntry() {
        int[] changed = { 10 };
        int[] unrelated = { 40 };
        int[] unrelatedWrites = { 0 };
        int[] saves = { 0 };
        IllegalStateException expected = new IllegalStateException("write fails");
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:rollback")
                .intValue("changed", () -> changed[0], value -> {
                    changed[0] = value;
                    if (value == 20) throw expected;
                }, 0, 100)
                .intValue("unrelated", () -> unrelated[0], value -> {
                    unrelatedWrites[0]++;
                    unrelated[0] = value;
                }, 0, 100)
                .onSave(() -> saves[0]++)
                .build();
        try {
            spec.applyAndSave(null, Map.of("changed", 20));
            throw new AssertionError("partial save must fail");
        } catch (Throwable failure) {
            check(failure == expected, "must propagate original failure");
        }
        check(changed[0] == 10 && unrelated[0] == 40, "submitted value must roll back");
        check(unrelatedWrites[0] == 0, "rollback must not write untouched entries");
        check(saves[0] == 1, "successful rollback must persist restored values once");
    }

    private static void rejectedUpdateDoesNotReadUnrelatedEntries() {
        int[] reads = { 0 };
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:invalid")
                .intValue("entry", () -> {
                    reads[0]++;
                    return 10;
                }, ignored -> { }, 0, 100)
                .build();
        try {
            spec.applyAndSave(null, Map.of("entry", 1000));
            throw new AssertionError("invalid value must fail validation");
        } catch (IllegalArgumentException expected) {
            // Validation should precede any snapshot reads.
        } catch (Throwable unexpected) {
            throw new AssertionError("unexpected exception", unexpected);
        }
        check(reads[0] == 0, "invalid update must be rejected before reading config");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
