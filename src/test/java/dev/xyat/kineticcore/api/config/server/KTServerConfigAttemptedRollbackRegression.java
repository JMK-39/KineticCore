package dev.xyat.kineticcore.api.config.server;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tests that a failed transaction restores only fields whose writers were attempted. */
public final class KTServerConfigAttemptedRollbackRegression {
    public static void main(String[] args) throws Throwable {
        applyDoesNotWriteUnattemptedField();
        applyAndSaveDoesNotWriteUnattemptedField();
        applyAndSaveRestoresAllAttemptedFields();
        System.out.println("PASS: 3 attempted-write rollback regression cases");
    }

    private static void applyDoesNotWriteUnattemptedField() {
        int[] first = { 1 };
        int[] second = { 2 };
        int[] secondWrites = { 0 };
        IllegalStateException failure = new IllegalStateException("first writer failed after modifying");
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:apply")
                .intValue("first", () -> first[0], value -> {
                    first[0] = value;
                    if (value == 11) throw failure;
                }, 0, 100)
                .intValue("second", () -> second[0], value -> {
                    secondWrites[0]++;
                    second[0] = value;
                }, 0, 100)
                .build();
        try {
            spec.apply(changes("first", 11, "second", 22));
            throw new AssertionError("expected failing first writer");
        } catch (IllegalStateException received) {
            check(received == failure, "original apply failure preserved");
        }
        check(first[0] == 1 && second[0] == 2, "in-memory values restored");
        check(secondWrites[0] == 0, "unattempted second field must not be written by rollback");
    }

    private static void applyAndSaveDoesNotWriteUnattemptedField() {
        int[] first = { 1 };
        int[] second = { 2 };
        int[] secondWrites = { 0 };
        int[] diskSaves = { 0 };
        IllegalStateException failure = new IllegalStateException("first writer failed after modifying");
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:save")
                .intValue("first", () -> first[0], value -> {
                    first[0] = value;
                    if (value == 11) throw failure;
                }, 0, 100)
                .intValue("second", () -> second[0], value -> {
                    secondWrites[0]++;
                    second[0] = value;
                }, 0, 100)
                .onSave(() -> diskSaves[0]++)
                .build();
        try {
            spec.applyAndSave(null, changes("first", 11, "second", 22));
            throw new AssertionError("expected failing first writer");
        } catch (Throwable received) {
            check(received == failure, "original save failure preserved");
        }
        check(first[0] == 1 && second[0] == 2, "values restored after save failure");
        check(secondWrites[0] == 0, "unattempted second field must not be written by save rollback");
        check(diskSaves[0] == 1, "restored state is persisted once");
    }

    private static void applyAndSaveRestoresAllAttemptedFields() {
        int[] first = { 1 };
        int[] second = { 2 };
        int[] third = { 3 };
        int[] thirdWrites = { 0 };
        IllegalStateException failure = new IllegalStateException("second writer failed after modifying");
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:two")
                .intValue("first", () -> first[0], value -> first[0] = value, 0, 100)
                .intValue("second", () -> second[0], value -> {
                    second[0] = value;
                    if (value == 22) throw failure;
                }, 0, 100)
                .intValue("third", () -> third[0], value -> {
                    thirdWrites[0]++;
                    third[0] = value;
                }, 0, 100)
                .build();
        Map<String, Object> values = changes("first", 11, "second", 22);
        values.put("third", 33);
        try {
            spec.applyAndSave(null, values);
            throw new AssertionError("expected failing second writer");
        } catch (Throwable received) {
            check(received == failure, "original second writer failure preserved");
        }
        check(first[0] == 1 && second[0] == 2 && third[0] == 3, "all attempted fields must be restored");
        check(thirdWrites[0] == 0, "third writer must never run if second writer fails");
    }

    private static Map<String, Object> changes(String firstId, Object firstValue, String secondId, Object secondValue) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(firstId, firstValue);
        values.put(secondId, secondValue);
        return values;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
