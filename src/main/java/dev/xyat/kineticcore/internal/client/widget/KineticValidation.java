package dev.xyat.kineticcore.internal.client.widget;

/** Internal guard for add-on supplied control validators; a broken validator rejects input. */
public final class KineticValidation {
    private KineticValidation() {}

    public static <T> boolean accepts(java.util.function.Predicate<? super T> validator, T value) {
        try {
            return validator == null || validator.test(value);
        } catch (RuntimeException invalid) {
            // A bad add-on validator must not crash a shared widget or accept the value.
            return false;
        }
    }
}
