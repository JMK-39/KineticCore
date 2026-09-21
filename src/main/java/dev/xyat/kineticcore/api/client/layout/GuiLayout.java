package dev.xyat.kineticcore.api.client.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout primitives for Kinetic virtual-canvas interfaces.
 */
public final class GuiLayout {
    /** Hard maximum virtual canvas width, independent of display resolution. */
    public static final int MAX_CANVAS_WIDTH = 640;
    /** Hard maximum virtual canvas height, independent of display resolution. */
    public static final int MAX_CANVAS_HEIGHT = 360;
    /** Density bucket derived from how much the target area must scale a design canvas. */
    public enum Level {
        LARGE,
        NORMAL,
        SMALL,
        COMPACT
    }

    /** Coarse aspect-ratio category for responsive layout decisions. */
    public enum Aspect {
        PORTRAIT,
        LANDSCAPE,
        ULTRAWIDE
    }

    /** Immutable rectangle in the caller's current coordinate space. */
    public record Rect(int x, int y, int width, int height) {
        /** Normalizes rectangle dimensions so width and height never become negative. */
        public Rect {
            width = Math.max(0, width);
            height = Math.max(0, height);
        }

        /** Returns the exclusive right edge. */
        public int right() {
            return (int) Math.min(Integer.MAX_VALUE, (long) x + width);
        }

        /** Returns the exclusive bottom edge. */
        public int bottom() {
            return (int) Math.min(Integer.MAX_VALUE, (long) y + height);
        }

        /** Returns whether the point lies inside this rectangle using exclusive right/bottom edges. */
        public boolean contains(double mouseX, double mouseY) {
            // Compare against the true (long) exclusive edges even when the
            // public int edge accessor has to saturate to Integer.MAX_VALUE.
            return mouseX >= x && mouseX < (long) x + width
                    && mouseY >= y && mouseY < (long) y + height;
        }

        /** Returns this rectangle inset equally from both horizontal and vertical sides; dimensions never become negative. */
        public Rect inset(int horizontal, int vertical) {
            int xInset = Math.min(Math.max(0, horizontal), width / 2);
            int yInset = Math.min(Math.max(0, vertical), height / 2);
            return new Rect(
                    saturate((long) x + xInset),
                    saturate((long) y + yInset),
                    Math.max(0, width - xInset * 2),
                    Math.max(0, height - yInset * 2)
            );
        }

        /** Returns this rectangle translated by the supplied delta without changing its size. */
        public Rect move(int deltaX, int deltaY) {
            return new Rect(saturate((long) x + deltaX), saturate((long) y + deltaY), width, height);
        }
    }

