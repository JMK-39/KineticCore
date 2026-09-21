package dev.xyat.kineticcore.api.monitoring;

import dev.xyat.kineticcore.internal.monitoring.ClientPerformanceRuntime;

/**
 * Reads client frame-performance data without exposing the Minecraft client singleton.
 */
public final class KineticClientPerformance {
    private KineticClientPerformance() {
    }

    /** Returns a defensive snapshot of the current FPS and frame-timing ring buffer. */
    public static FrameSnapshot snapshot() {
        return ClientPerformanceRuntime.snapshot();
    }

    /**
     * Immutable client frame-performance snapshot.
     *
     * @param currentFps current client FPS
     * @param logStart frame-timing ring-buffer start index
     * @param logEnd frame-timing ring-buffer end index
     * @param frameNanos frame durations in nanoseconds
     */
    public record FrameSnapshot(int currentFps, int logStart, int logEnd, long[] frameNanos) {
        /** Creates a defensive frame snapshot. */
        public FrameSnapshot {
            frameNanos = frameNanos == null ? new long[0] : frameNanos.clone();
        }

        /** Returns a defensive copy of the captured frame-duration buffer. */
        @Override
        public long[] frameNanos() {
            return frameNanos.clone();
        }
    }
}
