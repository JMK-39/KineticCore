package dev.xyat.kineticcore.api.config.client;

/** Headless regression: validated doubles must remain finite even if a custom validator accepts all values. */
public final class KTClientConfigFiniteRegression {
    public static void main(String[] args) {
        reject(() -> KTClientConfigSpec.builder().defineDoubleValidated("amount", Double.NaN, raw -> true));
        reject(() -> KTClientConfigSpec.builder().defineDoubleValidated("amount", Double.POSITIVE_INFINITY, raw -> true));
        KTClientConfigSpec.DoubleValue valid = KTClientConfigSpec.builder()
                .defineDoubleValidated("amount", 12.5, raw -> true);
        reject(() -> valid.set(Double.NaN));
        reject(() -> valid.set(Double.NEGATIVE_INFINITY));
        valid.set(1.25);
        if (valid.get() != 1.25) throw new AssertionError("finite values must remain writable");
        System.out.println("PASS: 5 validated-double regression cases");
    }

    private static void reject(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("non-finite double was accepted");
        } catch (IllegalArgumentException expected) { }
    }
}
