package dev.xyat.kineticcore.internal.client.render.structure;

//? if >=26.1 {
/*import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// 26.1 draws 3D content in the GUI through picture-in-picture renderers, each into its own texture. This one draws a
// block snapshot. The snapshot is tessellated once into a list of quads (and liquid vertices) that is replayed with
// the current angle whenever the view changes; while it does not change, the last texture is reused.
public final class StructurePreviewPip extends PictureInPictureRenderer<StructurePreviewPip.State> {
    // What the GUI submits for one preview: the snapshot, the cross-section, the angle, the size of a block in GUI
    // units (scale), the depth squeeze and the pan, inside the box x0,y0 - x1,y1.
    public record State(KineticBlockSnapshot snapshot, int cutY, float yaw, float pitch, float scale, float depth,
                        float panX, float panY, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea)
            implements PictureInPictureRenderState {
        @Override
        public boolean equals(Object other) {
            return other instanceof State state && state.snapshot == snapshot && state.cutY == cutY && state.yaw == yaw
                    && state.pitch == pitch && state.scale == scale && state.depth == depth && state.panX == panX
                    && state.panY == panY && state.x0 == x0 && state.y0 == y0 && state.x1 == x1 && state.y1 == y1;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(snapshot) * 31 + Float.hashCode(yaw) * 7 + Float.hashCode(pitch);
        }

        @Nullable
        @Override
        public ScreenRectangle bounds() {
            return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
        }
    }

    private record CachedQuad(float x, float y, float z, BakedQuad quad, int[] colors, int[] lights, int overlay,
                              ChunkSectionLayer layer) {
    }

    private record CachedVertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
                                float nx, float ny, float nz, ChunkSectionLayer layer) {
    }

    // A block drawn by its block entity's model (chest, bed, banner), as in a minecart.
    private record EntityBlock(int x, int y, int z, BlockState state) {
    }

    private static final int ENTITY_BLOCK_CAP = 1024;
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    @Nullable private State last;
    @Nullable private KineticBlockSnapshot meshedSnapshot;
    private int meshedCut = Integer.MIN_VALUE;
    private final List<CachedQuad> quads = new ArrayList<>();
    private final List<CachedVertex> liquid = new ArrayList<>();
    private final List<EntityBlock> entityBlocks = new ArrayList<>();
    private final BlockModelRenderState entityModel = new BlockModelRenderState();

    public StructurePreviewPip(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "kinetic structure preview";
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected boolean textureIsReadyToBlit(State state) {
        return state.equals(last);
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        mesh(state.snapshot(), state.cutY());
        KineticBlockSnapshot snapshot = state.snapshot();
        // The texture's Y axis points down; turning half way round Z puts up on top as in the other versions.
        pose.translate(state.panX() / state.scale(), state.panY() / state.scale(), 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.scale(1.0F, 1.0F, state.depth());
        pose.mulPose(Axis.XP.rotationDegrees(-state.pitch()));
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw() + 180.0F));
        pose.translate(-snapshot.sizeX() / 2.0F, -snapshot.sizeY() / 2.0F, -snapshot.sizeZ() / 2.0F);
        QuadInstance instance = new QuadInstance();
        for (CachedQuad cached : quads) {
            for (int vertex = 0; vertex < 4; vertex++) {
                instance.setColor(vertex, cached.colors()[vertex]);
                instance.setLightCoords(vertex, cached.lights()[vertex]);
            }
            instance.setOverlayCoords(cached.overlay());
            pose.pushPose();
            pose.translate(cached.x(), cached.y(), cached.z());
            bufferSource.getBuffer(type(cached.layer())).putBakedQuad(pose.last(), cached.quad(), instance);
            pose.popPose();
        }
        for (CachedVertex vertex : liquid) {
            bufferSource.getBuffer(type(vertex.layer()))
                    .addVertex(pose.last(), vertex.x(), vertex.y(), vertex.z())
                    .setColor(vertex.color())
                    .setUv(vertex.u(), vertex.v())
                    .setOverlay(vertex.overlay())
                    .setLight(vertex.light())
                    .setNormal(pose.last(), vertex.nx(), vertex.ny(), vertex.nz());
        }
        if (!entityBlocks.isEmpty()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
            FeatureRenderDispatcher features = minecraft.gameRenderer.getFeatureRenderDispatcher();
            BlockModelResolver resolver = minecraft.getBlockModelResolver();
            for (EntityBlock block : entityBlocks) {
                try {
                    resolver.update(entityModel, block.state(), DISPLAY_CONTEXT);
                    if (entityModel.isEmpty()) continue;
                    pose.pushPose();
                    pose.translate(block.x(), block.y(), block.z());
                    entityModel.submit(pose, features.getSubmitNodeStorage(), 15728880, OverlayTexture.NO_OVERLAY, 0);
                    pose.popPose();
                } catch (RuntimeException ignored) {
                    // A modded model that needs a real level draws nothing here.
                }
            }
            features.renderAllFeatures();
        }
        last = state;
    }

    private static RenderType type(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    private void mesh(KineticBlockSnapshot snapshot, int cutY) {
        if (snapshot == meshedSnapshot && cutY == meshedCut) return;
        quads.clear();
        liquid.clear();
        entityBlocks.clear();
        meshedSnapshot = snapshot;
        meshedCut = cutY;
        Minecraft minecraft = Minecraft.getInstance();
        Biome plains = minecraft.level == null ? null
                : minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME).getValue(Biomes.PLAINS);
        SnapshotBlockView view = new SnapshotBlockView(snapshot, plains, cutY);
        var models = minecraft.getModelManager().getBlockStateModelSet();
        ModelBlockRenderer blocks = new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
        FluidRenderer fluids = new FluidRenderer(minecraft.getModelManager().getFluidStateModelSet());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < Math.min(snapshot.sizeY(), cutY); y++) {
            for (int z = 0; z < snapshot.sizeZ(); z++) {
                for (int x = 0; x < snapshot.sizeX(); x++) {
                    if (snapshot.index(x, y, z) == 0) continue;
                    pos.set(x, y, z);
                    BlockState state = view.getBlockState(pos);
                    FluidState fluid = state.getFluidState();
                    int sectionX = x & ~15, sectionY = y & ~15, sectionZ = z & ~15;
                    try {
                        if (!fluid.isEmpty()) {
                            fluids.tesselate(view, pos, layer -> new LiquidRecorder(layer, sectionX, sectionY, sectionZ), state, fluid);
                        }
                        if (state.getRenderShape() == RenderShape.MODEL) {
                            blocks.tesselateBlock((qx, qy, qz, quad, quadInstance) -> quads.add(copy(qx, qy, qz, quad, quadInstance)),
                                    x, y, z, view, pos, state, models.get(state), state.getSeed(pos));
                        } else if (state.hasBlockEntity() && entityBlocks.size() < ENTITY_BLOCK_CAP) {
                            entityBlocks.add(new EntityBlock(x, y, z, state));
                        }
                    } catch (RuntimeException ignored) {
                        // A block whose model needs a real level is left out of the preview.
                    }
                }
            }
        }
    }

    private static CachedQuad copy(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
        int[] colors = new int[4], lights = new int[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            colors[vertex] = instance.getColor(vertex);
            lights[vertex] = instance.getLightCoords(vertex);
        }
        return new CachedQuad(x, y, z, quad, colors, lights, instance.overlayCoords(), quad.materialInfo().layer());
    }

    // Records the liquid renderer's vertices, which it writes relative to the block's 16-block section.
    private final class LiquidRecorder implements VertexConsumer {
        private final ChunkSectionLayer layer;
        private final float dx, dy, dz;

        LiquidRecorder(ChunkSectionLayer layer, int dx, int dy, int dz) {
            this.layer = layer;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            liquid.add(new CachedVertex(x + dx, y + dy, z + dz, color, u, v, overlay, light, nx, ny, nz, layer));
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setColor(int color) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    }
}
*///?}
