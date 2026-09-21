package dev.xyat.kineticcore.internal.client.screen;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.Field;
import java.util.Map;

/** Capturing a replacement draft must not destroy an existing valid draft. */
public final class GuiDraftConfigureFailureRegression {
    private static int cases;
    private static void check(boolean condition, String message) {
        cases++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        KineticScreen fresh = new GuiTestScreens.Kinetic();
        try {
            GuiSessionRuntime.configureDraft(fresh, () -> { throw new IllegalStateException("capture"); }, value -> {}, false);
            throw new AssertionError("failed initial capture must propagate");
        } catch (IllegalStateException expected) { }
        check(!drafts().containsKey(fresh), "failed initial configuration left an abandoned empty session");

        KineticScreen screen = new GuiTestScreens.Kinetic();
        int[] original = {5};
        GuiSessionRuntime.configureDraft(screen, () -> original[0], value -> original[0] = value, false);
        original[0] = 10;
        try {
            GuiSessionRuntime.configureDraft(screen, () -> { throw new IllegalStateException("replacement"); }, value -> {}, false);
            throw new AssertionError("failed replacement capture must propagate");
        } catch (IllegalStateException expected) { }
        check(GuiSessionRuntime.hasUnsavedEdits(screen), "failed replacement destroyed old capture callback");
        GuiSessionRuntime.discardDraft(screen);
        check(original[0] == 5, "failed replacement destroyed old rollback callback or baseline");
        GuiSessionRuntime.handleScreenOpening(screen, new GuiTestScreens.Plain());
        check(!drafts().containsKey(screen), "draft must still release on exit after failed reconfiguration");
        System.out.println("PASS: " + cases + " draft configuration regression cases");
    }
    @SuppressWarnings("unchecked")
    private static Map<Screen, ?> drafts() throws Exception {
        Field field = GuiSessionRuntime.class.getDeclaredField("DRAFTS");
        field.setAccessible(true);
        return (Map<Screen, ?>) field.get(null);
    }
}
