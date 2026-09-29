package dev.xyat.kineticcore.internal.client.gui.widget;

import dev.xyat.kineticcore.api.client.gui.state.DragStateController;
import dev.xyat.kineticcore.api.client.gui.state.LayerState;

/** Headless regression: an empty layer is not open and a null drag cannot retain payload. */
public final class WidgetLifecycleRegression {
    public static void main(String[] args) {
        verifyStandaloneLayer();
        verifyDragRejectsNullType();
        System.out.println("PASS: 2 widget lifecycle regression cases");
    }

    private static void verifyStandaloneLayer() {
        LayerState<String> state = new LayerState<>();
        check(!state.isOpen(null), "empty LayerState must not report null as open");
        state.open("palette");
        check(state.isOpen("palette"), "real layer remains open");
        state.closeAll();
        check(!state.isOpen(null), "closed LayerState must not report null as open");
    }

    private static void verifyDragRejectsNullType() {
        DragStateController<String> state = new DragStateController<>();
        Object payload = new Object();
        try {
            state.start(null, payload);
            throw new AssertionError("null drag type must be rejected");
        } catch (NullPointerException expected) {
        }
        check(!state.isActive() && state.type() == null && state.payload() == null,
                "invalid drag must not retain a payload");
        state.start("row", payload);
        check(state.isActive() && state.payload() == payload, "valid drag still works");
        state.clear();
        check(!state.isActive() && state.payload() == null, "clear releases drag state");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
