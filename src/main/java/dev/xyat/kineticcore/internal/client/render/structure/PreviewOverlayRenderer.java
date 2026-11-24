package dev.xyat.kineticcore.internal.client.render.structure;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws a {@link PreviewOverlay} through the preview's camera, after the snapshot itself: the entities, then the
 * boxes, first faintly through anything in front of them and then in full where nothing is.
 */
final class PreviewOverlayRenderer {
    /** Share of a box's opacity left for its edges behind blocks. */
    private static final float BEHIND_ALPHA = 0.4F;
    //? if >=26.1 {
    /*private static final com.mojang.blaze3d.pipeline.RenderPipeline SEE_THROUGH_PIPELINE = com.mojang.blaze3d.pipeline.RenderPipeline
            .builder(net.minecraft.client.renderer.RenderPipelines.LINES_SNIPPET)
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("kineticcore", "pipeline/preview_lines_see_through"))
            .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.ALWAYS_PASS, false))
            .build();
    // Drawn into the preview's own texture, so it keeps the main output target.
    private static final net.minecraft.client.renderer.rendertype.RenderType SEE_THROUGH = net.minecraft.client.renderer.rendertype.RenderType.create(
            "kinetic_preview_lines_see_through",
            net.minecraft.client.renderer.rendertype.RenderSetup.builder(SEE_THROUGH_PIPELINE).createRenderSetup());
    *///?}

    private PreviewOverlayRenderer() {
    }

    private static PoseStack camera(Matrix4f view) {
        PoseStack pose = new PoseStack();
        pose.last().pose().set(view);
        pose.last().normal().set(new Matrix3f(view));
        return pose;
    }

    private static int behind(int argb) {
        return (argb & 0x00FFFFFF) | (Math.round((argb >>> 24) * BEHIND_ALPHA) << 24);
    }

    //? if >=26.1 {
    /*/^* Draws into the texture being rendered, with the projection already set to the camera's lens. ^/
    static void draw(PreviewOverlay overlay, Matrix4f view, net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
        if (overlay == null || overlay.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = camera(view);
        if (!overlay.figures().isEmpty()) {
            minecraft.gameRenderer.getLighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_3D);
            var features = minecraft.gameRenderer.getFeatureRenderDispatcher();
            var dispatcher = minecraft.getEntityRenderDispatcher();
            var camera = new net.minecraft.client.renderer.state.level.CameraRenderState();
            for (PreviewOverlay.Figure figure : overlay.figures()) {
                try {
                    net.minecraft.client.renderer.entity.state.EntityRenderState state =
                            dispatcher.getRenderer(figure.entity()).createRenderState(figure.entity(), 1.0F);
                    state.shadowPieces.clear();
                    state.outlineColor = 0;
                    state.nameTag = null;
                    state.displayFireAnimation = false;
                    state.lightCoords = 15728880;
                    dispatcher.submit(state, camera, figure.x(), figure.y(), figure.z(), pose, features.getSubmitNodeStorage());
                } catch (RuntimeException ignored) {
                    // A modded renderer that needs a real level draws nothing here.
                }
            }
            features.renderAllFeatures();
        }
        if (overlay.boxes().isEmpty()) return;
        float width = minecraft.getWindow().getAppropriateLineWidth();
        var lines = net.minecraft.client.renderer.rendertype.RenderTypes.lines();
        // Grid lines first and a pixel wide, so the outlines stand out over them.
        for (PreviewOverlay.Box box : overlay.boxes()) if (box.fine()) shape(pose, buffers.getBuffer(lines), box, box.argb(), 1.0F);
        buffers.endBatch(lines);
        for (PreviewOverlay.Box box : overlay.boxes()) {
            if (box.seeThrough()) shape(pose, buffers.getBuffer(SEE_THROUGH), box, behind(box.argb()), width);
        }
        buffers.endBatch(SEE_THROUGH);
        for (PreviewOverlay.Box box : overlay.boxes()) if (!box.fine()) shape(pose, buffers.getBuffer(lines), box, box.argb(), width);
        buffers.endBatch(lines);
    }

    private static void shape(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, PreviewOverlay.Box box, int argb, float width) {
        net.minecraft.client.renderer.ShapeRenderer.renderShape(pose, consumer, net.minecraft.world.phys.shapes.Shapes.box(
                box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()), 0D, 0D, 0D, argb, width);
    }
    *///?} else {
    /**
     * Draws into the preview's viewport; the caller has drawn the snapshot there and its depth is still in place.
     * Batched drawing takes the lens and the camera from the render system, so both are set here and put back after.
     */
    static void draw(net.minecraft.client.gui.GuiGraphics graphics, PreviewOverlay overlay, Matrix4f view, Matrix4f projection) {
        if (overlay == null || overlay.isEmpty()) return;
        PoseStack pose = camera(view);
        com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection, com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
        var modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        //? if >=1.21 {
        /*modelView.pushMatrix();
        modelView.identity();
        *///?} else {
        modelView.pushPose();
        modelView.setIdentity();
        //?}
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        var buffers = graphics.bufferSource();
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        try {
            if (!overlay.figures().isEmpty()) {
                dispatcher.setRenderShadow(false);
                com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
                for (PreviewOverlay.Figure figure : overlay.figures()) {
                    try {
                        dispatcher.render(figure.entity(), figure.x(), figure.y(), figure.z(), figure.entity().getYRot(), 1.0F,
                                pose, buffers, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                    } catch (RuntimeException ignored) {
                        // A modded renderer that needs a real level draws nothing here.
                    }
                }
                buffers.endBatch();
            }
            if (!overlay.boxes().isEmpty()) {
                var window = Minecraft.getInstance().getWindow();
                var lines = net.minecraft.client.renderer.RenderType.lines();
                // Grid lines first and a pixel wide, so the outlines stand out over them.
                com.mojang.blaze3d.systems.RenderSystem.lineWidth(1.0F);
                for (PreviewOverlay.Box box : overlay.boxes()) if (box.fine()) lineBox(pose, buffers.getBuffer(lines), box, box.argb());
                buffers.endBatch(lines);
                com.mojang.blaze3d.systems.RenderSystem.lineWidth(Math.max(2.5F, window.getWidth() / 1920.0F * 2.5F));
                var seeThrough = dev.xyat.kineticcore.internal.client.render.KineticWorldRenderRuntime.seeThroughLines();
                for (PreviewOverlay.Box box : overlay.boxes()) {
                    if (box.seeThrough()) lineBox(pose, buffers.getBuffer(seeThrough), box, behind(box.argb()));
                }
                buffers.endBatch(seeThrough);
                for (PreviewOverlay.Box box : overlay.boxes()) if (!box.fine()) lineBox(pose, buffers.getBuffer(lines), box, box.argb());
                buffers.endBatch(lines);
                com.mojang.blaze3d.systems.RenderSystem.lineWidth(1.0F);
            }
        } finally {
            dispatcher.setRenderShadow(true);
            //? if >=1.21 {
            /*modelView.popMatrix();
            *///?} else {
            modelView.popPose();
            //?}
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void lineBox(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, PreviewOverlay.Box box, int argb) {
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose, consumer, box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ(), ((argb >>> 16) & 0xFF) / 255F, ((argb >>> 8) & 0xFF) / 255F,
                (argb & 0xFF) / 255F, (argb >>> 24) / 255F);
    }
    //?}
}
