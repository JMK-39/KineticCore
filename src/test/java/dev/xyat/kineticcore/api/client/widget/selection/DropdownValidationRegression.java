package dev.xyat.kineticcore.api.client.widget.selection;

import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Option;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Invalid choices must never be persisted through a dropdown responder. */
public final class DropdownValidationRegression {
    private static int tests;
    public static void main(String[] args) {
        AtomicInteger calls = new AtomicInteger();
        Dropdown dropdown = KineticWidgets.createDropdown(
                0, 0, 100,
                List.of(new Option("allowed", Component.empty(), Component.empty()),
                        new Option("blocked", Component.empty(), Component.empty())),
                "allowed", null, "allowed"::equals, value -> calls.incrementAndGet(), null);
        dropdown.choose(1);
        check("allowed".equals(dropdown.selectedValue()), "rejected choice changed the current selection");
        check(calls.get() == 0, "rejected choice reached the business callback");
        dropdown.choose(0);
        check(calls.get() == 1, "valid choice did not reach the business callback");
        Dropdown failedWriter = KineticWidgets.createDropdown(
                0, 0, 100,
                List.of(new Option("original", Component.empty(), Component.empty()),
                        new Option("next", Component.empty(), Component.empty())),
                "original", null, ignored -> true, value -> { throw new IllegalStateException("write rejected"); }, null);
        try {
            failedWriter.choose(1);
            throw new AssertionError("writer failure was swallowed");
        } catch (IllegalStateException expected) {
            check("original".equals(failedWriter.selectedValue()),
                    "failed business callback left an unpersisted value selected");
        }
        System.out.println("PASS: " + tests + " dropdown validation cases");
    }
    private static void check(boolean result, String message) {
        tests++;
        if (!result) throw new AssertionError(message);
    }
}