    /** Keeps GUI coordinates from wrapping around to the opposite side of the screen. */
    private static int saturate(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    /** Screen-space bounds available after applying a clamped outer margin. */
    public record SafeArea(int left, int top, int right, int bottom) {
        /** Creates a safe area from screen dimensions while clamping the margin so at least one pixel remains. */
        public static SafeArea of(int screenWidth, int screenHeight, int margin) {
            int safeWidth = Math.max(1, screenWidth);
            int safeHeight = Math.max(1, screenHeight);
            int maxHorizontalMargin = Math.max(0, (safeWidth - 1) / 2);
            int maxVerticalMargin = Math.max(0, (safeHeight - 1) / 2);
            int horizontalMargin = Math.min(Math.max(0, margin), maxHorizontalMargin);
            int verticalMargin = Math.min(Math.max(0, margin), maxVerticalMargin);
            return new SafeArea(
                    horizontalMargin,
                    verticalMargin,
                    safeWidth - horizontalMargin,
                    safeHeight - verticalMargin
            );
        }

        /** Returns the safe-area width, never less than one pixel. */
        public int width() {
            return Math.max(1, right - left);
        }

        /** Returns the safe-area height, never less than one pixel. */
        public int height() {
            return Math.max(1, bottom - top);
        }

        /** Returns the safe area as a {@link Rect}. */
        public Rect rect() {
            return new Rect(left, top, width(), height());
        }

        /** Returns whether the point lies inside this safe area using exclusive right/bottom edges. */
        public boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    /** Measured scale and aspect information for fitting a design canvas into available space. */
    public record Metrics(
            Level level,
            Aspect aspect,
            int availableWidth,
            int availableHeight,
            float designWidth,
            float designHeight,
            float fitScale,
            float aspectRatio
    ) {
        /** Returns whether the measured layout is in portrait form. */
        public boolean isPortrait() {
            return aspect == Aspect.PORTRAIT;
        }

        /** Returns whether the measured layout is ultrawide. */
        public boolean isUltrawide() {
            return aspect == Aspect.ULTRAWIDE;
        }

        /** Returns whether the measured layout should use compact spacing. */
        public boolean isCompact() {
            return level == Level.COMPACT;
        }
    }

    /** Pair of rectangles produced by one split operation. */
    public record Split(Rect first, Rect second) {
    }

    private GuiLayout() {
    }

    /** Measures a virtual design within the hard 640x360 maximum without expanding smaller dimensions. */
    public static Metrics measure(
            int availableWidth,
            int availableHeight,
            float designWidth,
            float designHeight
    ) {
        float safeDesignWidth = Float.isFinite(designWidth) ? Math.max(1f, Math.min(MAX_CANVAS_WIDTH, designWidth)) : 1f;
        float safeDesignHeight = Float.isFinite(designHeight) ? Math.max(1f, Math.min(MAX_CANVAS_HEIGHT, designHeight)) : 1f;
        return measureResolved(availableWidth, availableHeight, safeDesignWidth, safeDesignHeight);
    }

    /** Measures an arbitrary logical canvas without applying the standard 640x360 design cap. */
    public static Metrics measureCanvas(
            int availableWidth,
            int availableHeight,
            float designWidth,
            float designHeight
    ) {
        float safeDesignWidth = Float.isFinite(designWidth) ? Math.max(1f, designWidth) : 1f;
        float safeDesignHeight = Float.isFinite(designHeight) ? Math.max(1f, designHeight) : 1f;
        return measureResolved(availableWidth, availableHeight, safeDesignWidth, safeDesignHeight);
    }

    private static Metrics measureResolved(
            int availableWidth,
            int availableHeight,
            float safeDesignWidth,
            float safeDesignHeight
    ) {
        int safeWidth = Math.max(1, availableWidth);
        int safeHeight = Math.max(1, availableHeight);
        float fitScale = Math.min(safeWidth / safeDesignWidth, safeHeight / safeDesignHeight);

        Level level;
        if (fitScale >= 1.25f) {
            level = Level.LARGE;
        } else if (fitScale >= 0.95f) {
            level = Level.NORMAL;
        } else if (fitScale >= 0.72f) {
            level = Level.SMALL;
        } else {
            level = Level.COMPACT;
        }

        float aspectRatio = safeWidth / (float) safeHeight;
        Aspect aspect;
        if (aspectRatio < 0.9f) {
            aspect = Aspect.PORTRAIT;
        } else if (aspectRatio >= 2.0f) {
            aspect = Aspect.ULTRAWIDE;
        } else {
            aspect = Aspect.LANDSCAPE;
        }

        return new Metrics(
                level,
                aspect,
                safeWidth,
                safeHeight,
                safeDesignWidth,
                safeDesignHeight,
                fitScale,
                aspectRatio
        );
    }

    /** Divides an area into equal-height rows separated by a non-negative gap. */
    public static List<Rect> rows(Rect area, int count, int gap) {
        return divide(area, Math.max(0, count), Math.max(0, gap), false);
    }

    /** Divides an area into equal-width columns separated by a non-negative gap. */
    public static List<Rect> columns(Rect area, int count, int gap) {
        return divide(area, Math.max(0, count), Math.max(0, gap), true);
    }

    /** Splits an area into left/right rectangles using the requested first width and gap. */
    public static Split splitHorizontal(Rect area, int firstWidth, int gap) {
        int safeGap = Math.min(Math.max(0, gap), area.width());
        int available = area.width() - safeGap;
        int leftWidth = Math.max(0, Math.min(firstWidth, available));
        return new Split(
                new Rect(area.x(), area.y(), leftWidth, area.height()),
                new Rect(saturate((long) area.x() + leftWidth + safeGap), area.y(), available - leftWidth, area.height())
        );
    }

    /** Splits an area into top/bottom rectangles using the requested first height and gap. */
    public static Split splitVertical(Rect area, int firstHeight, int gap) {
        int safeGap = Math.min(Math.max(0, gap), area.height());
        int available = area.height() - safeGap;
        int topHeight = Math.max(0, Math.min(firstHeight, available));
        return new Split(
                new Rect(area.x(), area.y(), area.width(), topHeight),
                new Rect(area.x(), saturate((long) area.y() + topHeight + safeGap), area.width(), available - topHeight)
        );
    }

    /** Divides an area into columns proportional to non-negative weights. */
    public static List<Rect> weightedColumns(Rect area, int gap, int... weights) {
        return weighted(area, gap, true, weights);
    }

    /** Divides an area into rows proportional to non-negative weights. */
    public static List<Rect> weightedRows(Rect area, int gap, int... weights) {
        return weighted(area, gap, false, weights);
    }

    private static List<Rect> divide(Rect area, int count, int gap, boolean horizontal) {
        if (count <= 0) return List.of();
        int total = horizontal ? area.width() : area.height();
        gap = count == 1 ? 0 : Math.min(gap, total / (count - 1));
        int usable = total - gap * (count - 1);
        int base = usable / count;
        int remainder = usable % count;
        List<Rect> result = new ArrayList<>(count);
        long cursor = horizontal ? area.x() : area.y();

        for (int index = 0; index < count; index++) {
            int size = base + (index < remainder ? 1 : 0);
            if (horizontal) {
                result.add(new Rect(saturate(cursor), area.y(), size, area.height()));
            } else {
                result.add(new Rect(area.x(), saturate(cursor), area.width(), size));
            }
            cursor += size + gap;
        }
        return List.copyOf(result);
    }

    private static List<Rect> weighted(Rect area, int gap, boolean horizontal, int... weights) {
        if (weights == null || weights.length == 0) return List.of();
        int safeGap = Math.max(0, gap);
        long totalWeight = 0L;
        for (int weight : weights) totalWeight += Math.max(0, weight);
        if (totalWeight <= 0) return divide(area, weights.length, safeGap, horizontal);

        int total = horizontal ? area.width() : area.height();
        safeGap = weights.length == 1 ? 0 : Math.min(safeGap, total / (weights.length - 1));
        int usable = total - safeGap * (weights.length - 1);
        List<Rect> result = new ArrayList<>(weights.length);
        long cursor = horizontal ? area.x() : area.y();
        int consumed = 0;
        // Only a positive-weight panel can receive rounding leftovers. In particular,
        // a trailing zero-weight panel must remain empty on very small canvases.
        int finalPositiveIndex = weights.length - 1;
        while (finalPositiveIndex > 0 && weights[finalPositiveIndex] <= 0) {
            finalPositiveIndex--;
        }

        for (int index = 0; index < weights.length; index++) {
            int size;
            if (weights[index] <= 0) {
                size = 0;
            } else if (index == finalPositiveIndex) {
                size = Math.max(0, usable - consumed);
            } else {
                size = (int) Math.round(usable * (Math.max(0, weights[index]) / (double) totalWeight));
                size = Math.max(0, Math.min(size, usable - consumed));
            }
            if (horizontal) {
                result.add(new Rect(saturate(cursor), area.y(), size, area.height()));
            } else {
                result.add(new Rect(area.x(), saturate(cursor), area.width(), size));
            }
            cursor += size + safeGap;
            consumed += size;
        }
        return List.copyOf(result);
    }
}
