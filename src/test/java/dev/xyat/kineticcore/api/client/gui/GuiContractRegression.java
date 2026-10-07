package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.gui.input.KeyModifiers;
import dev.xyat.kineticcore.api.client.gui.input.MouseButton;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticTexture;
import dev.xyat.kineticcore.api.client.gui.state.DragStateController;
import dev.xyat.kineticcore.api.client.gui.state.EditedEntryTracker;
import dev.xyat.kineticcore.api.client.gui.state.LayerState;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Headless contract checks for the Minecraft-free parts of the public GUI API. */
public final class GuiContractRegression {
    private static int cases;

    public static void main(String[] args) {
        verifyMouseButtons();
        verifyMouseInput();
        verifyScrollInput();
        verifyItemGridDensity();
        verifyTextures();
        verifyStateHolders();
        System.out.println("PASS: " + cases + " GUI API contract checks");
    }

    private static void verifyMouseButtons() {
        check(MouseButton.of(0) == MouseButton.LEFT, "raw button 0 is the primary button");
        check(MouseButton.of(1) == MouseButton.RIGHT, "raw button 1 is the secondary button");
        check(MouseButton.of(2) == MouseButton.MIDDLE, "raw button 2 is the middle button");
        check(MouseButton.of(3) == MouseButton.OTHER && MouseButton.of(-1) == MouseButton.OTHER,
                "side and invalid buttons map to OTHER");
    }

    private static void verifyMouseInput() {
        int modifiers = KeyModifiers.SHIFT | KeyModifiers.ALT;
        MouseInput input = new MouseInput(10, 20, MouseButton.of(1), 1, modifiers);
        check(input.isRight() && !input.isLeft() && !input.isMiddle(), "button predicates follow the mapped button");
        check(input.hasShift() && input.hasAlt() && !input.hasControl(), "modifier predicates read the bit mask");
        check(KeyModifiers.control(KeyModifiers.CONTROL | KeyModifiers.SUPER)
                        && !KeyModifiers.shift(KeyModifiers.SUPER),
                "modifier bits are independent");
        check(input.inside(10, 20, 1, 1), "rectangle includes its top-left pixel");
        check(!input.inside(0, 0, 10, 20), "rectangle excludes its right and bottom edges");
        check(!input.inside(10, 20, 0, 0), "empty rectangle contains nothing");
    }

    private static void verifyScrollInput() {
        ScrollInput scroll = new ScrollInput(4.5, 9.5, 0, 1);
        check(scroll.inside(4, 9, 1, 1), "fractional page coordinates fall inside their pixel");
        check(!scroll.inside(5, 9, 10, 10), "left edge is exclusive of earlier pixels");
    }

    private static void verifyItemGridDensity() {
        check(ItemGridDensity.COMPACT.slotSize() == 22, "compact slots leave two pixels inside the border around a native icon");
        int previousSlot = 0;
        for (ItemGridDensity density : ItemGridDensity.values()) {
            check(density.slotSize() > 0 && density.gap() >= 0 && density.padding() >= 0
                            && density.renderScale() > 0F,
                    density + " must have usable dimensions");
            check(density.cellPitch() == density.slotSize() + density.gap(), density + " pitch is slot plus gap");
            check(density.slotSize() > previousSlot, "densities are ordered from smallest to largest slot");
            previousSlot = density.slotSize();
        }
        check(ItemGridDensity.PREVIEW.decorations() && !ItemGridDensity.STANDARD.decorations(),
                "only the preview density draws stack decorations");
    }

    private static void verifyTextures() {
        KineticTexture texture = KineticTexture.of("examplemod", "textures/gui/panel.png");
        check(texture.textureWidth() == 256 && texture.textureHeight() == 256, "default texture size is 256x256");
        expectIllegalArgument(() -> KineticTexture.of(" ", "textures/gui/panel.png"), "blank namespace");
        expectIllegalArgument(() -> KineticTexture.of("examplemod", ""), "blank path");
        expectIllegalArgument(() -> KineticTexture.of("examplemod", "a.png", 0, 16), "zero width");
        expectIllegalArgument(() -> KineticTexture.of("examplemod", "a.png", 16, -1), "negative height");
    }

    private static void verifyStateHolders() {
        DragStateController<String> drag = new DragStateController<>();
        Object payload = new Object();
        drag.start("row", payload);
        check(drag.isActive() && drag.payload() == payload && "row".equals(drag.type()), "drag keeps its payload");
        drag.clear();
        check(!drag.isActive() && drag.payload() == null, "clear releases the drag payload");

        LayerState<String> layers = new LayerState<>();
        layers.open("menu");
        layers.close("dialog");
        check(layers.isOpen("menu"), "closing another layer keeps the open one");
        layers.open("dialog");
        check(!layers.isOpen("menu") && "dialog".equals(layers.activeLayer()), "opening a layer replaces the previous one");

        EditedEntryTracker<String> edits = new EditedEntryTracker<>();
        List<String> entries = List.of("a", "b", "c", "d");
        edits.refresh(entries, "c"::equals);
        check(edits.update("a", true), "first edit reports a change");
        check(!edits.update("a", true), "repeated edit reports no change");
        check(edits.update("d", true), "second edit reports a change");
        check(edits.update("c", false), "revert reports a change");
        List<String> sorted = new ArrayList<>(entries);
        sorted.sort(edits.comparator(Comparator.naturalOrder()));
        check(sorted.equals(List.of("d", "a", "b", "c")), "newest edit first, then older edits, then clean rows");
    }

    private static void expectIllegalArgument(Runnable action, String label) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            cases++;
            return;
        }
        throw new AssertionError(label + " must be rejected");
    }

    private static void check(boolean condition, String message) {
        cases++;
        if (!condition) throw new AssertionError(message);
    }
}
