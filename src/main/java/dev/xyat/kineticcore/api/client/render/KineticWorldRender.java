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
         * Draws one world-space box that stays visible behind blocks, as selection boxes in building tools do: the
         * edges in the open in full colour, the edges behind blocks faintly.
         */
        public void boxThroughBlocks(AABB worldBox, Indicator indicator) {
            if (closed || worldBox == null || indicator == null) {
                return;
            }
            int argb = indicatorColor(indicator);
            KineticWorldRenderRuntime.lineBoxSeeThrough(
                    context.poseStack(),
                    context.camera(),
                    context.bufferSource(),
                    worldBox,
                    (argb & 0x00FFFFFF) | 0x60000000
            );
            KineticWorldRenderRuntime.lineBox(
                    context.poseStack(),
                    context.camera(),
                    context.bufferSource(),
                    worldBox,
                    argb
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
     * Begins a batch of glowing quads for the supplied Kinetic level-render callback: additive, lightning-style
     * blending, both faces drawn and depth-tested without writing depth, so overlapping glows add up softly. Use it
     * for light trails, auras, shock rings and other energy shapes; points are world positions. Draw from
     * {@link KineticClientEvents.LevelRenderStage#AFTER_PARTICLES} or later (from
     * {@link KineticClientEvents.LevelRenderStage#AFTER_WEATHER} clouds and rain no longer cover it), and close the batch
     * after drawing.
     */
    public static GlowBatch beginGlowBatch(KineticClientEvents.LevelRenderContext context) {
        return new GlowBatch(Objects.requireNonNull(context, "context"), true);
    }

    /**
     * Begins a batch of tinted quads: like {@link #beginGlowBatch(KineticClientEvents.LevelRenderContext)} but with
     * ordinary alpha blending, so a colour keeps its hue over bright backgrounds (additive red turns pink against a
     * daytime sky). Draw the coloured body of an effect here, close the batch, then add its bright core in a glow batch.
     */
    public static GlowBatch beginTintBatch(KineticClientEvents.LevelRenderContext context) {
        return new GlowBatch(Objects.requireNonNull(context, "context"), false);
    }

    /**
     * Quads in world space, glowing or tinted; see {@link #beginGlowBatch(KineticClientEvents.LevelRenderContext)}
     * and {@link #beginTintBatch(KineticClientEvents.LevelRenderContext)}.
     */
    public static final class GlowBatch implements AutoCloseable {
        private final KineticClientEvents.LevelRenderContext context;
        private final boolean additive;
        private final Vec3 cameraPosition;
        private final Vec3 forward;
        private final Vec3 right;
        private final Vec3 up;
        private boolean closed;

        private GlowBatch(KineticClientEvents.LevelRenderContext context, boolean additive) {
            KineticWorldRenderRuntime.installViewCapture();
            this.context = context;
            this.additive = additive;
            this.cameraPosition = KineticWorldRenderRuntime.cameraPosition(context.camera());
            double yaw = Math.toRadians(KineticWorldRenderRuntime.cameraYaw(context.camera()));
            double pitch = Math.toRadians(KineticWorldRenderRuntime.cameraPitch(context.camera()));
            this.forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
            this.right = side.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
            this.up = right.cross(forward).normalize();
        }

        /** The camera position in world space. */
        public Vec3 cameraPosition() {
            return cameraPosition;
        }

        /** Unit vector the camera looks along. */
        public Vec3 cameraForward() {
            return forward;
        }

        /** Unit vector to the right of the view, for camera-facing shapes. */
        public Vec3 cameraRight() {
            return right;
        }

        /** Unit vector up in the view, for camera-facing shapes. */
        public Vec3 cameraUp() {
            return up;
        }

        /** One quad in a single ARGB colour; alpha scales the glow. */
        public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
            quad(a, b, c, d, argb, argb, argb, argb);
        }

        /** One quad with a colour per corner, for gradients and fading ends (alpha 0 is invisible). */
        public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argbA, int argbB, int argbC, int argbD) {
            if (closed || a == null || b == null || c == null || d == null) {
                return;
            }
            KineticWorldRenderRuntime.glowQuad(context.poseStack(), cameraPosition, additive,
                    a, b, c, d, argbA, argbB, argbC, argbD);
        }

        /** A soft camera-facing glow: the core colour at the centre fading to nothing at {@code radius}. */
        public void glow(Vec3 center, double radius, int coreArgb) {
            if (closed || center == null || radius <= 0.0D) {
                return;
            }
            int edge = coreArgb & 0x00FFFFFF;
            Vec3 previous = null;
            for (int step = 0; step <= 8; step++) {
                double angle = step * Math.PI / 4.0D;
                Vec3 point = center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
                if (previous != null) {
                    quad(center, center, previous, point, coreArgb, coreArgb, edge, edge);
                }
                previous = point;
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            KineticWorldRenderRuntime.endGlowBatch(additive, context.projectionMatrix());
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
