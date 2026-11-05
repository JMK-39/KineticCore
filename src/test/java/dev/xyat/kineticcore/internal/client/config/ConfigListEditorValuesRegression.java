package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ConfigListEditorValuesRegression {
    public static void main(String[] args) {
        AtomicReference<List<Integer>> stored = new AtomicReference<>(List.of(0, 1, 2, 3));
        var page = KTConfigPage.builder("test:custom_integer_list", Component.empty())
                .intListValidated("radii", Component.empty(), stored::get, stored::set, stored.get(),
                        values -> values.stream().allMatch(value -> value >= -1 && value <= 16), null).build();
        var entry = page.entries().get(0);
        var converted = ConfigListEditorValues.resultFor(entry, List.of("-1", "0", "8", "16"));
        check(converted.equals(List.of(-1, 0, 8, 16)) && entry.accepts(converted), "visual drafts must become typed integer values");
        check(stored.get().equals(List.of(0, 1, 2, 3)), "conversion must not save or mutate the source");
        entry.writeSnapshot(converted);
        check(stored.get().equals(List.of(-1, 0, 8, 16)), "accepted integer drafts must use the existing typed writer");
        for (var invalid : List.of(List.of("1.5"), List.of("2147483648"), List.of("17"))) {
            check(!entry.accepts(ConfigListEditorValues.resultFor(entry, invalid)), "invalid and out-of-range drafts must remain unsaveable");
        }
        var strings = KTConfigPage.builder("test:custom_strings", Component.empty())
                .stringList("labels", Component.empty(), () -> List.of("001"), ignored -> {}, List.of("001"), null).build().entries().get(0);
        check(ConfigListEditorValues.resultFor(strings, List.of("001")).equals(List.of("001")), "string editors must preserve numeric-looking text");
        System.out.println("PASS: visual integer-list conversion, invalid drafts and string preservation");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
