package dev.xyat.kineticcore.api.world.structure;

import dev.xyat.kineticcore.api.network.KineticCompression;
import dev.xyat.kineticcore.internal.world.structure.BlockStateLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A block volume captured for previews: a palette of block states, one palette index per position (index 0 is air),
 * the block entity data of positions that have it, and entities. A palette entry the reading side does not know is
 * kept as its name and reported as missing instead of failing; a preview shows it as a missing block and generation
 * places air, as vanilla does with unknown blocks in a structure template.
 *
 * <p>Immutable; build one with {@link #builder(int, int, int)}. A snapshot holds at most {@link #MAX_VOLUME}
 * positions; larger structures are sent {@link #downsample(int) downsampled}, each cell then standing for
 * {@link #scale()}³ blocks.
 */
public final class KineticBlockSnapshot {
    /** Largest number of positions one snapshot holds (for example 256 × 64 × 256). */
    public static final int MAX_VOLUME = 4_194_304;
    /** Most block entities and entities kept; the rest are dropped from the preview. */
    public static final int MAX_BLOCK_ENTITIES = 8192;
    /** Most entities kept; the rest are dropped from the preview. */
    public static final int MAX_ENTITIES = 2048;
    /** Ceiling of an encoded snapshot, compressed and decompressed. */
    public static final int MAX_ENCODED_BYTES = 32 * 1024 * 1024;

    private static final int FORMAT = 0x4B425331; // "KBS1"

    private final int sizeX, sizeY, sizeZ, scale;
    private final BlockState[] palette;
    private final String[] paletteNames;
    private final boolean[] paletteMissing;
    private final int[] blocks;
    private final Map<Integer, CompoundTag> blockEntities;
    private final List<EntityEntry> entities;
    private final int containers;
    private final int nonAir, missing;

    /** An entity at a position relative to the snapshot's minimum corner. */
    public record EntityEntry(double x, double y, double z, CompoundTag tag) {
        /** Copies the tag so the entry cannot change afterwards. */
        public EntityEntry {
            tag = Objects.requireNonNull(tag, "tag").copy();
        }
    }

    private KineticBlockSnapshot(int sizeX, int sizeY, int sizeZ, int scale, BlockState[] palette, String[] paletteNames,
                                 boolean[] paletteMissing, int[] blocks, Map<Integer, CompoundTag> blockEntities,
                                 List<EntityEntry> entities, int containers) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.scale = scale;
        this.palette = palette;
        this.paletteNames = paletteNames;
        this.paletteMissing = paletteMissing;
        this.blocks = blocks;
        this.blockEntities = Collections.unmodifiableMap(blockEntities);
        this.entities = List.copyOf(entities);
        this.containers = containers;
        int solid = 0, absent = 0;
        for (int index : blocks) {
            if (index == 0) continue;
            solid++;
            if (paletteMissing[index]) absent++;
        }
        this.nonAir = solid;
        this.missing = absent;
    }

    /**
     * Starts a snapshot of the given size, in cells.
     *
     * @throws IllegalArgumentException if a size is below 1 or the volume exceeds {@link #MAX_VOLUME}
     */
    public static Builder builder(int sizeX, int sizeY, int sizeZ) {
        return new Builder(sizeX, sizeY, sizeZ);
    }

    /** Width in cells. */
    public int sizeX() { return sizeX; }
    /** Height in cells. */
    public int sizeY() { return sizeY; }
    /** Depth in cells. */
    public int sizeZ() { return sizeZ; }
    /** Blocks per cell along each axis: 1 for a full-detail snapshot. */
    public int scale() { return scale; }
    /** Number of cells, air included. */
    public int volume() { return blocks.length; }
    /** Cells that are not air, missing blocks included. */
    public int nonAirCount() { return nonAir; }
    /** Cells whose block is unknown on this side. */
    public int missingCount() { return missing; }
    /** Block entities kept in the snapshot. */
    public int blockEntityCount() { return blockEntities.size(); }
    /** Entities kept in the snapshot. */
    public int entityCount() { return entities.size(); }
    /** Containers in the captured volume, as counted where the snapshot was made. */
    public int containerCount() { return containers; }

    /** Number of palette entries, air included at index 0. */
    public int paletteSize() { return palette.length; }

    /** Block state of a palette entry; air for a missing one. */
    public BlockState paletteState(int index) { return palette[index]; }

    /** Saved name of a palette entry, for example {@code minecraft:oak_stairs[facing=east,...]}. */
    public String paletteName(int index) { return paletteNames[index]; }

    /** Whether a palette entry names a block this side does not know. */
    public boolean isMissing(int index) { return paletteMissing[index]; }

    /** Names of the missing palette entries with the number of cells that use each. */
    public Map<String, Integer> missingNames() {
        int[] uses = new int[palette.length];
        for (int index : blocks) uses[index]++;
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 1; i < palette.length; i++) {
            if (paletteMissing[i] && uses[i] > 0) result.merge(blockId(paletteNames[i]), uses[i], Integer::sum);
        }
        return result;
    }

    /** Palette index at a cell; 0 for air or outside the volume. */
    public int index(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return 0;
        return blocks[cell(x, y, z)];
    }

    /** Block state at a cell; air outside the volume and for missing blocks. */
    public BlockState state(int x, int y, int z) {
        return palette[index(x, y, z)];
    }

    /** Block entity data at a cell, or {@code null}. The tag is shared; copy it before changing it. */
    @Nullable
    public CompoundTag blockEntity(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return null;
        return blockEntities.get(cell(x, y, z));
    }

    /** Block entity data by cell index ({@code x + sizeX * (z + sizeZ * y)}). */
    public Map<Integer, CompoundTag> blockEntities() { return blockEntities; }

    /** Entities, positioned relative to the minimum corner. */
    public List<EntityEntry> entities() { return entities; }

    private int cell(int x, int y, int z) {
        return x + sizeX * (z + sizeZ * y);
    }

    /**
     * A coarser copy where each cell covers {@code factor}³ cells of this one and shows the most common block among
     * them that is not air. Block entities and entities are left out.
     */
    public KineticBlockSnapshot downsample(int factor) {
        if (factor <= 1) return this;
        int nx = (sizeX + factor - 1) / factor, ny = (sizeY + factor - 1) / factor, nz = (sizeZ + factor - 1) / factor;
        int[] coarse = new int[nx * ny * nz];
        int[] votes = new int[palette.length];
        int[] seen = new int[factor * factor * factor];
        for (int cy = 0; cy < ny; cy++) {
            for (int cz = 0; cz < nz; cz++) {
                for (int cx = 0; cx < nx; cx++) {
                    int used = 0, best = 0, bestVotes = 0;
                    for (int y = cy * factor; y < Math.min(sizeY, (cy + 1) * factor); y++) {
                        for (int z = cz * factor; z < Math.min(sizeZ, (cz + 1) * factor); z++) {
                            for (int x = cx * factor; x < Math.min(sizeX, (cx + 1) * factor); x++) {
                                int index = blocks[cell(x, y, z)];
                                if (index == 0) continue;
                                if (votes[index]++ == 0) seen[used++] = index;
                                if (votes[index] > bestVotes) { bestVotes = votes[index]; best = index; }
                            }
                        }
                    }
                    for (int i = 0; i < used; i++) votes[seen[i]] = 0;
                    coarse[cx + nx * (cz + nz * cy)] = best;
                }
            }
        }
        return new KineticBlockSnapshot(nx, ny, nz, scale * factor, palette, paletteNames, paletteMissing, coarse,
                new HashMap<>(), List.of(), containers);
    }

    /** Smallest {@link #downsample(int)} factor that brings a volume of this size within {@code maxVolume}. */
    public static int factorFor(long sizeX, long sizeY, long sizeZ, long maxVolume) {
        int factor = 1;
        while (ceilDiv(sizeX, factor) * ceilDiv(sizeY, factor) * ceilDiv(sizeZ, factor) > maxVolume) factor++;
        return factor;
    }

    private static long ceilDiv(long value, long divisor) {
        return (value + divisor - 1) / divisor;
    }

    // ---------- encoding ----------

    /**
     * Encodes the snapshot, compressed. Palette entries are written as block state strings and the cells as runs, so
     * structures with large air or stone areas stay small.
     *
     * @throws IllegalStateException if the encoded snapshot exceeds {@link #MAX_ENCODED_BYTES}
     */
    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(1024, blocks.length / 8));
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(FORMAT);
            writeVarInt(out, sizeX);
            writeVarInt(out, sizeY);
            writeVarInt(out, sizeZ);
            writeVarInt(out, scale);
            writeVarInt(out, containers);
            writeVarInt(out, palette.length);
            for (int i = 0; i < palette.length; i++) {
                out.writeBoolean(paletteMissing[i]);
                out.writeUTF(paletteNames[i]);
            }
            int runs = 0;
            for (int i = 0; i < blocks.length; i++) if (i == 0 || blocks[i] != blocks[i - 1]) runs++;
            writeVarInt(out, runs);
            int start = 0;
            for (int i = 1; i <= blocks.length; i++) {
                if (i == blocks.length || blocks[i] != blocks[start]) {
                    writeVarInt(out, blocks[start]);
                    writeVarInt(out, i - start);
                    start = i;
                }
            }
            writeVarInt(out, blockEntities.size());
            for (Map.Entry<Integer, CompoundTag> entry : blockEntities.entrySet()) {
                writeVarInt(out, entry.getKey());
                NbtIo.write(entry.getValue(), out);
            }
            writeVarInt(out, entities.size());
            for (EntityEntry entity : entities) {
                out.writeDouble(entity.x());
                out.writeDouble(entity.y());
                out.writeDouble(entity.z());
                NbtIo.write(entity.tag(), out);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot encode block snapshot", e);
        }
        if (bytes.size() > MAX_ENCODED_BYTES) throw new IllegalStateException("Block snapshot too large: " + bytes.size());
        return KineticCompression.compressBytes(bytes.toByteArray(), MAX_ENCODED_BYTES);
    }

    /**
     * Decodes a snapshot made by {@link #encode()}. Block states this side does not know become missing entries.
     *
     * @throws IllegalArgumentException if the data is malformed or exceeds the size limits
     */
    public static KineticBlockSnapshot decode(byte[] encoded) {
        byte[] raw = KineticCompression.decompressBytes(Objects.requireNonNull(encoded, "encoded"), MAX_ENCODED_BYTES);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            if (in.readInt() != FORMAT) throw new IllegalArgumentException("Not a block snapshot");
            int sizeX = readVarInt(in), sizeY = readVarInt(in), sizeZ = readVarInt(in);
            checkSize(sizeX, sizeY, sizeZ);
            int scale = Math.max(1, readVarInt(in));
            int containers = readVarInt(in);
            int paletteSize = readVarInt(in);
            if (paletteSize < 1 || paletteSize > 1 << 20) throw new IllegalArgumentException("Bad palette size " + paletteSize);
            BlockState[] palette = new BlockState[paletteSize];
            String[] names = new String[paletteSize];
            boolean[] absent = new boolean[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                boolean missingThere = in.readBoolean();
                names[i] = in.readUTF();
                BlockState state = i == 0 ? Blocks.AIR.defaultBlockState() : missingThere ? null : BlockStateLookup.parse(names[i]);
                absent[i] = i != 0 && state == null;
                palette[i] = state == null ? Blocks.AIR.defaultBlockState() : state;
            }
            int volume = sizeX * sizeY * sizeZ;
            int[] blocks = new int[volume];
            int runs = readVarInt(in), at = 0;
            for (int r = 0; r < runs; r++) {
                int index = readVarInt(in), length = readVarInt(in);
                if (index < 0 || index >= paletteSize || length < 0 || at + length > volume) {
                    throw new IllegalArgumentException("Bad run " + index + "×" + length);
                }
                if (index != 0) Arrays.fill(blocks, at, at + length, index);
                at += length;
            }
            Map<Integer, CompoundTag> blockEntities = new HashMap<>();
            int blockEntityCount = readVarInt(in);
            if (blockEntityCount > MAX_BLOCK_ENTITIES) throw new IllegalArgumentException("Too many block entities");
            for (int i = 0; i < blockEntityCount; i++) {
                int cell = readVarInt(in);
                CompoundTag tag = NbtIo.read(in);
                if (cell >= 0 && cell < volume) blockEntities.put(cell, tag);
            }
            List<EntityEntry> entities = new ArrayList<>();
            int entityCount = readVarInt(in);
            if (entityCount > MAX_ENTITIES) throw new IllegalArgumentException("Too many entities");
            for (int i = 0; i < entityCount; i++) {
                double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
                entities.add(new EntityEntry(x, y, z, NbtIo.read(in)));
            }
            return new KineticBlockSnapshot(sizeX, sizeY, sizeZ, scale, palette, names, absent, blocks, blockEntities,
                    entities, containers);
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed block snapshot", e);
        }
    }

    /** Block id of a saved state string: the part before its properties. */
    private static String blockId(String stateName) {
        int bracket = stateName.indexOf('[');
        return bracket < 0 ? stateName : stateName.substring(0, bracket);
    }

    private static void checkSize(int sizeX, int sizeY, int sizeZ) {
        if (sizeX < 1 || sizeY < 1 || sizeZ < 1 || (long) sizeX * sizeY * sizeZ > MAX_VOLUME) {
            throw new IllegalArgumentException("Bad snapshot size " + sizeX + "×" + sizeY + "×" + sizeZ);
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
        }
        throw new IOException("VarInt too long");
    }

    /** Collects cells, block entities and entities for a {@link KineticBlockSnapshot}. Not thread-safe. */
    public static final class Builder {
        private final int sizeX, sizeY, sizeZ;
        private final int[] blocks;
        private final List<BlockState> palette = new ArrayList<>();
        private final List<String> names = new ArrayList<>();
        private final List<Boolean> absent = new ArrayList<>();
        private final Map<BlockState, Integer> stateIndex = new HashMap<>();
        private final Map<String, Integer> missingIndex = new HashMap<>();
        private final Map<Integer, CompoundTag> blockEntities = new HashMap<>();
        private final List<EntityEntry> entities = new ArrayList<>();
        private int scale = 1;
        private int containers;

        private Builder(int sizeX, int sizeY, int sizeZ) {
            checkSize(sizeX, sizeY, sizeZ);
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.blocks = new int[sizeX * sizeY * sizeZ];
            BlockState air = Blocks.AIR.defaultBlockState();
            palette.add(air);
            names.add(BlockStateLookup.serialize(air));
            absent.add(false);
            stateIndex.put(air, 0);
        }

        /** Width in cells. */
        public int sizeX() { return sizeX; }
        /** Height in cells. */
        public int sizeY() { return sizeY; }
        /** Depth in cells. */
        public int sizeZ() { return sizeZ; }

        /** Sets a cell; positions outside the volume are ignored. */
        public Builder set(int x, int y, int z, BlockState state) {
            if (!inside(x, y, z) || state == null) return this;
            Integer index = stateIndex.get(state);
            if (index == null) {
                index = state.isAir() ? 0 : add(state, BlockStateLookup.serialize(state), false);
                stateIndex.put(state, index);
            }
            blocks[x + sizeX * (z + sizeZ * y)] = index;
            return this;
        }

        /** Sets a cell to a block this side does not know, kept by its saved name. */
        public Builder setMissing(int x, int y, int z, String name) {
            if (!inside(x, y, z) || name == null || name.isBlank()) return this;
            int index = missingIndex.computeIfAbsent(name, key -> add(Blocks.AIR.defaultBlockState(), key, true));
            blocks[x + sizeX * (z + sizeZ * y)] = index;
            return this;
        }

        /** Block entity data at a cell; ignored past {@link #MAX_BLOCK_ENTITIES}. */
        public Builder blockEntity(int x, int y, int z, CompoundTag tag) {
            if (inside(x, y, z) && tag != null && blockEntities.size() < MAX_BLOCK_ENTITIES) {
                blockEntities.put(x + sizeX * (z + sizeZ * y), tag.copy());
            }
            return this;
        }

        /** An entity at a position relative to the minimum corner; ignored past {@link #MAX_ENTITIES}. */
        public Builder entity(double x, double y, double z, CompoundTag tag) {
            if (tag != null && entities.size() < MAX_ENTITIES) entities.add(new EntityEntry(x, y, z, tag));
            return this;
        }

        /** Number of containers in the captured volume. */
        public Builder containers(int count) {
            containers = Math.max(0, count);
            return this;
        }

        /** Blocks per cell, for a volume captured at reduced detail. */
        public Builder scale(int blocksPerCell) {
            scale = Math.max(1, blocksPerCell);
            return this;
        }

        /** The finished snapshot; the builder can keep being used afterwards. */
        public KineticBlockSnapshot build() {
            int size = palette.size();
            boolean[] missingFlags = new boolean[size];
            for (int i = 0; i < size; i++) missingFlags[i] = absent.get(i);
            return new KineticBlockSnapshot(sizeX, sizeY, sizeZ, scale, palette.toArray(new BlockState[0]),
                    names.toArray(new String[0]), missingFlags, blocks.clone(), new HashMap<>(blockEntities),
                    entities, containers);
        }

        private int add(BlockState state, String name, boolean missing) {
            palette.add(state);
            names.add(name);
            absent.add(missing);
            return palette.size() - 1;
        }

        private boolean inside(int x, int y, int z) {
            return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
        }
    }
}
