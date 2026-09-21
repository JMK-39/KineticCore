package dev.xyat.kineticcore.internal.client.screen;

import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.Field;
import java.util.Map;

/** Headless regression: leaving an edit flow releases shared draft callbacks and screen references. */
public final class GuiSessionLifecycleRegression {
    public static void main(String[] args) throws Exception {
        leavingFlowReleasesDraftReferences();
        returningToParentRetainsDraft();
        failedRestoreStillReleasesDraft();
        System.out.println("PASS: 3 GUI draft lifecycle regression cases");
    }

    private static void leavingFlowReleasesDraftReferences() throws Exception {
        KineticScreen owner = new GuiTestScreens.Kinetic();
        KineticScreen child = new GuiTestScreens.Kinetic();
        Screen external = new GuiTestScreens.Plain();
        int[] value = { 10 };
        GuiSessionRuntime.configureDraft(owner, () -> value[0], initial -> value[0] = initial, false);
        GuiSessionRuntime.handleScreenOpening(owner, child);
        value[0] = 20;
        check(GuiSessionRuntime.hasUnsavedEdits(child), "child must share owner's draft");
        GuiSessionRuntime.handleScreenOpening(child, external);
        check(value[0] == 10, "leaving must discard uncommitted changes");
        check(!drafts().containsKey(owner) && !drafts().containsKey(child),
                "abandoned draft must release owner and child references");
    }

    private static void returningToParentRetainsDraft() throws Exception {
        KineticScreen owner = new GuiTestScreens.Kinetic();
        KineticScreen child = new GuiTestScreens.Kinetic();
        Screen outer = new GuiTestScreens.Plain();
        int[] value = { 1 };
        GuiSessionRuntime.handleScreenOpening(outer, owner);
        GuiSessionRuntime.configureDraft(owner, () -> value[0], initial -> value[0] = initial, false);
        GuiSessionRuntime.handleScreenOpening(owner, child);
        value[0] = 2;
        GuiSessionRuntime.handleScreenOpening(child, owner);
        check(value[0] == 2 && GuiSessionRuntime.hasUnsavedEdits(owner),
                "returning to parent must retain edits and shared session");
        check(drafts().containsKey(owner), "active owner must retain its draft");
        GuiSessionRuntime.handleScreenOpening(owner, outer);
        check(value[0] == 1, "exiting after return must discard unsaved edits");
        check(!drafts().containsKey(owner) && !drafts().containsKey(child),
                "finished flow must release both shared screen references");
    }

    private static void failedRestoreStillReleasesDraft() throws Exception {
        KineticScreen owner = new GuiTestScreens.Kinetic();
        Screen external = new GuiTestScreens.Plain();
        int[] value = { 5 };
        IllegalStateException restoreFailure = new IllegalStateException("restore failed");
        GuiSessionRuntime.configureDraft(owner, () -> value[0], initial -> {
            throw restoreFailure;
        }, false);
        value[0] = 6;
        try {
            GuiSessionRuntime.handleScreenOpening(owner, external);
            throw new AssertionError("restore failure must propagate");
        } catch (IllegalStateException received) {
            check(received == restoreFailure, "original restore failure must be retained");
        }
        check(!drafts().containsKey(owner), "failed restoration must not retain abandoned callbacks");
    }

    @SuppressWarnings("unchecked")
    private static Map<Screen, ?> drafts() throws Exception {
        Field field = GuiSessionRuntime.class.getDeclaredField("DRAFTS");
        field.setAccessible(true);
        return (Map<Screen, ?>) field.get(null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
