package dev.xyat.kineticcore.internal.client.render.structure;

//? if <26.1 {
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;
//? if >=1.21 {
/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.model.data.ModelData;
//?}

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GPU mesh of a block snapshot for the GUI preview: one vertex buffer per block render layer. The mesh is built on
 * the render thread a slice at a time ({@link #build(long)}), so a large structure never stalls a frame, and is then
 * drawn with one call per layer however the preview is turned or zoomed. Blocks drawn by block entity renderers
 * (chests, beds, shulker boxes, banners) are drawn as their item models each frame, up to a cap.
 */
final class StructureMesh {
    /** Layers in draw order: translucent last so it blends over what is behind it. */
    private static final List<RenderType> ORDER = List.of(RenderType.solid(), RenderType.cutoutMipped(),
            RenderType.cutout(), RenderType.translucent(), RenderType.tripwire());
    private static final int ENTITY_BLOCK_CAP = 1024;
    //? if <1.21 {
    // 1.20.1 builders own memory that is never freed, so one set is kept and reused by one mesh at a time: a mesh that
    // starts building takes them over, and one that was still building with them starts again when next drawn.
    private static final Map<RenderType, BufferBuilder> SHARED_BUILDERS = new IdentityHashMap<>();
    @Nullable private static StructureMesh sharedOwner;
    //?}

    private final KineticBlockSnapshot snapshot;
    private final int cutY;
    private final SnapshotBlockView view;
    private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
    //? if >=1.21 {
    /*private final Map<RenderType, ByteBufferBuilder> memory = new IdentityHashMap<>();
    *///?}
    private final Map<RenderType, VertexBuffer> buffers = new LinkedHashMap<>();
    private final List<int[]> entityBlocks = new ArrayList<>();
    private final PoseStack pose = new PoseStack();
    private final RandomSource random = RandomSource.create();
    private final BlockPos.MutableBlockPos cursorPos = new BlockPos.MutableBlockPos();
    private int cursor;
    private int meshed;
    private boolean finished;
    private boolean released;

    StructureMesh(KineticBlockSnapshot snapshot, int cutY) {
        this.snapshot = snapshot;
        this.cutY = Math.max(0, cutY);
        this.view = new SnapshotBlockView(snapshot, plainsBiome(), this.cutY);
    }

    KineticBlockSnapshot snapshot() { return snapshot; }
    int cutY() { return cutY; }
    boolean ready() { return finished && !released; }

    /** Share of the cells meshed so far, from 0 to 1. */
    float progress() {
        if (finished) return 1F;
        int total = snapshot.nonAirCount();
        return total == 0 ? 0F : Math.min(0.99F, meshed / (float) total);
    }

    /**
     * Meshes cells until {@code deadlineNanos} ({@link System#nanoTime()}) passes, then uploads the mesh once every
     * cell is done. Returns whether the mesh is finished.
     */
    boolean build(long deadlineNanos) {
        if (finished || released) return finished;
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        int sizeX = snapshot.sizeX(), sizeZ = snapshot.sizeZ(), layer = sizeX * sizeZ;
        int volume = snapshot.volume();
        while (cursor < volume) {
            if ((cursor & 63) == 0 && System.nanoTime() > deadlineNanos) return false;
            int cell = cursor++;
            int y = cell / layer, z = (cell % layer) / sizeX, x = cell % sizeX;
            if (y >= cutY || snapshot.index(x, y, z) == 0) continue;
            meshed++;
            meshCell(dispatcher, x, y, z);
        }
        upload();
        finished = true;
        return true;
    }

    private void meshCell(BlockRenderDispatcher dispatcher, int x, int y, int z) {
        BlockPos pos = cursorPos.set(x, y, z);
        BlockState state = view.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluid);
            // The liquid renderer places vertices by the position within its 16-block section.
            dispatcher.renderLiquid(pos, view, new OffsetConsumer(builder(layer), x & ~15, y & ~15, z & ~15), state, fluid);
        }
        RenderShape shape = state.getRenderShape();
        if (shape == RenderShape.ENTITYBLOCK_ANIMATED) {
            if (entityBlocks.size() < ENTITY_BLOCK_CAP) entityBlocks.add(new int[]{x, y, z});
            return;
        }
        if (shape != RenderShape.MODEL) return;
        BakedModel model = dispatcher.getBlockModel(state);
        long seed = state.getSeed(pos);
        random.setSeed(seed);
        for (RenderType layer : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            random.setSeed(seed);
            pose.pushPose();
            pose.translate(x, y, z);
            dispatcher.renderBatched(state, pos, view, pose, builder(layer), true, random, ModelData.EMPTY, layer);
            pose.popPose();
        }
    }

    private BufferBuilder builder(RenderType layer) {
        RenderType key = ORDER.contains(layer) ? layer : RenderType.cutout();
        BufferBuilder builder = builders.get(key);
        if (builder != null) return builder;
        //? if >=1.21 {
        /*ByteBufferBuilder bytes = new ByteBufferBuilder(key.bufferSize());
        memory.put(key, bytes);
        builder = new BufferBuilder(bytes, key.mode(), key.format());
        *///?} else {
        if (sharedOwner != this) claimSharedBuilders();
        builder = SHARED_BUILDERS.computeIfAbsent(key, type -> new BufferBuilder(type.bufferSize()));
        endStale(builder);
        builder.begin(key.mode(), key.format());
        //?}
        builders.put(key, builder);
        return builder;
    }

    //? if <1.21 {
    private void claimSharedBuilders() {
        StructureMesh previous = sharedOwner;
        sharedOwner = this;
        if (previous == null || previous.finished || previous.released) return;
        previous.builders.clear();
        previous.entityBlocks.clear();
        previous.cursor = 0;
        previous.meshed = 0;
    }

    /** Ends a build left open by another mesh; discard alone keeps the builder building. */
    private static void endStale(BufferBuilder builder) {
        if (!builder.building()) return;
        BufferBuilder.RenderedBuffer stale = builder.endOrDiscardIfEmpty();
        if (stale != null) stale.release();
    }
    //?}

    private void upload() {
        for (Map.Entry<RenderType, BufferBuilder> entry : builders.entrySet()) {
            //? if >=1.21 {
            /*MeshData data = entry.getValue().build();
            if (data != null) {
                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                buffer.bind();
                buffer.upload(data);
                buffers.put(entry.getKey(), buffer);
            }
            *///?} else {
            BufferBuilder.RenderedBuffer rendered = entry.getValue().endOrDiscardIfEmpty();
            if (rendered != null) {
                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                buffer.bind();
                buffer.upload(rendered);
                buffers.put(entry.getKey(), buffer);
            }
            //?}
        }
        VertexBuffer.unbind();
        builders.clear();
        //? if >=1.21 {
        /*memory.values().forEach(ByteBufferBuilder::close);
        memory.clear();
        *///?}
    }

    /**
     * Draws the mesh. {@code model} maps snapshot cells (one unit per cell) into the GUI pose space; the caller has
     * cleared depth inside its clip and flushed pending GUI geometry.
     */
    void draw(GuiGraphics graphics, Matrix4f model) {
        if (!ready()) return;
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(model);
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        float fogStart = RenderSystem.getShaderFogStart();
        FogRenderer.setupNoFog();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            for (RenderType layer : ORDER) {
                VertexBuffer buffer = buffers.get(layer);
                if (buffer == null) continue;
                layer.setupRenderState();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    // The world renderer leaves the last chunk's offset in this uniform.
                    if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
                    buffer.bind();
                    buffer.drawWithShader(modelView, projection, shader);
                }
                layer.clearRenderState();
            }
            VertexBuffer.unbind();
            drawEntityBlocks(graphics, model);
        } finally {
            RenderSystem.setShaderFogStart(fogStart);
        }
    }

    private void drawEntityBlocks(GuiGraphics graphics, Matrix4f model) {
        if (entityBlocks.isEmpty()) return;
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        PoseStack stack = new PoseStack();
        stack.last().pose().set(model);
        // Lit item models need unit-length normals under the preview's rotation and scale.
        float unit = new org.joml.Vector3f(model.m00(), model.m01(), model.m02()).length();
        stack.last().normal().set(new org.joml.Matrix3f(model).normal()).scale(unit);
        Lighting.setupFor3DItems();
        var buffers = graphics.bufferSource();
        for (int[] cell : entityBlocks) {
            BlockState state = view.getBlockState(cursorPos.set(cell[0], cell[1], cell[2]));
            stack.pushPose();
            stack.translate(cell[0], cell[1], cell[2]);
            try {
                dispatcher.renderSingleBlock(state, stack, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (RuntimeException ignored) {
                // A modded renderer that needs a real level draws nothing here.
            }
            stack.popPose();
        }
        buffers.endBatch();
    }

    /** Frees the GPU buffers; the mesh draws nothing afterwards. */
    void release() {
        if (released) return;
        released = true;
        buffers.values().forEach(VertexBuffer::close);
        buffers.clear();
        //? if >=1.21 {
        /*memory.values().forEach(ByteBufferBuilder::close);
        memory.clear();
        *///?} else {
        builders.values().forEach(StructureMesh::endStale);
        //?}
        builders.clear();
    }

    @Nullable
    private static Biome plainsBiome() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return level.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.PLAINS);
    }

    /** Moves every vertex by a fixed offset; used for liquids, which are meshed relative to their section. */
    private static final class OffsetConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float dx, dy, dz;

        OffsetConsumer(VertexConsumer delegate, int dx, int dy, int dz) {
            this.delegate = delegate;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        //? if >=1.21 {
        /*@Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x + dx, y + dy, z + dz);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
        *///?} else {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x + dx, y + dy, z + dz);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
        //?}
    }
}
//?}
