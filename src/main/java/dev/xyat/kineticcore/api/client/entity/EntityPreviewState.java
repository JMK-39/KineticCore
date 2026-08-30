package dev.xyat.kineticcore.api.client.entity;

public final class EntityPreviewState {
    private float angle;
    private int zoomPercent;
    private long lastNanos;
    private boolean hovered;

    public EntityPreviewState(float defaultAngle, int defaultZoomPercent) {
        this.angle = defaultAngle;
        this.zoomPercent = defaultZoomPercent;
        this.lastNanos = System.nanoTime();
    }

    public float updateRotation(
            boolean hoveredNow,
            float baseSpeed,
            int speedPercent,
            boolean clockwise,
            long frameLimitNanos
    ) {
        long now = System.nanoTime();

        if (hoveredNow && hovered) {
            long elapsedNanos = now - lastNanos;

            if (elapsedNanos > 0L && elapsedNanos <= frameLimitNanos) {
                float elapsedSeconds = elapsedNanos / 1_000_000_000f;
                float direction = clockwise ? 1f : -1f;

                angle = (
                        angle
                                + elapsedSeconds
                                * baseSpeed
                                * speedPercent
                                * direction
                                / 100f
                ) % 360f;
            }
        }

        hovered = hoveredNow;
        lastNanos = now;
        return angle;
    }

    public int getZoomPercent() {
        return zoomPercent;
    }

    public void setZoomPercent(int zoomPercent) {
        this.zoomPercent = zoomPercent;
    }
}
