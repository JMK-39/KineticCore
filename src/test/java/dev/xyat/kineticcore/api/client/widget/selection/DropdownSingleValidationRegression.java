package dev.xyat.kineticcore.api.client.widget.selection;

import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Option;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** A stateful addon validator must be evaluated exactly once per user selection. */
public final class DropdownSingleValidationRegression {
    private static int assertions;
    private static void check(boolean yes, String reason) { assertions++; if (!yes) throw new AssertionError(reason); }
    public static void main(String[] args) {
        AtomicInteger checks = new AtomicInteger();
        AtomicInteger saved = new AtomicInteger();
        Dropdown dropdown = KineticWidgets.createDropdown(
                0, 0, 100,
                List.of(new Option("initial", Component.empty(), Component.empty()),
                        new Option("next", Component.empty(), Component.empty())),
                "initial", null, value -> checks.incrementAndGet() % 2 == 1,
                value -> saved.incrementAndGet(), null);
        check(checks.get() == 1, "initial presentation must validate exactly once");
        dropdown.choose(1); // second validation rejects the selection.
        check(saved.get() == 0, "invalid selection reached addon callback");
        check("initial".equals(dropdown.selectedValue()), "rejected selection mutated widget state");
        check(checks.get() == 2, "rejected option validated more than once");
        dropdown.choose(1); // third validation allows this selection.
        check(saved.get() == 1, "valid selection not forwarded exactly once");
        check("next".equals(dropdown.selectedValue()), "valid selection not displayed");
        check(checks.get() == 3, "accepted option validated more than once");
        System.out.println("PASS: " + assertions + " dropdown single-validation cases");
    }
}
