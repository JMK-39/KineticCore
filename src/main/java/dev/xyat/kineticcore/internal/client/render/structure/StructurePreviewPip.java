package dev.xyat.kineticcore.internal.client.render.structure;

//? if >=26.1 {
/*import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

// 26.1 draws 3D content in the GUI through picture-in-picture renderers, each into its own texture. This one draws a
// block snapshot through the preview's camera (PreviewCamera). The snapshot is meshed a slice per frame into GPU
// buffers, one per block layer, that stay on the GPU; every frame the view changes they are drawn again with the new
// camera in one call per layer, and while it does not change the last texture is reused.
public final class StructurePreviewPip extends PictureInPictureRenderer<StructurePreviewPip.State> {
    // What the GUI submits for one preview: the snapshot, the cross-section, the camera (angles in degrees, distance
    // and target in cells), the box x0,y0 - x1,y1 in GUI units and what is drawn over the snapshot (compared by
    // identity: a view makes a new overlay whenever it changes).
    public record State(KineticBlockSnapshot snapshot, int cutY, float yaw, float pitch, float distance, float targetX,
                        float targetY, float targetZ, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea,
                        PreviewOverlay overlay)
            implements PictureInPictureRenderState {
        @Override
        public float scale() {
            return 1F;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof State state && state.snapshot == snapshot && state.cutY == cutY && state.yaw == yaw
                    && state.pitch == pitch && state.distance == distance && state.targetX == targetX
                    && state.targetY == targetY && state.targetZ == targetZ && state.x0 == x0 && state.y0 == y0
                    && state.x1 == x1 && state.y1 == y1 && state.overlay == overlay;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(snapshot) * 31 + Float.hashCode(yaw) * 7 + Float.hashCode(pitch) + Float.hashCode(distance);
        }

        @Nullable
        @Override
        public ScreenRectangle bounds() {
            return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
        }
    }

    // Render-thread time one frame may spend meshing.
    private static final long FRAME_BUDGET_NANOS = 6_000_000L;
    private static final int CACHED_MESHES = 6;
    private static final ChunkSectionLayer[] ORDER = {ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT, ChunkSectionLayer.TRANSLUCENT};
    private static final Map<Key, GpuMesh> MESHES = new LinkedHashMap<>(16, 0.75F, true);
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    @Nullable private static Field renderSetup;

    @Nullable private State last;
    private final BlockModelRenderState entityModel = new BlockModelRenderState();
    private final ProjectionMatrixBuffer projection = new ProjectionMatrixBuffer("kinetic structure preview");

    public StructurePreviewPip(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    // How far the mesh for this snapshot and cross-section is built, from 0 to 1.
    public static float progress(KineticBlockSnapshot snapshot, int cutY) {
        GpuMesh mesh = MESHES.get(new Key(snapshot, Math.max(0, cutY)));
        return mesh == null ? 0F : mesh.progress();
    }

    // Frees the GPU buffers of a snapshot whose screen closed.
    public static void release(KineticBlockSnapshot snapshot) {
        for (Iterator<Map.Entry<Key, GpuMesh>> it = MESHES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Key, GpuMesh> entry = it.next();
            if (entry.getKey().snapshot() == snapshot) {
                entry.getValue().close();
                it.remove();
            }
        }
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
    protected boolean textureIsReadyToBlit(State state) {
        return state.equals(last);
    }

    @Override
    protected void renderToTexture(State state, PoseStack ignored) {
        GpuMesh mesh = mesh(state.snapshot(), state.cutY());
        if (!mesh.ready()) {
            mesh.build(System.nanoTime() + FRAME_BUDGET_NANOS);
            // Drawn again next frame, until the mesh is done.
            if (!mesh.ready()) return;
        }
        KineticBlockSnapshot snapshot = state.snapshot();
        float aspect = (state.x1() - state.x0()) / (float) Math.max(1, state.y1() - state.y0());
        Matrix4f view = PreviewCamera.view(state.yaw(), state.pitch(), state.distance(), state.targetX(), state.targetY(), state.targetZ());
        Matrix4f lens = PreviewCamera.projection(aspect, state.distance(), PreviewCamera.reach(snapshot.sizeX(), snapshot.sizeY(),
                snapshot.sizeZ(), state.targetX(), state.targetY(), state.targetZ()));
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projection.getBuffer(lens), ProjectionType.PERSPECTIVE);
        try {
            mesh.draw(view);
            drawEntityBlocks(mesh, view);
            bufferSource.endBatch();
            PreviewOverlayRenderer.draw(state.overlay(), view, bufferSource);
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
        last = state;
    }

    private void drawEntityBlocks(GpuMesh mesh, Matrix4f view) {
        if (mesh.entityBlocks.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        FeatureRenderDispatcher features = minecraft.gameRenderer.getFeatureRenderDispatcher();
        BlockModelResolver resolver = minecraft.getBlockModelResolver();
        PoseStack pose = new PoseStack();
        pose.last().pose().set(view);
        pose.last().normal().set(new Matrix3f(view));
        for (EntityBlock block : mesh.entityBlocks) {
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

    @Override
    public void close() {
        super.close();
        projection.close();
    }

    private static RenderType type(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    // The textures a render type samples (block atlas, light map); the type keeps its setup private.
    private static Map<String, RenderSetup.TextureAndSampler> textures(RenderType type) {
        try {
            if (renderSetup == null) {
                renderSetup = RenderType.class.getDeclaredField("state");
                renderSetup.setAccessible(true);
            }
            return ((RenderSetup) renderSetup.get(type)).getTextures();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read the textures of " + type, e);
        }
    }

    private static GpuMesh mesh(KineticBlockSnapshot snapshot, int cutY) {
        Key key = new Key(snapshot, Math.max(0, cutY));
        GpuMesh mesh = MESHES.get(key);
        if (mesh != null) return mesh;
        mesh = new GpuMesh(snapshot, key.cutY());
        MESHES.put(key, mesh);
        while (MESHES.size() > CACHED_MESHES) {
            Iterator<Map.Entry<Key, GpuMesh>> eldest = MESHES.entrySet().iterator();
            eldest.next().getValue().close();
            eldest.remove();
        }
        return mesh;
    }

    // Cache key: the snapshot by identity and the cross-section height.
    private record Key(KineticBlockSnapshot snapshot, int cutY) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.snapshot == snapshot && key.cutY == cutY;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(snapshot) * 31 + cutY;
        }
    }

    // A block drawn by its block entity's model (chest, bed, banner), as in a minecart.
    private record EntityBlock(int x, int y, int z, BlockState state) {
    }

    // One snapshot's mesh: built into CPU buffers a slice at a time, then uploaded once into a GPU buffer per layer.
    private static final class GpuMesh {
        private static final int ENTITY_BLOCK_CAP = 1024;
        private final KineticBlockSnapshot snapshot;
        private final int cutY;
        private final SnapshotBlockView view;
        private final ModelBlockRenderer blocks;
        private final FluidRenderer fluids;
        private final Map<ChunkSectionLayer, ByteBufferBuilder> memory = new EnumMap<>(ChunkSectionLayer.class);
        private final Map<ChunkSectionLayer, BufferBuilder> builders = new EnumMap<>(ChunkSectionLayer.class);
        private final Map<ChunkSectionLayer, GpuBuffer> buffers = new EnumMap<>(ChunkSectionLayer.class);
        private final Map<ChunkSectionLayer, Integer> indexCounts = new EnumMap<>(ChunkSectionLayer.class);
        private final List<EntityBlock> entityBlocks = new ArrayList<>();
        private final PoseStack pose = new PoseStack();
        private final BlockPos.MutableBlockPos cursorPos = new BlockPos.MutableBlockPos();
        private int cursor, meshed;
        private boolean finished, closed;

        GpuMesh(KineticBlockSnapshot snapshot, int cutY) {
            this.snapshot = snapshot;
            this.cutY = cutY;
            Minecraft minecraft = Minecraft.getInstance();
            Biome plains = minecraft.level == null ? null
                    : minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME).getValue(Biomes.PLAINS);
            view = new SnapshotBlockView(snapshot, plains, cutY);
            blocks = new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
            fluids = new FluidRenderer(minecraft.getModelManager().getFluidStateModelSet());
        }

        boolean ready() {
            return finished && !closed;
        }

        float progress() {
            if (finished) return 1F;
            int total = snapshot.nonAirCount();
            return total == 0 ? 0F : Math.min(0.99F, meshed / (float) total);
        }

        void build(long deadlineNanos) {
            if (finished || closed) return;
            var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
            int sizeX = snapshot.sizeX(), layer = sizeX * snapshot.sizeZ(), volume = snapshot.volume();
            while (cursor < volume) {
                if ((cursor & 63) == 0 && System.nanoTime() > deadlineNanos) return;
                int cell = cursor++;
                int y = cell / layer, z = (cell % layer) / sizeX, x = cell % sizeX;
                if (y >= cutY || snapshot.index(x, y, z) == 0) continue;
                meshed++;
                BlockPos pos = cursorPos.set(x, y, z);
                BlockState state = view.getBlockState(pos);
                FluidState fluid = state.getFluidState();
                int sectionX = x & ~15, sectionY = y & ~15, sectionZ = z & ~15;
                try {
                    if (!fluid.isEmpty()) {
                        fluids.tesselate(view, pos, fluidLayer -> new Offset(builder(fluidLayer), sectionX, sectionY, sectionZ), state, fluid);
                    }
                    if (state.getRenderShape() == RenderShape.MODEL) {
                        int[] quads = {0};
                        blocks.tesselateBlock((qx, qy, qz, quad, instance) -> {
                            quads[0]++;
                            pose.pushPose();
                            pose.translate(qx, qy, qz);
                            builder(quad.materialInfo().layer()).putBakedQuad(pose.last(), quad, instance);
                            pose.popPose();
                        }, x, y, z, view, pos, state, models.get(state), state.getSeed(pos));
                        // Chests, beds, signs and the like have a model with no faces; their block entity draws them.
                        if (quads[0] == 0 && state.hasBlockEntity() && entityBlocks.size() < ENTITY_BLOCK_CAP) {
                            entityBlocks.add(new EntityBlock(x, y, z, state));
                        }
                    } else if (state.hasBlockEntity() && entityBlocks.size() < ENTITY_BLOCK_CAP) {
                        entityBlocks.add(new EntityBlock(x, y, z, state));
                    }
                } catch (RuntimeException ignored) {
                    // A block whose model needs a real level is left out of the preview.
                }
            }
            upload();
            finished = true;
        }

        private BufferBuilder builder(ChunkSectionLayer layer) {
            BufferBuilder builder = builders.get(layer);
            if (builder != null) return builder;
            RenderType type = type(layer);
            ByteBufferBuilder bytes = new ByteBufferBuilder(type.bufferSize());
            memory.put(layer, bytes);
            builder = new BufferBuilder(bytes, type.mode(), type.format());
            builders.put(layer, builder);
            return builder;
        }

        private void upload() {
            for (Map.Entry<ChunkSectionLayer, BufferBuilder> entry : builders.entrySet()) {
                try (MeshData data = entry.getValue().build()) {
                    if (data == null) continue;
                    ChunkSectionLayer layer = entry.getKey();
                    buffers.put(layer, RenderSystem.getDevice().createBuffer(() -> "Kinetic structure preview " + layer,
                            GpuBuffer.USAGE_VERTEX, data.vertexBuffer()));
                    indexCounts.put(layer, data.drawState().indexCount());
                }
            }
            builders.clear();
            memory.values().forEach(ByteBufferBuilder::close);
            memory.clear();
        }

        // Draws every layer into the texture being rendered, with the projection already set to the camera's lens.
        void draw(Matrix4f view) {
            int most = 0;
            for (int count : indexCounts.values()) most = Math.max(most, count);
            if (most == 0) return;
            RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer indices = quads.getBuffer(most);
            GpuBufferSlice transform = RenderSystem.getDynamicUniforms()
                    .writeTransform(view, new Vector4f(1F, 1F, 1F, 1F), new Vector3f(), new Matrix4f());
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Kinetic structure preview",
                    RenderSystem.outputColorTextureOverride, OptionalInt.empty(), RenderSystem.outputDepthTextureOverride, OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transform);
                pass.setIndexBuffer(indices, quads.type());
                for (ChunkSectionLayer layer : ORDER) {
                    GpuBuffer buffer = buffers.get(layer);
                    if (buffer == null) continue;
                    RenderType type = type(layer);
                    pass.setPipeline(type.pipeline());
                    for (Map.Entry<String, RenderSetup.TextureAndSampler> texture : textures(type).entrySet()) {
                        pass.bindTexture(texture.getKey(), texture.getValue().textureView(), texture.getValue().sampler());
                    }
                    pass.setVertexBuffer(0, buffer);
                    pass.drawIndexed(0, 0, indexCounts.get(layer), 1);
                }
            }
        }

        void close() {
            if (closed) return;
            closed = true;
            buffers.values().forEach(GpuBuffer::close);
            buffers.clear();
            builders.clear();
            memory.values().forEach(ByteBufferBuilder::close);
            memory.clear();
        }
    }

    // Moves the liquid renderer's vertices, which it writes relative to the block's 16-block section, into the mesh.
    private record Offset(VertexConsumer target, float dx, float dy, float dz) implements VertexConsumer {
        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            target.addVertex(x + dx, y + dy, z + dz, color, u, v, overlay, light, nx, ny, nz);
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
