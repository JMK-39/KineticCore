package dev.xyat.kineticcore.api.config.client;

import net.minecraft.network.chat.Component;

/** Duplicate persisted choice values make selection and display ambiguous. */
public final class KTConfigChoiceUniquenessRegression {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void reject(Runnable action) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("duplicate persisted choice values must be rejected");
    }
    public static void main(String[] args) {
        var builder = KTConfigPage.builder("test:choices", Component.empty());
        reject(() -> builder.choice("mode", Component.empty(), () -> "a", v -> {}, "a", null, "a", "a"));
        check(builder.build().entries().isEmpty(), "failed registration must not consume the entry identifier");
        var retry = KTConfigPage.builder("test:retry", Component.empty());
        reject(() -> retry.choiceOptions("mode", Component.empty(), () -> "a", v -> {}, "a", null,
                new KTConfigEntry.ChoiceOption("a", Component.empty(), Component.empty()),
                new KTConfigEntry.ChoiceOption(" a ", Component.empty(), Component.empty())));
        retry.choice("mode", Component.empty(), () -> "a", v -> {}, "a", null, "a", "b");
        check(retry.build().entries().size() == 1, "valid retry must succeed after rejected duplicate options");
        System.out.println("PASS: " + checks + " choice uniqueness checks");
    }
}
