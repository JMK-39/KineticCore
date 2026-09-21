package dev.xyat.kineticcore.api.client.render;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.internal.client.render.KineticWorldRenderRuntime;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

/**
 * Shared world-space line rendering helpers with Kinetic-owned visual semantics.
 */
public final class KineticWorldRender {
    /** Semantic world-line indicators whose actual colors remain owned by KineticCore. */
    public enum Indicator {
        INFO,
        SUCCESS,
        WARNING,
        DANGER,
        MUTED
    }

    private KineticWorldRender() {
    }

    /**
     * Begins one line-render batch for the supplied Kinetic level-render callback.
     * The returned batch must be closed after all boxes for the callback have been submitted.
     */
    public static LineBatch beginLineBatch(KineticClientEvents.LevelRenderContext context) {
        return new LineBatch(Objects.requireNonNull(context, "context"));
    }

    /**
     * Batched world-line renderer that converts world-space boxes to camera-relative geometry and flushes once on close.
     */
    public static final class LineBatch implements AutoCloseable {
        private final KineticClientEvents.LevelRenderContext context;
        private boolean closed;

        private LineBatch(KineticClientEvents.LevelRenderContext context) {
            this.context = context;
        }

        /** Draws one world-space axis-aligned box using a standard semantic Kinetic indicator. */
        public void box(AABB worldBox, Indicator indicator) {
            if (closed || worldBox == null || indicator == null) {
                return;
            }
            KineticWorldRenderRuntime.lineBox(
                    context.poseStack(),
                    context.camera(),
                    context.bufferSource(),
                    worldBox,
                    indicatorColor(indicator)
            );
        }

        /**
         * Draws a chunk-aligned three-dimensional cage with internal chunk grid lines.
         * Radius is measured in chunks around {@code center}; height bounds are world-space Y coordinates.
         */
        public void chunkCage(ChunkPos center, int radius, double minY, double maxY, Indicator indicator) {
            if (closed || center == null || indicator == null || radius < 0) {
                return;
            }
            KineticWorldRenderRuntime.chunkCage(
                    context.poseStack(),
                    context.camera(),
                    context.bufferSource(),
                    center,
                    radius,
                    minY,
                    maxY,
                    indicatorColor(indicator)
            );
        }

        /** Flushes the shared line buffer for this batch. Repeated calls are ignored. */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            KineticWorldRenderRuntime.endLineBatch(context.bufferSource());
        }
    }

    private static int indicatorColor(Indicator indicator) {
        return switch (indicator) {
            case INFO -> 0xFF268CFF;
            case SUCCESS -> 0xFF33FF40;
            case WARNING -> 0xFFFFAA00;
            case DANGER -> 0xFFFF2626;
            case MUTED -> 0xFF777777;
        };
    }
}
