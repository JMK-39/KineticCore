package dev.xyat.kineticcore.internal.client.gui.screen;

import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;

/**
 * 宿主页面坐标与屏幕坐标之间的换算。画布模式把固定逻辑画布等比缩放并居中到安全区；原生模式与屏幕坐标一一对应。
 * Conversion between a host's page coordinates and screen coordinates. Canvas mode fits a fixed logical canvas into
 * the safe area; native mode is the identity transform and treats the whole screen as the page.
 */
public final class KineticCanvasTransform {
    private final boolean nativeCoordinates;
    private int width;
    private int height;
    private float scale = 1f;
    private int x;
    private int y;
    private KineticLayout.SafeArea safeArea = KineticLayout.SafeArea.of(1, 1, 0);
    private KineticLayout.Metrics metrics = KineticLayout.measure(1, 1, 640, 360);

    private KineticCanvasTransform(boolean nativeCoordinates, int width, int height) {
        this.nativeCoordinates = nativeCoordinates;
        this.width = width;
        this.height = height;
    }

    /** Creates a scaled logical canvas reporting the given size until the first {@link #fit}. */
    public static KineticCanvasTransform canvas(int initialWidth, int initialHeight) {
        return new KineticCanvasTransform(false, initialWidth, initialHeight);
    }

    /** Creates the identity transform used by native-coordinate screens. */
    public static KineticCanvasTransform nativeCoordinates() {
        return new KineticCanvasTransform(true, 1, 1);
    }

    /** Fits a {@code designWidth x designHeight} canvas into the safe area of the current screen size. */
    public void fit(int screenWidth, int screenHeight, int designWidth, int designHeight, int safeMargin) {
        safeArea = KineticLayout.SafeArea.of(screenWidth, screenHeight, safeMargin);
        metrics = KineticLayout.measure(safeArea.width(), safeArea.height(), designWidth, designHeight);
        scale = Math.max(0.0001f, metrics.fitScale());
        width = designWidth;
        height = designHeight;
        x = safeArea.left() + Math.round((safeArea.width() - width * scale) / 2f);
        y = safeArea.top() + Math.round((safeArea.height() - height * scale) / 2f);
    }

    /** Returns whether page coordinates are screen coordinates. */
    public boolean isNative() {
        return nativeCoordinates;
    }

    /** Logical canvas width. */
    public int width() {
        return width;
    }

    /** Logical canvas height. */
    public int height() {
        return height;
    }

    /** Scale from page coordinates to screen coordinates. */
    public float scale() {
        return scale;
    }

    /** Screen-space X origin of the canvas. */
    public int x() {
        return x;
    }

    /** Screen-space Y origin of the canvas. */
    public int y() {
        return y;
    }

    /** Safe area measured by the last {@link #fit}. */
    public KineticLayout.SafeArea safeArea() {
        return safeArea;
    }

    /** Layout metrics measured by the last {@link #fit}. */
    public KineticLayout.Metrics metrics() {
        return metrics;
    }

    /** Converts a screen X coordinate to page coordinates. */
    public double toVirtualX(double screenX) {
        return (screenX - x) / scale;
    }

    /** Converts a screen Y coordinate to page coordinates. */
    public double toVirtualY(double screenY) {
        return (screenY - y) / scale;
    }

    /** Converts a page X coordinate to the screen pixel containing it. */
    public int toScreenX(double virtualX) {
        return x + (int) Math.floor(virtualX * scale);
    }

    /** Converts a page Y coordinate to the screen pixel containing it. */
    public int toScreenY(double virtualY) {
        return y + (int) Math.floor(virtualY * scale);
    }

    /** Converts an exclusive page right edge to the covering screen edge. */
    public int toScreenRight(double virtualX) {
        return x + (int) Math.ceil(virtualX * scale);
    }

    /** Converts an exclusive page bottom edge to the covering screen edge. */
    public int toScreenBottom(double virtualY) {
        return y + (int) Math.ceil(virtualY * scale);
    }

    /** Returns whether a screen point lies inside the canvas; native coordinates have no canvas bounds. */
    public boolean contains(double screenX, double screenY) {
        if (nativeCoordinates) return true;
        double virtualX = toVirtualX(screenX);
        double virtualY = toVirtualY(screenY);
        return virtualX >= 0 && virtualX < width && virtualY >= 0 && virtualY < height;
    }

    /** Screen X at which an overlay menu anchored at a page X opens. */
    public int menuX(double virtualX) {
        return nativeCoordinates ? (int) Math.round(virtualX) : toScreenX(virtualX);
    }

    /** Screen Y at which an overlay menu anchored at a page Y opens. */
    public int menuY(double virtualY) {
        return nativeCoordinates ? (int) Math.round(virtualY) : toScreenY(virtualY);
    }

    /** Screen width of an overlay menu that is {@code virtualWidth} page units wide. */
    public int menuWidth(int virtualWidth) {
        return Math.max(1, nativeCoordinates ? virtualWidth : Math.round(virtualWidth * scale));
    }
}
