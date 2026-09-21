package dev.xyat.kineticcore.api.config.client;

import net.minecraft.network.chat.Component;
import java.util.concurrent.atomic.AtomicInteger;

/** Public writes must use the same validation rules as snapshot writes. */
public final class KTConfigEntryDirectWriteRegression {
    public static void main(String[] args) {
        AtomicInteger written = new AtomicInteger(-1);
        AtomicInteger checks = new AtomicInteger();
        KTConfigEntry<Integer> entry = KTConfigEntry.value(
                "count", KTConfigEntry.Type.INTEGER, Component.empty(), null,
                written::get, written::set, 1, 0, 10, null,
                value -> value instanceof Integer integer ? integer : null,
                value -> value, value -> { checks.incrementAndGet(); return value % 2 == 0; });
        reject(() -> entry.write(12));
        check(written.get() == -1, "public write bypassed numeric bounds");
        reject(() -> entry.write(3));
        check(written.get() == -1, "public write bypassed business validator");
        reject(() -> entry.write(null));
        check(written.get() == -1, "public write forwarded null");
        checks.set(0);
        entry.write(4);
        check(written.get() == 4 && checks.get() == 1, "valid write must validate once and persist once");
        checks.set(0);
        entry.writeSnapshot(6);
        check(written.get() == 6 && checks.get() == 1, "snapshot write must not run duplicate validation");
        System.out.println("PASS: 5 public config-entry write checks");
    }
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid public write was accepted");
    }
    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
}
