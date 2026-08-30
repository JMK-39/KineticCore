package dev.xyat.kineticcore.api.client.layout;

public record GuiSafeArea(
        int left,
        int top,
        int right,
        int bottom
) {
    public static GuiSafeArea of(
            int screenWidth,
            int screenHeight,
            int margin
    ) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);

        int maxHorizontalMargin =
                Math.max(0, (safeWidth - 1) / 2);

        int maxVerticalMargin =
                Math.max(0, (safeHeight - 1) / 2);

        int horizontalMargin =
                Math.min(
                        Math.max(0, margin),
                        maxHorizontalMargin
                );

        int verticalMargin =
                Math.min(
                        Math.max(0, margin),
                        maxVerticalMargin
                );

        return new GuiSafeArea(
                horizontalMargin,
                verticalMargin,
                safeWidth - horizontalMargin,
                safeHeight - verticalMargin
        );
    }

    public int width() {
        return Math.max(1, right - left);
    }

    public int height() {
        return Math.max(1, bottom - top);
    }

    public boolean contains(
            double x,
            double y
    ) {
        return x >= left
                && x < right
                && y >= top
                && y < bottom;
    }
}
