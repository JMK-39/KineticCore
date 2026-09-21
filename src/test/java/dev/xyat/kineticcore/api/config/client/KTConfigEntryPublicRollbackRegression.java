package dev.xyat.kineticcore.api.config.client;

import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Public config writes must restore an in-flight field that mutates before throwing. */
public final class KTConfigEntryPublicRollbackRegression {
    private static int checks;

    public static void main(String[] args) {
        checkWriteRollback(false);
        checkWriteRollback(true);
        checkSameFailure(false);
        checkSameFailure(true);
        checkDifferentRollbackFailure();
        checkValidationBeforeReading();
        System.out.println("PASS: " + checks + " public entry rollback checks");
    }

    private static KTConfigEntry<Integer> entry(AtomicReference<Integer> value,
                                                 java.util.function.Consumer<Integer> writer,
                                                 AtomicInteger reads) {
        return KTConfigEntry.value("test", KTConfigEntry.Type.INTEGER, Component.empty(), null,
                () -> { reads.incrementAndGet(); return value.get(); }, writer,
                4, 0, 10, null,
                raw -> raw instanceof Integer integer ? integer : null,
                number -> number, number -> true);
    }

    private static void checkWriteRollback(boolean snapshot) {
        AtomicReference<Integer> state = new AtomicReference<>(4);
        AtomicInteger calls = new AtomicInteger();
        KTConfigEntry<Integer> field = entry(state, next -> {
            state.set(next);
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("first write failed");
        }, new AtomicInteger());
        try {
            write(field, 8, snapshot);
            throw new AssertionError("writer failure was swallowed");
        } catch (IllegalStateException expected) {
            check("first write failed".equals(expected.getMessage()), "original failure not propagated");
        }
        check(state.get() == 4, "partially written value not restored: " + snapshot);
        check(calls.get() == 2, "must attempt exactly one rollback: " + snapshot);
    }

    private static void checkSameFailure(boolean snapshot) {
        AtomicReference<Integer> state = new AtomicReference<>(4);
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException shared = new IllegalStateException("shared");
        KTConfigEntry<Integer> field = entry(state, next -> {
            state.set(next);
            calls.incrementAndGet();
            throw shared;
        }, new AtomicInteger());
        try {
            write(field, 9, snapshot);
            throw new AssertionError("writer failure was swallowed");
        } catch (IllegalStateException actual) {
            check(actual == shared, "same original exception instance must survive: " + snapshot);
            check(actual.getSuppressed().length == 0, "exception must not suppress itself");
        }
        check(state.get() == 4 && calls.get() == 2, "same-error rollback must restore previous value");
    }

    private static void checkDifferentRollbackFailure() {
        AtomicReference<Integer> state = new AtomicReference<>(4);
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException original = new IllegalStateException("original");
        IllegalStateException rollback = new IllegalStateException("rollback");
        KTConfigEntry<Integer> field = entry(state, next -> {
            state.set(next);
            throw calls.incrementAndGet() == 1 ? original : rollback;
        }, new AtomicInteger());
        try {
            field.writeSnapshot(7);
            throw new AssertionError("writer failure was swallowed");
        } catch (IllegalStateException actual) {
            check(actual == original, "rollback must not replace primary failure");
            check(actual.getSuppressed().length == 1 && actual.getSuppressed()[0] == rollback,
                    "different rollback failure must be attached");
        }
        check(state.get() == 4, "rollback still attempts to restore the field");
    }

    private static void checkValidationBeforeReading() {
        AtomicReference<Integer> state = new AtomicReference<>(4);
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        KTConfigEntry<Integer> field = entry(state, next -> writes.incrementAndGet(), reads);
        for (boolean snapshot : new boolean[]{false, true}) {
            try {
                write(field, 11, snapshot);
                throw new AssertionError("invalid input accepted");
            } catch (IllegalArgumentException expected) {
                check(reads.get() == 0 && writes.get() == 0,
                        "invalid write must not read or change existing field");
            }
        }
    }

    private static void write(KTConfigEntry<Integer> field, int value, boolean snapshot) {
        if (snapshot) field.writeSnapshot(value);
        else field.write(value);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
