package dev.xyat.kineticcore.internal.client.render.structure;

//? if <26.1 {
import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;

/**
 * Read-only world over a block snapshot, for meshing a preview: every cell is fully lit, colours are those of the
 * given biome, faces are shaded as in the overworld, and blocks unknown on this side show as red stained glass.
 * Cells at or above {@code cutY} read as air, which opens a cross-section.
 */
final class SnapshotBlockView implements BlockAndTintGetter {
    static final BlockState MISSING = Blocks.RED_STAINED_GLASS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final KineticBlockSnapshot snapshot;
    @Nullable private final Biome biome;
    private final int cutY;
    private LevelLightEngine light;

    SnapshotBlockView(KineticBlockSnapshot snapshot, @Nullable Biome biome, int cutY) {
        this.snapshot = snapshot;
        this.biome = biome;
        this.cutY = cutY;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.getY() >= cutY) return AIR;
        int index = snapshot.index(pos.getX(), pos.getY(), pos.getZ());
        if (index == 0) return AIR;
        return snapshot.isMissing(index) ? MISSING : snapshot.paletteState(index);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getHeight() {
        return snapshot.sizeY() + 2;
    }

    @Override
    public int getMinBuildHeight() {
        return -1;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        if (!shade) return 1.0F;
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return 15;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        // Brightness is answered above; this engine only exists for callers that ask for one.
        if (light == null) {
            light = new LevelLightEngine(new LightChunkGetter() {
                @Nullable
                @Override
                public LightChunk getChunkForLighting(int x, int z) {
                    return null;
                }

                @Override
                public BlockGetter getLevel() {
                    return SnapshotBlockView.this;
                }
            }, false, false);
        }
        return light;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return biome == null ? 0xFFFFFF : resolver.getColor(biome, pos.getX(), pos.getZ());
    }
}
//?} else {
/*import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;

// The 26.1 form of the read-only snapshot world: full light, overworld face shading, the given biome's colours,
// unknown blocks as red stained glass and nothing at or above cutY.
final class SnapshotBlockView implements net.minecraft.client.renderer.block.BlockAndTintGetter {
    static final BlockState MISSING = Blocks.RED_STAINED_GLASS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final KineticBlockSnapshot snapshot;
    @Nullable private final Biome biome;
    private final int cutY;

    SnapshotBlockView(KineticBlockSnapshot snapshot, @Nullable Biome biome, int cutY) {
        this.snapshot = snapshot;
        this.biome = biome;
        this.cutY = cutY;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.getY() >= cutY) return AIR;
        int index = snapshot.index(pos.getX(), pos.getY(), pos.getZ());
        if (index == 0) return AIR;
        return snapshot.isMissing(index) ? MISSING : snapshot.paletteState(index);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getHeight() {
        return snapshot.sizeY() + 2;
    }

    @Override
    public int getMinY() {
        return -1;
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return LevelLightEngine.EMPTY;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return biome == null ? -1 : resolver.getColor(biome, pos.getX(), pos.getZ());
    }
}
*///?}
