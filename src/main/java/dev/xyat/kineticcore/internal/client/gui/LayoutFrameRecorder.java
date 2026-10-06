package dev.xyat.kineticcore.internal.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Records, for GUI validation only, what a page frame drew: the outlined frames (panels, row surfaces), the text boxes
 * and page-drawn buttons, and the controls, all in the page's own coordinates. {@link LayoutCheck} then reports text or
 * controls that cross, touch or come closer than {@link LayoutCheck#MIN_GAP} px to a frame line.
 * Off unless the game runs with {@code -Dkineticcore.layoutCheck=true}.
 */
public final class LayoutFrameRecorder {
    public static final boolean ENABLED = Boolean.getBoolean("kineticcore.layoutCheck");

    public enum Kind { FRAME, TEXT, BUTTON, CONTROL }

    public record Box(Kind kind, int x, int y, int width, int height, String label) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean inside(int left, int top, int rightEdge, int bottomEdge) {
            return x >= left && y >= top && right() <= rightEdge && bottom() <= bottomEdge;
        }
    }

    private static final List<Box> current = new ArrayList<>();
    private static List<Box> last = List.of();
    private static final Deque<int[]> clips = new ArrayDeque<>();
    private static boolean recording;
    //? if >=26.1 {
    /*private static org.joml.Matrix3x2f baseInverse;
    *///?} else {
    private static org.joml.Matrix4f baseInverse;
    //?}

    private LayoutFrameRecorder() {
    }

    /** Starts recording one page frame drawn with {@code graphics}, whose pose maps page coordinates. */
    public static void beginFrame(GuiGraphics graphics) {
        if (!ENABLED || graphics == null) return;
        current.clear();
        clips.clear();
        //? if >=26.1 {
        /*baseInverse = new org.joml.Matrix3x2f(graphics.pose()).invert();
        *///?} else {
        baseInverse = new org.joml.Matrix4f(graphics.pose().last().pose()).invert();
        //?}
        recording = true;
    }

    /** Ends the page frame; overlays and tooltips drawn afterwards are not recorded. */
    public static void endFrame() {
        if (!recording) return;
        recording = false;
        last = List.copyOf(current);
        current.clear();
        clips.clear();
        report();
    }

    private static final java.util.Set<String> reported = new java.util.HashSet<>();
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    // Logs each problem once per page, so any capture run or manual check lists every layout fault it meets.
    private static void report() {
        var page = dev.xyat.kineticcore.api.client.gui.KineticGui.currentPage();
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        String owner = page != null ? page.getClass().getName() : screen == null ? "none" : screen.getClass().getName();
        for (String problem : LayoutCheck.currentScreenProblems()) {
            if (reported.add(owner + " " + problem)) LOGGER.warn("KINETIC_LAYOUT page={} {}", owner, problem);
        }
    }

    /** What the last finished page frame drew. */
    public static List<Box> lastFrame() {
        return last;
    }

    public static void frame(GuiGraphics graphics, int x, int y, int width, int height) {
        record(graphics, Kind.FRAME, x, y, width, height, "");
    }

    public static void text(GuiGraphics graphics, int x, int y, int width, String label) {
        record(graphics, Kind.TEXT, x, y, width, 8, label);
    }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height, String label) {
        record(graphics, Kind.BUTTON, x, y, width, height, label);
    }

    /** Records the visible controls after the widgets were drawn with the page pose. */
    public static void controls(GuiGraphics graphics, List<? extends AbstractWidget> widgets) {
        if (!recording) return;
        for (AbstractWidget widget : widgets) {
            if (!widget.visible || widget.getWidth() <= 0 || widget.getHeight() <= 0) continue;
            String label = widget.getMessage() == null ? "" : widget.getMessage().getString();
            record(graphics, Kind.CONTROL, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                    widget.getClass().getSimpleName() + "[" + label + "]");
        }
    }

    /** A scissor rectangle given in the coordinates of the current pose. */
    public static void pushClip(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (!recording) return;
        int[] rect = toPage(graphics, left, top, right - left, bottom - top);
        if (rect == null) return;
        int[] parent = clips.peek();
        if (parent != null) {
            rect = new int[] {Math.max(rect[0], parent[0]), Math.max(rect[1], parent[1]),
                    Math.max(0, Math.min(rect[0] + rect[2], parent[0] + parent[2]) - Math.max(rect[0], parent[0])),
                    Math.max(0, Math.min(rect[1] + rect[3], parent[1] + parent[3]) - Math.max(rect[1], parent[1]))};
        }
        clips.push(rect);
    }

    public static void popClip() {
        if (!recording || clips.isEmpty()) return;
        clips.pop();
    }

    private static void record(GuiGraphics graphics, Kind kind, int x, int y, int width, int height, String label) {
        if (!recording || graphics == null || width <= 0 || height <= 0) return;
        int[] rect = toPage(graphics, x, y, width, height);
        if (rect == null) return;
        int[] clip = clips.peek();
        // Rows scrolled partly out of a list are cut by its clip; their visible part is not a real layout.
        if (clip != null && (rect[0] < clip[0] || rect[1] < clip[1]
                || rect[0] + rect[2] > clip[0] + clip[2] || rect[1] + rect[3] > clip[1] + clip[3])) return;
        String text = label == null ? "" : label;
        if (text.length() > 40) text = text.substring(0, 40) + "...";
        current.add(new Box(kind, rect[0], rect[1], rect[2], rect[3], text));
    }

    private static int[] toPage(GuiGraphics graphics, int x, int y, int width, int height) {
        if (baseInverse == null) return null;
        //? if >=26.1 {
        /*org.joml.Matrix3x2f m = new org.joml.Matrix3x2f(baseInverse).mul(new org.joml.Matrix3x2f(graphics.pose()));
        org.joml.Vector2f a = m.transformPosition(new org.joml.Vector2f(x, y));
        org.joml.Vector2f b = m.transformPosition(new org.joml.Vector2f(x + width, y + height));
        float ax = a.x, ay = a.y, bx = b.x, by = b.y;
        *///?} else {
        org.joml.Matrix4f m = new org.joml.Matrix4f(baseInverse).mul(graphics.pose().last().pose());
        org.joml.Vector4f a = m.transform(new org.joml.Vector4f(x, y, 0F, 1F));
        org.joml.Vector4f b = m.transform(new org.joml.Vector4f(x + width, y + height, 0F, 1F));
        float ax = a.x(), ay = a.y(), bx = b.x(), by = b.y();
        //?}
        int left = Math.round(Math.min(ax, bx));
        int top = Math.round(Math.min(ay, by));
        return new int[] {left, top, Math.round(Math.max(ax, bx)) - left, Math.round(Math.max(ay, by)) - top};
    }
}
