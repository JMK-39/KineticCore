package dev.xyat.kineticcore.feature.spawnegg.util;

public final class SpawnImpactOffset {
    private static final double CLEARANCE = 0.001D;

    private SpawnImpactOffset() {
    }

    public static Position outsideFace(
            int blockX,
            int blockY,
            int blockZ,
            int stepX,
            int stepY,
            int stepZ,
            double entityWidth,
            double entityHeight
    ) {
        double halfWidth = Math.max(0.0D, entityWidth) * 0.5D;
        double horizontalClearance = Math.max(0.5D, halfWidth + CLEARANCE);
        double x = blockX + 0.5D;
        double y = blockY;
        double z = blockZ + 0.5D;

        if (stepX > 0) {
            x = blockX + 1.0D + horizontalClearance;
        } else if (stepX < 0) {
            x = blockX - horizontalClearance;
        } else if (stepZ > 0) {
            z = blockZ + 1.0D + horizontalClearance;
        } else if (stepZ < 0) {
            z = blockZ - horizontalClearance;
        } else if (stepY > 0) {
            y = blockY + 1.0D + CLEARANCE;
        } else if (stepY < 0) {
            y = blockY - Math.max(0.0D, entityHeight) - CLEARANCE;
        }

        return new Position(x, y, z);
    }

    public record Position(double x, double y, double z) {
    }
}
