package dev.xyat.kineticcore.api.client.layout;

public record ResponsiveLayout(
        Level level,
        Aspect aspect,
        int availableWidth,
        int availableHeight,
        float designWidth,
        float designHeight,
        float fitScale,
        float aspectRatio
) {
    public enum Level {
        LARGE,
        NORMAL,
        SMALL,
        COMPACT
    }

    public enum Aspect {
        PORTRAIT,
        LANDSCAPE,
        ULTRAWIDE
    }

    public static ResponsiveLayout evaluate(
            int availableWidth,
            int availableHeight,
            float designWidth,
            float designHeight
    ) {
        int safeWidth =
                Math.max(1, availableWidth);

        int safeHeight =
                Math.max(1, availableHeight);

        float safeDesignWidth =
                Math.max(1f, designWidth);

        float safeDesignHeight =
                Math.max(1f, designHeight);

        float widthScale =
                safeWidth / safeDesignWidth;

        float heightScale =
                safeHeight / safeDesignHeight;

        float fitScale =
                Math.min(
                        widthScale,
                        heightScale
                );

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

        float aspectRatio =
                safeWidth / (float) safeHeight;

        Aspect aspect;

        if (aspectRatio < 0.9f) {
            aspect = Aspect.PORTRAIT;
        } else if (aspectRatio >= 2.0f) {
            aspect = Aspect.ULTRAWIDE;
        } else {
            aspect = Aspect.LANDSCAPE;
        }

        return new ResponsiveLayout(
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

    public boolean isPortrait() {
        return aspect == Aspect.PORTRAIT;
    }

    public boolean isUltrawide() {
        return aspect == Aspect.ULTRAWIDE;
    }

    public boolean isCompact() {
        return level == Level.COMPACT;
    }
}
