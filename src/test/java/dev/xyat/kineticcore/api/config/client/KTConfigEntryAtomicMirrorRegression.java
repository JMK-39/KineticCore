package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Checks the real client-sync mirror path against current page-builder semantics. */
public final class KTConfigEntryAtomicMirrorRegression {
    private static int checks;

    public static void main(String[] args) throws Exception {
        AtomicReference<Integer> value = new AtomicReference<>(4);
        AtomicInteger validations = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        KTConfigPage page = page(value, validations, writes, false);
        apply(page, Map.of("value", 6));
        check(value.get() == 6, "valid mirror value not written");
        check(validations.get() == 1, "mirror validated same value more than once");
        check(writes.get() == 1, "mirror wrote same value more than once");

        validations.set(0);
        writes.set(0);
        apply(page, Map.of("value", 13));
        check(value.get() == 6, "invalid mirror value changed local data");
        check(writes.get() == 0, "invalid mirror value invoked writer");
        check(validations.get() == 1, "invalid mirror value validated multiple times");

        AtomicReference<Integer> fails = new AtomicReference<>(4);
        AtomicInteger failureValidations = new AtomicInteger();
        AtomicInteger failureWrites = new AtomicInteger();
        KTConfigPage failingPage = page(fails, failureValidations, failureWrites, true);
        apply(failingPage, Map.of("value", 9));
        check(fails.get() == 4, "writer that mutated before throwing must be rolled back");
        check(failureValidations.get() == 1, "failure path must only validate incoming value once");
        check(failureWrites.get() == 2, "rollback must write previous value exactly once");

        AtomicReference<Integer> sameValue = new AtomicReference<>(4);
        AtomicInteger attempts = new AtomicInteger();
        IllegalStateException sharedFailure = new IllegalStateException("same failure from write and rollback");
        KTConfigEntry<Integer> sameErrorEntry = KTConfigEntry.value(
                "value", KTConfigEntry.Type.INTEGER, Component.empty(), null,
                sameValue::get, next -> {
                    attempts.incrementAndGet();
                    sameValue.set(next);
                    throw sharedFailure;
                }, 4, 0, 20, null,
                raw -> raw instanceof Integer integer ? integer : null,
                integer -> integer, integer -> true);
        try {
            sameErrorEntry.applySnapshotWithRollback(8);
            throw new AssertionError("writer failure was swallowed");
        } catch (IllegalStateException actual) {
            check(actual == sharedFailure, "original failure must be preserved");
            check(actual.getSuppressed().length == 0, "exception cannot suppress itself");
        }
        check(attempts.get() == 2, "rollback must still attempt to restore field");
        check(sameValue.get() == 4, "failed rollback must restore data when writer mutates first");

        System.out.println("PASS: " + checks + " atomic mirror regression assertions");
    }

    private static KTConfigPage page(
            AtomicReference<Integer> value,
            AtomicInteger validations,
            AtomicInteger writes,
            boolean failOnce
    ) {
        return KTConfigPage.builder("test:atomic_mirror", Component.empty())
                .intValueValidated(
                        "value",
                        Component.empty(),
                        value::get,
                        next -> {
                            int index = writes.incrementAndGet();
                            value.set(next);
                            if (failOnce && index == 1) {
                                throw new IllegalStateException("write failed after mutation");
                            }
                        },
                        4,
                        0,
                        20,
                        integer -> {
                            validations.incrementAndGet();
                            return integer >= 0 && integer != 13;
                        },
                        null
                )
                .build();
    }

    private static void apply(KTConfigPage page, Map<String, Object> values) throws Exception {
        Method apply = ServerConfigClientRuntime.class.getDeclaredMethod(
                "applyToClientMirror", KTConfigPage.class, Map.class);
        apply.setAccessible(true);
        apply.invoke(null, page, values);
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
