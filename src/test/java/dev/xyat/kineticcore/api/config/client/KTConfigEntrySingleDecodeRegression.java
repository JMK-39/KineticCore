package dev.xyat.kineticcore.api.config.client;

import net.minecraft.network.chat.Component;
import java.util.concurrent.atomic.AtomicInteger;

/** A snapshot must be decoded once and rejected before invoking its writer. */
public final class KTConfigEntrySingleDecodeRegression {
    public static void main(String[] args) {
        AtomicInteger decodes = new AtomicInteger();
        AtomicInteger validations = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        KTConfigEntry<Integer> entry = KTConfigEntry.value(
                "number", KTConfigEntry.Type.INTEGER, Component.empty(), null,
                () -> 0, ignored -> writes.incrementAndGet(), 0, 0, 10, null,
                raw -> { decodes.incrementAndGet(); return raw instanceof Integer value ? value : null; },
                value -> value, value -> { validations.incrementAndGet(); return true; });
        decodes.set(0);
        validations.set(0);
        entry.writeSnapshot(5);
        check(decodes.get() == 1, "writeSnapshot decoded the same value more than once");
        check(validations.get() == 1, "writeSnapshot must validate precisely once");
        check(writes.get() == 1, "writer should run exactly once");
        try { entry.writeSnapshot(12); throw new AssertionError("invalid value accepted"); }
        catch (IllegalArgumentException expected) { }
        check(writes.get() == 1, "invalid snapshot reached writer");
        System.out.println("PASS: 4 config-entry snapshot checks");
    }
    private static void check(boolean yes, String message) { if (!yes) throw new AssertionError(message); }
}
