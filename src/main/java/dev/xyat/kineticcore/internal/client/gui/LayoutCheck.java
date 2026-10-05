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
        return problems;
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
