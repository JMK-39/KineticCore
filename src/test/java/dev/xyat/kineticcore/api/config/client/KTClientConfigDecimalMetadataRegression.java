package dev.xyat.kineticcore.api.config.client;

import java.math.BigDecimal;

/** Regression: Forge-side metadata validates the original decimal, not its rounded double. */
public final class KTClientConfigDecimalMetadataRegression {
    private static int cases;

    public static void main(String[] args) {
        // Build through a separate builder so that entry metadata is observable.
        KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
        builder.defineDouble("amount", 0.5, 0, 1);
        KTClientConfigSpec.EntryDefinition entry =
                (KTClientConfigSpec.EntryDefinition) builder.build().operations().get(0);
        check(!entry.validator().test(new BigDecimal("1.0000000000000001")),
                "fraction beyond double maximum is invalid");
        check(!entry.validator().test(new BigDecimal("1e-1000")),
                "nonzero decimal cannot be accepted after underflow");
        check(entry.validator().test(new BigDecimal("0.5")),
                "representable in-range decimal remains valid");
        System.out.println("PASS: " + cases + " client decimal metadata cases");
    }

    private static void check(boolean ok, String reason) {
        cases++;
        if (!ok) throw new AssertionError(reason);
    }
}
