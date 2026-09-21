package dev.xyat.kineticcore.api.config.client;

import net.minecraft.network.chat.Component;

/** Regression: an entry that fails construction must not reserve its identifier. */
public final class KTConfigPageBuilderAtomicRegression {
    private static int cases;

    public static void main(String[] args) {
        KTConfigPage.Builder builder = KTConfigPage.builder("test:builder_atomic", Component.empty());
        reject(() -> builder.intValue("count", Component.empty(), () -> 0, ignored -> {},
                20, 0, 10, null));
        builder.intValue("count", Component.empty(), () -> 5, ignored -> {}, 5, 0, 10, null);
        check(builder.build().entries().size() == 1,
                "a rejected default must not reserve the entry id");

        KTConfigPage.Builder callback = KTConfigPage.builder("test:builder_callback", Component.empty());
        reject(() -> callback.action("run", Component.empty(), null, null));
        callback.action("run", Component.empty(), () -> {}, null);
        check(callback.build().entries().size() == 1,
                "a failed action registration must not reserve the entry id");

        KTConfigPage.Builder duplicate = KTConfigPage.builder("test:builder_duplicate", Component.empty());
        duplicate.action("run", Component.empty(), () -> {}, null);
        reject(() -> duplicate.action("run", Component.empty(), () -> {}, null));
        check(duplicate.build().entries().size() == 1,
                "valid duplicate ids must still be rejected");
        System.out.println("PASS: " + cases + " config-page atomic registration cases");
    }

    private static void reject(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException | NullPointerException expected) {
            return;
        }
        throw new AssertionError("expected invalid registration to fail");
    }

    private static void check(boolean ok, String reason) {
        cases++;
        if (!ok) throw new AssertionError(reason);
    }
}
