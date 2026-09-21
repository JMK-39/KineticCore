package dev.xyat.kineticcore.api.client.widget;

import dev.xyat.kineticcore.api.client.widget.state.DragStateController;
import dev.xyat.kineticcore.api.client.widget.state.KineticUiState;
import dev.xyat.kineticcore.api.client.widget.state.LayerState;

/** Headless regression: an empty layer is not open and a null drag cannot retain payload. */
public final class WidgetLifecycleRegression {
    public static void main(String[] args) {
        verifyStandaloneLayer();
        verifyUnifiedLayer();
        verifyDragRejectsNullType();
        System.out.println("PASS: 3 widget lifecycle regression cases");
    }

    private static void verifyStandaloneLayer() {
        LayerState<String> state = new LayerState<>();
        check(!state.isOpen(null), "empty LayerState must not report null as open");
        state.open("palette");
        check(state.isOpen("palette"), "real layer remains open");
        state.closeAll();
        check(!state.isOpen(null), "closed LayerState must not report null as open");
    }

    private static void verifyUnifiedLayer() {
        KineticUiState.Layer<String> state = new KineticUiState.Layer<>();
        check(!state.isOpen(null), "empty KineticUiState.Layer must not report null as open");
        state.open("menu");
        check(state.isOpen("menu"), "real unified layer remains open");
        state.closeAll();
        check(!state.isOpen(null), "closed KineticUiState.Layer must not report null as open");
    }

    private static void verifyDragRejectsNullType() {
        DragStateController<String> state = new DragStateController<>();
        Object payload = new Object();
        try {
            state.start(null, payload);
            throw new AssertionError("null drag type must be rejected");
        } catch (NullPointerException expected) {
            // Match KineticUiState.Drag: invalid drag cannot hold a payload.
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
