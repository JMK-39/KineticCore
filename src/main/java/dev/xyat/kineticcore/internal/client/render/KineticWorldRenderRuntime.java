package dev.xyat.kineticcore.internal.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Internal Minecraft rendering bridge used by the public Kinetic world-render API. */
public final class KineticWorldRenderRuntime {
    private static final double CHUNK_CAGE_THICKNESS = 0.04D;
    private static final RenderType THICK_WORLD_LINES = ThickWorldLineType.createType();

    private KineticWorldRenderRuntime() {
    }

    /** Draws one camera-relative line box into the shared line buffer. */
    public static void lineBox(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            AABB worldBox,
            int argb
    ) {
        if (poseStack == null || camera == null || bufferSource == null || worldBox == null) {
            return;
        }
        Vec3 cameraPosition = camera.getPosition();
        AABB relativeBox = worldBox.move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        LevelRenderer.renderLineBox(poseStack, consumer, relativeBox, red, green, blue, alpha);
    }

    /** Draws one camera-relative chunk cage with internal 16-block grid lines. */
    public static void chunkCage(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            ChunkPos center,
            int radius,
            double minY,
            double maxY,
            int argb
    ) {
        if (poseStack == null || camera == null || bufferSource == null || center == null || radius < 0) {
            return;
        }

        double lowY = Math.min(minY, maxY);
        double highY = Math.max(minY, maxY);
        int minChunkX = center.x - radius;
        int maxChunkX = center.x + radius;
        int minChunkZ = center.z - radius;
        int maxChunkZ = center.z + radius;
        double minX = minChunkX * 16.0D;
        double maxX = (maxChunkX + 1) * 16.0D;
        double minZ = minChunkZ * 16.0D;
        double maxZ = (maxChunkZ + 1) * 16.0D;

        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;

        Vec3 cameraPosition = camera.getPosition();
        VertexConsumer consumer = bufferSource.getBuffer(THICK_WORLD_LINES);
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f matrix = poseStack.last().pose();

        drawSolidLine(consumer, matrix, minX, lowY, minZ, maxX, lowY, minZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, minX, highY, minZ, maxX, highY, minZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, minX, lowY, maxZ, maxX, lowY, maxZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, minX, highY, maxZ, maxX, highY, maxZ, red, green, blue, alpha);

        drawSolidLine(consumer, matrix, minX, lowY, minZ, minX, lowY, maxZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, minX, highY, minZ, minX, highY, maxZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, maxX, lowY, minZ, maxX, lowY, maxZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, maxX, highY, minZ, maxX, highY, maxZ, red, green, blue, alpha);

        drawSolidLine(consumer, matrix, minX, lowY, minZ, minX, highY, minZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, maxX, lowY, minZ, maxX, highY, minZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, minX, lowY, maxZ, minX, highY, maxZ, red, green, blue, alpha);
        drawSolidLine(consumer, matrix, maxX, lowY, maxZ, maxX, highY, maxZ, red, green, blue, alpha);

        for (double x = minX + 16.0D; x < maxX; x += 16.0D) {
            drawSolidLine(consumer, matrix, x, lowY, minZ, x, highY, minZ, red, green, blue, alpha);
            drawSolidLine(consumer, matrix, x, lowY, maxZ, x, highY, maxZ, red, green, blue, alpha);
        }
        for (double z = minZ + 16.0D; z < maxZ; z += 16.0D) {
            drawSolidLine(consumer, matrix, minX, lowY, z, minX, highY, z, red, green, blue, alpha);
            drawSolidLine(consumer, matrix, maxX, lowY, z, maxX, highY, z, red, green, blue, alpha);
        }

        poseStack.popPose();
    }

    /** Flushes all shared world-line render types after one public batch finishes. */
    public static void endLineBatch(MultiBufferSource.BufferSource bufferSource) {
        if (bufferSource != null) {
            bufferSource.endBatch(RenderType.lines());
            bufferSource.endBatch(THICK_WORLD_LINES);
        }
    }

    private static void drawSolidLine(
            VertexConsumer consumer,
            Matrix4f pose,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        double tx = x1 == x2 ? CHUNK_CAGE_THICKNESS : 0.0D;
        double ty = y1 == y2 ? CHUNK_CAGE_THICKNESS : 0.0D;
        double tz = z1 == z2 ? CHUNK_CAGE_THICKNESS : 0.0D;
        double minX = Math.min(x1, x2) - tx;
        double maxX = Math.max(x1, x2) + tx;
        double minY = Math.min(y1, y2) - ty;
        double maxY = Math.max(y1, y2) + ty;
        double minZ = Math.min(z1, z2) - tz;
        double maxZ = Math.max(z1, z2) + tz;

        addQuad(consumer, pose, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addQuad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        addQuad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addQuad(consumer, pose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        addQuad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addQuad(consumer, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void addQuad(
            VertexConsumer consumer,
            Matrix4f pose,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            double x4,
            double y4,
            double z4,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.vertex(pose, (float) x1, (float) y1, (float) z1).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, (float) x2, (float) y2, (float) z2).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, (float) x3, (float) y3, (float) z3).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, (float) x4, (float) y4, (float) z4).color(red, green, blue, alpha).endVertex();
    }

    private static final class ThickWorldLineType extends RenderType {
        private ThickWorldLineType(
                String name,
                VertexFormat format,
                VertexFormat.Mode mode,
                int bufferSize,
                boolean affectsCrumbling,
                boolean sortOnUpload,
                Runnable setupState,
                Runnable clearState
        ) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        private static RenderType createType() {
            return RenderType.create(
                    "kinetic_thick_world_lines",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(POSITION_COLOR_SHADER)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .createCompositeState(false)
            );
        }
    }
}
