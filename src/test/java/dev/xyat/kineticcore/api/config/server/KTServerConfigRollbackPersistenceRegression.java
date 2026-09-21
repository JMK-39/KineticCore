package dev.xyat.kineticcore.api.config.server;

import java.util.Map;

/** Rollback callbacks must only run after the restored state has been persisted. */
public final class KTServerConfigRollbackPersistenceRegression {
    public static void main(String[] args) throws Throwable {
        failedRollbackPersistenceSkipsApplyCallback();
        successfulRollbackPersistenceRunsApplyCallback();
        System.out.println("PASS: 2 rollback persistence regression cases");
    }

    private static void failedRollbackPersistenceSkipsApplyCallback() throws Throwable {
        int[] value = { 10 };
        int[] saveCalls = { 0 };
        int[] applyCalls = { 0 };
        IllegalStateException writerFailure = new IllegalStateException("writer failed after modifying");
        IllegalStateException persistenceFailure = new IllegalStateException("disk write failed");
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:rollback_disk_failure")
                .intValue("value", () -> value[0], updated -> {
                    value[0] = updated;
                    if (updated == 20) throw writerFailure;
                }, 0, 100)
                .onSave(() -> {
                    saveCalls[0]++;
                    throw persistenceFailure;
                })
                .afterSave(ignored -> applyCalls[0]++)
                .build();
        try {
            spec.applyAndSave(null, Map.of("value", 20));
            throw new AssertionError("writer must fail");
        } catch (Throwable received) {
            check(received == writerFailure, "original writer error must be preserved");
            check(received.getSuppressed().length == 1 && received.getSuppressed()[0] == persistenceFailure,
                    "rollback persistence failure must be attached to original error");
        }
        check(value[0] == 10, "memory should still be restored");
        check(saveCalls[0] == 1, "rollback persistence should have been attempted");
        check(applyCalls[0] == 0, "must not announce successful rollback when disk persistence fails");
    }

    private static void successfulRollbackPersistenceRunsApplyCallback() throws Throwable {
        int[] value = { 10 };
        int[] saveCalls = { 0 };
        int[] applyCalls = { 0 };
        IllegalStateException writerFailure = new IllegalStateException("writer failed after modifying");
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:rollback_disk_success")
                .intValue("value", () -> value[0], updated -> {
                    value[0] = updated;
                    if (updated == 20) throw writerFailure;
                }, 0, 100)
                .onSave(() -> saveCalls[0]++)
                .afterSave(ignored -> applyCalls[0]++)
                .build();
        try {
            spec.applyAndSave(null, Map.of("value", 20));
            throw new AssertionError("writer must fail");
        } catch (Throwable received) {
            check(received == writerFailure, "original writer error must be preserved");
        }
        check(value[0] == 10 && saveCalls[0] == 1 && applyCalls[0] == 1,
                "successful rollback must persist and then apply restored state once");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
