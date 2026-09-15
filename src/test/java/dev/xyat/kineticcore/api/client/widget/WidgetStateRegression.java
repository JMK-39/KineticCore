package dev.xyat.kineticcore.api.client.widget;

import dev.xyat.kineticcore.api.client.widget.state.DragStateController;
import dev.xyat.kineticcore.api.client.widget.state.EditedEntryTracker;
import dev.xyat.kineticcore.api.client.widget.state.LayerState;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Headless regression checks for the public widget state implementations. */
public final class WidgetStateRegression {
    public static void main(String[] args) {
        verifyDrag(new DragStateController<>());
        verifyEditedOrder(new EditedEntryTracker<>());
        verifyLayers(new LayerState<>());
        verifyScrollState();
        System.out.println("Widget state regression: public implementations passed.");
    }

    private static void verifyDrag(DragStateController<String> drag) {
        check(!drag.isActive(), "new drag must be inactive");
        Object payload = new Object();
        drag.start("row", payload);
        check(drag.isActive() && drag.payload() == payload && "row".equals(drag.type()), "drag payload");
        drag.clear();
        check(!drag.isActive() && drag.payload() == null && drag.type() == null, "clear drag references");
    }

    private static void verifyEditedOrder(EditedEntryTracker<String> edits) {
        edits.refresh(List.of("a", "b", "c"), "a"::equals);
        check(edits.update("b", true), "first edit changes order");
        check(!edits.update("b", true), "repeated edit preserves order");
        List<String> rows = new ArrayList<>(List.of("c", "a", "b"));
        rows.sort(edits.comparator(Comparator.naturalOrder()));
        check(rows.equals(List.of("b", "a", "c")), "new edits before baseline edits before clean rows");
        check(edits.update("b", false) && !edits.isEdited("b"), "revert edit");
        edits.clear();
        rows.sort(edits.comparator(Comparator.naturalOrder()));
        check(rows.equals(List.of("a", "b", "c")), "clear restores fallback ordering");
    }

    private static void verifyLayers(LayerState<String> layers) {
        layers.open("menu");
        layers.close("dialog");
        check(layers.isOpen("menu"), "closing unrelated layer preserves active layer");
        layers.open("dialog");
        check(!layers.isOpen("menu") && layers.isOpen("dialog"), "single active layer");
        layers.closeAll();
        check(!layers.isAnyOpen() && layers.activeLayer() == null, "clear layers");
    }

    private static void verifyScrollState() {
        KineticScroll.State state = new KineticScroll.State();
        state.snap(500, 100);
        check(state.target() == 100 && state.current() == 100, "clamp scroll to maximum");
        check(state.wheel(100, 1, 20, 100) == 80, "integer wheel direction and step");
        check(state.follow(80, 50, true) == 50, "shrinking range clamps immediate position");
        state.snap(80D, 100D);
        check(state.follow(80D, 50D, true) == 50D, "precise follow clamps shrinking range");
        state.snap(80D, 100D);
        check(state.wheel(80D, 0D, 20D, 50D) == 50D, "zero precise wheel clamps shrinking range");
        state.snap(80D, 100D);
        check(state.wheel(80, 0D, 20D, 50D) == 50, "zero integer wheel clamps shrinking range");
        check(state.wheel(50D, -100D, 20D, 100D) == 100, "precise wheel clamps upper end");
        state.snap(-20, 100);
        check(state.current() == 0, "clamp lower end");
        check(state.update(40, 0, true) == 0, "empty scroll range");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
