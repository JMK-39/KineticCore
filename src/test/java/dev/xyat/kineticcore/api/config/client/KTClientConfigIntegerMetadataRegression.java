package dev.xyat.kineticcore.api.config.client;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Number validators on public metadata must not accept truncated or fractional numbers. */
public final class KTClientConfigIntegerMetadataRegression {
    private static int cases;
    private static void check(boolean condition, String message) {
        cases++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
        builder.defineInt("amount", 0, 0, 10);
        builder.defineLong("duration", 0, 0, 10);
        builder.defineDouble("ratio", 0.5, 0.0, 1.0);
        KTClientConfigSpec spec = builder.build();
        KTClientConfigSpec.EntryDefinition integer = (KTClientConfigSpec.EntryDefinition) spec.operations().get(0);
        KTClientConfigSpec.EntryDefinition longEntry = (KTClientConfigSpec.EntryDefinition) spec.operations().get(1);
        KTClientConfigSpec.EntryDefinition decimal = (KTClientConfigSpec.EntryDefinition) spec.operations().get(2);
        check(!integer.validator().test(4_294_967_296L), "int validator cannot wrap a long to zero");
        check(!integer.validator().test(new BigDecimal("1.5")), "int validator cannot truncate fractions");
        check(!longEntry.validator().test(new BigInteger("18446744073709551616")), "long validator cannot wrap BigInteger");
        check(!longEntry.validator().test(new BigDecimal("1.5")), "long validator cannot truncate fractions");
        check(integer.validator().test(5) && longEntry.validator().test(5L), "valid integer metadata must remain accepted");
        check(!integer.validator().test(Double.NaN) && !longEntry.validator().test(Double.POSITIVE_INFINITY),
                "non-finite numeric metadata must be rejected");
        check(!decimal.validator().test(new BigDecimal("1.000000000000000000000000001")),
                "decimal validator cannot round an out-of-range BigDecimal down to max");
        check(!decimal.validator().test(new BigDecimal("-0.000000000000000000000000001")),
                "decimal validator cannot round an out-of-range BigDecimal up to min");
        check(decimal.validator().test(0.5) && !decimal.validator().test(Double.NaN),
                "finite in-range double metadata must still be accepted");
        System.out.println("PASS: " + cases + " integer config metadata regression cases");
    }
}
