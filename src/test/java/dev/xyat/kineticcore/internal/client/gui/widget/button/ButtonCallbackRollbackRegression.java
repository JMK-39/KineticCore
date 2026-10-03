package dev.xyat.kineticcore.internal.client.gui.widget.button;

import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.CycleButton;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** A rejected value or failed writer must not leave a control showing an unpersisted value. */
public final class ButtonCallbackRollbackRegression {
    public static void main(String[] args) {
        Component label = Component.literal("label");
        AtomicInteger toggleWrites = new AtomicInteger();
        ToggleButton rejected = KineticWidgets.createToggleButton(0, 0, 80, false,
                label, label, null, value -> !value, value -> toggleWrites.incrementAndGet());
        press(rejected);
        check(!rejected.value() && toggleWrites.get() == 0, "rejected toggle reached business callback");
        ToggleButton failing = KineticWidgets.createToggleButton(0, 0, 80, false,
                label, label, null, value -> true, value -> { throw new IllegalStateException("write failed"); });
        reject(() -> press(failing));
        check(!failing.value(), "failed toggle write left false configuration displayed as true");
        ToggleButton success = KineticWidgets.createToggleButton(0, 0, 80, false,
                label, label, null, value -> true, value -> toggleWrites.incrementAndGet());
        press(success);
        check(success.value() && toggleWrites.get() == 1, "valid toggle must persist");
        AtomicInteger cycleWrites = new AtomicInteger();
        List<Component> options = List.of(Component.literal("zero"), Component.literal("one"));
        CycleButton rejectedCycle = KineticWidgets.createCycleButton(0, 0, 80, 0,
                options, null, index -> index == 0, index -> cycleWrites.incrementAndGet());
        press(rejectedCycle);
        check(rejectedCycle.index() == 0 && cycleWrites.get() == 0, "rejected cycle reached business callback");
        CycleButton failingCycle = KineticWidgets.createCycleButton(0, 0, 80, 0,
                options, null, index -> true, index -> { throw new IllegalStateException("write failed"); });
        reject(() -> press(failingCycle));
        check(failingCycle.index() == 0, "failed cycle write left new index selected");
        CycleButton successCycle = KineticWidgets.createCycleButton(0, 0, 80, 0,
                options, null, index -> true, index -> cycleWrites.incrementAndGet());
        press(successCycle);
        check(successCycle.index() == 1 && cycleWrites.get() == 1, "valid cycle must persist");
        AtomicInteger toggleValidationCalls = new AtomicInteger();
        ToggleButton onceToggle = KineticWidgets.createToggleButton(0, 0, 80, false,
                label, label, null, value -> { toggleValidationCalls.incrementAndGet(); return true; }, value -> { });
        toggleValidationCalls.set(0);
        press(onceToggle);
        check(toggleValidationCalls.get() == 1, "one toggle click must not validate twice");
        AtomicInteger cycleValidationCalls = new AtomicInteger();
        CycleButton onceCycle = KineticWidgets.createCycleButton(0, 0, 80, 0,
                options, null, index -> { cycleValidationCalls.incrementAndGet(); return true; }, index -> { });
        cycleValidationCalls.set(0);
        press(onceCycle);
        check(cycleValidationCalls.get() == 1, "one cycle click must not validate twice");
        System.out.println("PASS: 8 toggle/cycle callback checks");
    }
    private static void press(net.minecraft.client.gui.components.AbstractButton button) {
        //? if >=26.1 {
        /*button.onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
        *///?} else {
        button.onPress();
        //?}
    }
    private static void reject(Runnable work) {
        try { work.run(); throw new AssertionError("writer exception swallowed"); }
        catch (IllegalStateException expected) { }
    }
    private static void check(boolean yes, String message) { if (!yes) throw new AssertionError(message); }
}
