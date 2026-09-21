package dev.xyat.kineticcore.api.config.server;

import java.util.LinkedHashMap;
import java.util.Map;

/** Roll back dependent writers in the opposite order of the attempted writes. */
public final class KTServerConfigDependencyRollbackRegression {
    private static int checks;

    public static void main(String[] args) throws Throwable {
        checkApply();
        checkApplyAndSave();
        System.out.println("PASS: " + checks + " dependent config rollback checks");
    }

    private static void checkApply() {
        Fixture fixture = new Fixture();
        try {
            fixture.spec.apply(fixture.changes());
            throw new AssertionError("expected failure from dependent second writer");
        } catch (IllegalStateException failure) {
            check(failure == fixture.originalFailure, "apply preserves original write failure");
        }
        check(fixture.first[0] == 1 && fixture.second[0] == 2,
                "apply restores all attempted writers in dependency-safe order");
        check(fixture.restoreLog.toString().equals("BA"), "apply rollback must run B before A");
    }

    private static void checkApplyAndSave() throws Throwable {
        Fixture fixture = new Fixture();
        try {
            fixture.spec.applyAndSave(null, fixture.changes());
            throw new AssertionError("expected failure from dependent second writer");
        } catch (IllegalStateException failure) {
            check(failure == fixture.originalFailure, "applyAndSave preserves original write failure");
        }
        check(fixture.first[0] == 1 && fixture.second[0] == 2,
                "applyAndSave restores all attempted writers in dependency-safe order");
        check(fixture.restoreLog.toString().equals("BA"), "saved rollback must run B before A");
        check(fixture.diskSaves[0] == 1, "successful restoration is saved exactly once");
    }

    private static final class Fixture {
        final int[] first = {1};
        final int[] second = {2};
        final int[] diskSaves = {0};
        final StringBuilder restoreLog = new StringBuilder();
        final IllegalStateException originalFailure = new IllegalStateException("B fails after mutation");
        final KTServerConfigSpec spec = KTServerConfigSpec.builder("test:dependency")
                .intValue("A", () -> first[0], v -> {
                    if (v == 1) restoreLog.append('A');
                    first[0] = v;
                }, 0, 100)
                .intValue("B", () -> second[0], v -> {
                    if (v == 2) {
                        // Restoring B depends on A still having its newly written value.
                        if (first[0] != 11) throw new IllegalStateException("A restored too early");
                        restoreLog.append('B');
                    }
                    second[0] = v;
                    if (v == 22) throw originalFailure;
                }, 0, 100)
                .onSave(() -> diskSaves[0]++)
                .build();
        Map<String, Object> changes() {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("A", 11);
            changes.put("B", 22);
            return changes;
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
}
