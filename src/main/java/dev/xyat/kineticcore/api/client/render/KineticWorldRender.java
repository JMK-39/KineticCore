package dev.xyat.kineticcore.api.client.render;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.render.KineticWorldRenderRuntime;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Objects;

/**
 * Shared world-space rendering helpers with Kinetic-owned visual semantics: line boxes and chunk cages, and a
 * screen-space overlay for labels that follow world positions.
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

    /**
     * Begins a screen-space overlay for the supplied Kinetic level-render callback, for labels that follow world
     * positions such as name tags or damage numbers. Project a world point with {@link ScreenOverlay#project(Vec3)}
     * and draw at the result through {@link ScreenOverlay#graphics()}. Drawing uses GUI-scaled coordinates, ignores
     * depth and stays on top of the world. The overlay must be closed after drawing.
     */
    public static ScreenOverlay beginScreenOverlay(KineticClientEvents.LevelRenderContext context) {
        return new ScreenOverlay(Objects.requireNonNull(context, "context"));
    }

    /**
     * Screen-space canvas over the rendered world for one level-render callback; closing it flushes the drawing and
     * restores the world render state.
     */
    public static final class ScreenOverlay implements AutoCloseable {
        private final Matrix4f view;
        private final Matrix4f projection;
        private final Vec3 cameraPosition;
        private final int width;
        private final int height;
        private final KineticGraphics graphics;
        private boolean closed;

        private ScreenOverlay(KineticClientEvents.LevelRenderContext context) {
            this.view = new Matrix4f(context.poseStack().last().pose());
            this.projection = new Matrix4f(context.projectionMatrix());
            this.cameraPosition = context.camera().getPosition();
            this.width = KineticClientRuntime.guiScaledWidth();
            this.height = KineticClientRuntime.guiScaledHeight();
            this.graphics = KineticWorldRenderRuntime.beginScreenOverlay(width, height);
        }

        /** GUI-scaled width of the overlay canvas. */
        public int width() {
            return width;
        }

        /** GUI-scaled height of the overlay canvas. */
        public int height() {
            return height;
        }

        /**
         * Projects a world point to GUI-scaled screen coordinates.
         *
         * @param worldPosition absolute world position
         * @return the screen position, which may lie outside the canvas, or {@code null} when the point is behind the
         *   camera
         */
        public @Nullable Vec2 project(Vec3 worldPosition) {
            if (worldPosition == null) {
                return null;
            }
            Vector4f position = new Vector4f(
                    (float) (worldPosition.x - cameraPosition.x),
                    (float) (worldPosition.y - cameraPosition.y),
                    (float) (worldPosition.z - cameraPosition.z),
                    1.0F
            );
            view.transform(position);
            projection.transform(position);
            if (position.w() <= 0.0F) {
                return null;
            }
            float screenX = (position.x() / position.w() + 1.0F) * 0.5F * width;
            float screenY = (1.0F - position.y() / position.w()) * 0.5F * height;
            return new Vec2(screenX, screenY);
        }

        /** Graphics surface drawing in GUI-scaled screen coordinates on top of the world. */
        public KineticGraphics graphics() {
            return graphics;
        }

        /** Flushes the drawing and restores the world render state. Repeated calls are ignored. */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            KineticWorldRenderRuntime.endScreenOverlay(graphics);
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
