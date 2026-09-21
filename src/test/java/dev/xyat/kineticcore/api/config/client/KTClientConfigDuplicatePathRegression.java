package dev.xyat.kineticcore.api.config.client;

/** Check duplicate client-config paths before they reach Forge's native builder. */
public final class KTClientConfigDuplicatePathRegression {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void reject(Runnable action) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("duplicate path must be rejected");
    }
    public static void main(String[] args) {
        var one = KTClientConfigSpec.builder();
        one.defineBoolean("enabled", true);
        reject(() -> one.defineInt("enabled", 1, 0, 3));
        check(one.build().operations().size() == 1, "duplicate must not enter the spec");

        var nested = KTClientConfigSpec.builder();
        nested.push("left");
        nested.defineBoolean("shared", true);
        nested.pop();
        nested.push("right");
        nested.defineBoolean("shared", false);
        nested.pop();
        check(nested.build().operations().size() == 6, "same entry in separate sections must remain valid");

        var repeated = KTClientConfigSpec.builder();
        repeated.push("same");
        repeated.defineBoolean("key", true);
        repeated.pop();
        repeated.push("same");
        reject(() -> repeated.defineBoolean("key", false));
        repeated.defineBoolean("newKey", false);
        repeated.pop();
        check(repeated.build().operations().size() == 6, "reopening a section must not silently duplicate keys");
        System.out.println("PASS: " + checks + " duplicate client-config path checks");
    }
}
