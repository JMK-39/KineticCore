package dev.xyat.kineticcore.internal.client.screen;

import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.Field;
import java.util.Map;

/** Reopening a screen from no current screen must not retain its previous caller. */
public final class GuiRootNavigationRegression {
    public static void main(String[] args) throws Exception {
        Screen caller = new GuiTestScreens.Plain();
        KineticScreen editor = new GuiTestScreens.Kinetic();
        GuiSessionRuntime.handleScreenOpening(caller, editor);
        check(parents().get(editor) == caller, "initial parent");
        GuiSessionRuntime.handleScreenOpening(editor, null);
        GuiSessionRuntime.handleScreenOpening(null, editor);
        check(!parents().containsKey(editor), "opening as root must clear old parent");
        GuiSessionRuntime.handleScreenOpening(caller, editor);
        check(parents().get(editor) == caller, "normal opening still records parent");
        System.out.println("PASS: 3 root navigation checks");
    }
    @SuppressWarnings("unchecked")
    private static Map<Screen, Screen> parents() throws Exception {
        Field f = GuiSessionRuntime.class.getDeclaredField("PARENTS");
        f.setAccessible(true);
        return (Map<Screen, Screen>) f.get(null);
    }
    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
