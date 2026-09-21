package dev.xyat.kineticcore.internal.client.screen;

import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.Map;

/** Headless checks for reused screen navigation and ancestor navigation. */
public final class GuiSessionNavigationRegression {
    public static void main(String[] args) throws Exception {
        reopenedScreenReturnsToNewCaller();
        ancestorNavigationDoesNotCorruptParentChain();
        directParentNavigationKeepsOriginalParent();
        System.out.println("PASS: 3 GUI navigation regression cases");
    }

    private static void reopenedScreenReturnsToNewCaller() throws Exception {
        Screen firstCaller = new GuiTestScreens.Plain();
        Screen secondCaller = new GuiTestScreens.Plain();
        KineticScreen editor = new GuiTestScreens.Kinetic();
        GuiSessionRuntime.handleScreenOpening(firstCaller, editor);
        check(parents().get(editor) == firstCaller, "first opening must record first caller");
        GuiSessionRuntime.handleScreenOpening(editor, firstCaller);
        GuiSessionRuntime.handleScreenOpening(secondCaller, editor);
        check(parents().get(editor) == secondCaller,
                "reused editor must navigate back to its latest caller, not a stale previous caller");
    }

    private static void ancestorNavigationDoesNotCorruptParentChain() throws Exception {
        Screen external = new GuiTestScreens.Plain();
        KineticScreen root = new GuiTestScreens.Kinetic();
        KineticScreen child = new GuiTestScreens.Kinetic();
        KineticScreen grandchild = new GuiTestScreens.Kinetic();
        GuiSessionRuntime.handleScreenOpening(external, root);
        GuiSessionRuntime.handleScreenOpening(root, child);
        GuiSessionRuntime.handleScreenOpening(child, grandchild);
        GuiSessionRuntime.handleScreenOpening(grandchild, root);
        check(parents().get(root) == external,
                "navigating to an ancestor must preserve its original external parent");
        check(parents().get(child) == root && parents().get(grandchild) == child,
                "ancestor navigation must not introduce a cycle");
    }

    private static void directParentNavigationKeepsOriginalParent() throws Exception {
        Screen external = new GuiTestScreens.Plain();
        KineticScreen root = new GuiTestScreens.Kinetic();
        KineticScreen child = new GuiTestScreens.Kinetic();
        GuiSessionRuntime.handleScreenOpening(external, root);
        GuiSessionRuntime.handleScreenOpening(root, child);
        GuiSessionRuntime.handleScreenOpening(child, root);
        check(parents().get(root) == external, "returning to direct parent must not change its caller");
    }

    @SuppressWarnings("unchecked")
    private static Map<Screen, Screen> parents() throws Exception {
        Field field = GuiSessionRuntime.class.getDeclaredField("PARENTS");
        field.setAccessible(true);
        return (Map<Screen, Screen>) field.get(null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
