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
     * a button or control is that control's label and is not compared with it.
     */
    static List<String> frameProblems(List<LayoutFrameRecorder.Box> boxes) {
        List<String> problems = new ArrayList<>();
        List<LayoutFrameRecorder.Box> frames = new ArrayList<>();
        List<LayoutFrameRecorder.Box> items = new ArrayList<>();
        for (LayoutFrameRecorder.Box box : boxes) (box.kind() == LayoutFrameRecorder.Kind.FRAME ? frames : items).add(box);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (LayoutFrameRecorder.Box item : items) {
            for (LayoutFrameRecorder.Box frame : frames) {
                // A frame drawn as a control's own background, or the control's own outline.
                if (Math.abs(frame.x() - item.x()) <= 1 && Math.abs(frame.y() - item.y()) <= 1
                        && Math.abs(frame.right() - item.right()) <= 1 && Math.abs(frame.bottom() - item.bottom()) <= 1) continue;
                boolean overlaps = item.x() < frame.right() && item.right() > frame.x() && item.y() < frame.bottom() && item.bottom() > frame.y();
                if (!overlaps) continue;
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
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                LayoutFrameRecorder.Box a = items.get(i);
                LayoutFrameRecorder.Box b = items.get(j);
                if (a.kind() == LayoutFrameRecorder.Kind.CONTROL && b.kind() == LayoutFrameRecorder.Kind.CONTROL) continue;
                boolean overlaps = a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
                if (!overlaps) continue;
                // Text inside a button or control is its label; anything inside a control is drawn by that control.
                if (a.inside(b.x(), b.y(), b.right(), b.bottom())
                        && (a.kind() == LayoutFrameRecorder.Kind.TEXT || b.kind() == LayoutFrameRecorder.Kind.CONTROL)) continue;
                if (b.inside(a.x(), a.y(), a.right(), a.bottom())
                        && (b.kind() == LayoutFrameRecorder.Kind.TEXT || a.kind() == LayoutFrameRecorder.Kind.CONTROL)) continue;
                String problem = "overlap: " + describe(a) + " / " + describe(b);
                if (seen.add(problem)) problems.add(problem);
            }
        }
        return problems;
    }

    private static String describe(LayoutFrameRecorder.Box box) {
        return box.kind().name().toLowerCase(java.util.Locale.ROOT) + (box.label().isEmpty() ? "" : "[" + box.label() + "]")
                + " @" + box.x() + "," + box.y() + " " + box.width() + "x" + box.height();
    }

    private static String compare(AbstractWidget a, AbstractWidget b) {
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

    private static String describe(AbstractWidget widget) {
        String label = widget.getMessage() == null ? "" : widget.getMessage().getString();
        if (label.length() > 32) label = label.substring(0, 32) + "...";
        return widget.getClass().getSimpleName() + "[" + label + "] @" + widget.getX() + "," + widget.getY()
                + " " + widget.getWidth() + "x" + widget.getHeight();
    }
}
