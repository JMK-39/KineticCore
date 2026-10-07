package dev.xyat.kineticcore.internal.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout check for GUI validation fixtures: lists visible controls of the open screen that overlap each other or
 * sit closer than {@link #MIN_GAP} px to a neighbor. Text placement inside controls is enforced by the drawing
 * helpers themselves, so only the control rectangles are compared here.
 */
public final class LayoutCheck {
    /** Smallest gap between two neighboring controls. */
    public static final int MIN_GAP = 2;

    /** Number of controls compared by the last check, so fixtures can tell a clean screen from an empty one. */
    public static int lastCheckedControls;

    private LayoutCheck() {
    }

    /** Problems on the currently open screen, one readable line each; empty when the layout is clean. */
    public static List<String> currentScreenProblems() {
        Screen screen = Minecraft.getInstance().screen;
        List<String> problems = new ArrayList<>();
        lastCheckedControls = 0;
        if (screen == null) return problems;
        List<AbstractWidget> widgets = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible && widget.getWidth() > 0 && widget.getHeight() > 0) {
                widgets.add(widget);
            }
        }
        lastCheckedControls = widgets.size();
        for (int i = 0; i < widgets.size(); i++) {
            for (int j = i + 1; j < widgets.size(); j++) {
                String problem = compare(widgets.get(i), widgets.get(j));
                if (problem != null) problems.add(problem);
            }
        }
        problems.addAll(frameProblems(LayoutFrameRecorder.lastFrame()));
        return problems;
    }

    /**
     * Text, page-drawn buttons and controls against the frame lines drawn in the same page frame: nothing may cross
     * or cover a frame line, and anything inside a frame keeps {@link #MIN_GAP} px from it. A text box that lies inside
     * a button or control is that control's label and is not compared with it. The boxes are in drawing order: a frame
     * drawn after an item that it overlaps is a popup or dialog covering that item, so the item is not compared with
     * that frame, nor with what the popup draws inside it.
     */
    static List<String> frameProblems(List<LayoutFrameRecorder.Box> boxes) {
        List<String> problems = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < boxes.size(); i++) {
            LayoutFrameRecorder.Box item = boxes.get(i);
            if (item.kind() == LayoutFrameRecorder.Kind.FRAME) continue;
            for (int f = 0; f < boxes.size(); f++) {
                LayoutFrameRecorder.Box frame = boxes.get(f);
                if (frame.kind() != LayoutFrameRecorder.Kind.FRAME) continue;
                // A frame drawn as a control's own background, or the control's own outline.
                if (Math.abs(frame.x() - item.x()) <= 1 && Math.abs(frame.y() - item.y()) <= 1
                        && Math.abs(frame.right() - item.right()) <= 1 && Math.abs(frame.bottom() - item.bottom()) <= 1) continue;
                boolean overlaps = item.x() < frame.right() && item.right() > frame.x() && item.y() < frame.bottom() && item.bottom() > frame.y();
                if (!overlaps) continue;
                // Drawn later over a text or page-drawn button: a popup covering it. Controls draw after the page.
                if (f > i && item.kind() != LayoutFrameRecorder.Kind.CONTROL) continue;
                if (insidePopupOver(boxes, f, i)) continue;
                if (item.x() <= frame.x() && item.y() <= frame.y() && item.right() >= frame.right() && item.bottom() >= frame.bottom()
                        && item.kind() != LayoutFrameRecorder.Kind.TEXT) continue;
                String problem;
                if (!item.inside(frame.x() + 1, frame.y() + 1, frame.right() - 1, frame.bottom() - 1)) {
                    problem = "on frame line: " + describe(item) + " / frame " + describe(frame);
                } else {
                    int gap = Math.min(Math.min(item.x() - frame.x() - 1, frame.right() - 1 - item.right()),
                            Math.min(item.y() - frame.y() - 1, frame.bottom() - 1 - item.bottom()));
                    if (gap >= MIN_GAP) continue;
                    problem = "gap " + gap + " px to frame line: " + describe(item) + " / frame " + describe(frame);
                }
                if (seen.add(problem)) problems.add(problem);
            }
        }
        for (int i = 0; i < boxes.size(); i++) {
            LayoutFrameRecorder.Box a = boxes.get(i);
            if (a.kind() == LayoutFrameRecorder.Kind.FRAME) continue;
            for (int j = i + 1; j < boxes.size(); j++) {
                LayoutFrameRecorder.Box b = boxes.get(j);
                if (b.kind() == LayoutFrameRecorder.Kind.FRAME) continue;
                if (a.kind() == LayoutFrameRecorder.Kind.CONTROL && b.kind() == LayoutFrameRecorder.Kind.CONTROL) continue;
                boolean overlaps = a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
                if (!overlaps) continue;
                // Text inside a button or control is its label; anything inside a control is drawn by that control.
                if (a.inside(b.x(), b.y(), b.right(), b.bottom())
                        && (a.kind() == LayoutFrameRecorder.Kind.TEXT || b.kind() == LayoutFrameRecorder.Kind.CONTROL)) continue;
                if (b.inside(a.x(), a.y(), a.right(), a.bottom())
                        && (b.kind() == LayoutFrameRecorder.Kind.TEXT || a.kind() == LayoutFrameRecorder.Kind.CONTROL)) continue;
                if (coveredBetween(boxes, i, j)) continue;
                String problem = "overlap: " + describe(a) + " / " + describe(b);
                if (seen.add(problem)) problems.add(problem);
            }
        }
        return problems;
    }

    // True when the item lies in a popup drawn after the frame: a later frame that holds the item and crosses the
    // frame's lines (a dropdown hanging over a panel edge). Boxes nested inside the frame do not count.
    private static boolean insidePopupOver(List<LayoutFrameRecorder.Box> boxes, int frameIndex, int itemIndex) {
        LayoutFrameRecorder.Box frame = boxes.get(frameIndex);
        LayoutFrameRecorder.Box item = boxes.get(itemIndex);
        for (int p = frameIndex + 1; p < itemIndex; p++) {
            LayoutFrameRecorder.Box popup = boxes.get(p);
            if (popup.kind() != LayoutFrameRecorder.Kind.FRAME) continue;
            if (!item.inside(popup.x(), popup.y(), popup.right(), popup.bottom())) continue;
            boolean overlaps = popup.x() < frame.right() && popup.right() > frame.x() && popup.y() < frame.bottom() && popup.bottom() > frame.y();
            if (overlaps && !popup.inside(frame.x(), frame.y(), frame.right(), frame.bottom())) return true;
        }
        return false;
    }

    // True when a frame drawn between the two items covers the earlier one and holds the later one: a popup over the page.
    private static boolean coveredBetween(List<LayoutFrameRecorder.Box> boxes, int earlier, int later) {
        LayoutFrameRecorder.Box a = boxes.get(earlier);
        LayoutFrameRecorder.Box b = boxes.get(later);
        if (a.kind() == LayoutFrameRecorder.Kind.CONTROL) return false;
        for (int f = earlier + 1; f < later; f++) {
            LayoutFrameRecorder.Box frame = boxes.get(f);
            if (frame.kind() != LayoutFrameRecorder.Kind.FRAME) continue;
            boolean coversA = a.x() < frame.right() && a.right() > frame.x() && a.y() < frame.bottom() && a.bottom() > frame.y();
            if (coversA && b.inside(frame.x(), frame.y(), frame.right(), frame.bottom())) return true;
        }
        return false;
    }

    private static String describe(LayoutFrameRecorder.Box box) {
        return box.kind().name().toLowerCase(java.util.Locale.ROOT) + (box.label().isEmpty() ? "" : "[" + box.label() + "]")
                + " @" + box.x() + "," + box.y() + " " + box.width() + "x" + box.height();
    }

    private static String compare(AbstractWidget a, AbstractWidget b) {
        // A control holding another one (a list and its row buttons) is one element, not an overlap.
        if (contains(a, b) || contains(b, a)) return null;
        int aRight = a.getX() + a.getWidth();
        int aBottom = a.getY() + a.getHeight();
        int bRight = b.getX() + b.getWidth();
        int bBottom = b.getY() + b.getHeight();
        int horizontalGap = Math.max(b.getX() - aRight, a.getX() - bRight);
        int verticalGap = Math.max(b.getY() - aBottom, a.getY() - bBottom);
        if (horizontalGap < 0 && verticalGap < 0) return "overlap: " + describe(a) + " and " + describe(b);
        // Side by side (sharing rows) or stacked (sharing columns) and closer than the minimum gap.
        if (verticalGap < 0 && horizontalGap < MIN_GAP) return "gap " + horizontalGap + " px: " + describe(a) + " and " + describe(b);
        if (horizontalGap < 0 && verticalGap < MIN_GAP) return "gap " + verticalGap + " px: " + describe(a) + " and " + describe(b);
        return null;
    }

    private static boolean contains(AbstractWidget outer, AbstractWidget inner) {
        return inner.getX() >= outer.getX() && inner.getY() >= outer.getY()
                && inner.getX() + inner.getWidth() <= outer.getX() + outer.getWidth()
                && inner.getY() + inner.getHeight() <= outer.getY() + outer.getHeight();
    }

    private static String describe(AbstractWidget widget) {
        String label = widget.getMessage() == null ? "" : widget.getMessage().getString();
        if (label.length() > 32) label = label.substring(0, 32) + "...";
        return widget.getClass().getSimpleName() + "[" + label + "] @" + widget.getX() + "," + widget.getY()
                + " " + widget.getWidth() + "x" + widget.getHeight();
    }
}
