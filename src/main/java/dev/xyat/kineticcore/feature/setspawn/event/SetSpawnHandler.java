package dev.xyat.kineticcore.feature.setspawn.event;

import dev.xyat.kineticcore.feature.setspawn.config.SetSpawnConfig;
import dev.xyat.kineticcore.feature.setspawn.data.SetSpawnData;

import com.mojang.authlib.GameProfile;
import dev.xyat.kineticcore.KineticCore;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class SetSpawnHandler {

    public static boolean isInternalModifying = false;

    private static final int SAFE_SEARCH_RADIUS = 96;
    private static final int PLATFORM_RADIUS = 1;
    private static final int FORCED_AIR_HEIGHT = 3;
    private static final String LOG_PREFIX = "[SetSpawn]";
    private static final int STRUCTURE_SEARCH_STEP_CHUNKS = 32;
    private static final long FINAL_SAFE_SEARCH_TIMEOUT_MS = 900L;
    private static final long STRUCTURE_SAFE_SEARCH_TIMEOUT_MS = 1800L;
    private static final long BIOME_SAFE_SEARCH_TIMEOUT_MS = 1500L;
    private static final long DIMENSION_SAFE_SEARCH_TIMEOUT_MS = 900L;

    private static final Map<UUID, Pair<ServerLevel, BlockPos>> FRESH_LOGIN_PLACEMENTS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Pair<ServerLevel, BlockPos>> PENDING_RESPAWN_PLACEMENT = new ThreadLocal<>();
    private static final ThreadLocal<Pair<ServerLevel, BlockPos>> APPLIED_RESPAWN_PLACEMENT = new ThreadLocal<>();

    public static boolean hasPlayerDataFile(MinecraftServer server, UUID uuid) {
        try {
            Path playerFile = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid + ".dat");
            boolean exists = Files.exists(playerFile);
            debug("Checking player data: uuid=" + uuid + ", exists=" + exists + ", path=" + playerFile);
            return exists;
        } catch (Exception e) {
            debug("Error checking player data, assuming exists for safety: uuid=" + uuid + ", error=" + e.getMessage());
            return true;
        }
    }

    public static Optional<ServerPlayer> createFreshLoginPlayer(MinecraftServer server, GameProfile profile) {
        UUID uuid = profile.getId();
        debug("createFreshLoginPlayer init: name=" + profile.getName() + ", uuid=" + uuid);

        if (uuid == null) {
            debug("createFreshLoginPlayer abort: uuid is null");
            return Optional.empty();
        }
        if (!SetSpawnConfig.enableCustomSpawn) {
            debug("createFreshLoginPlayer abort: setspawn.enable=false");
            return Optional.empty();
        }
        boolean hasPlayerFile = hasPlayerDataFile(server, uuid);
        boolean hasSingleplayerTag = hasSingleplayerPlayerTag(server);
        if (hasPlayerFile) {
            debug("createFreshLoginPlayer abort: player data exists (hasPlayerFile=true)");
            return Optional.empty();
        }
        if (hasSingleplayerTag) {
            SetSpawnData data = SetSpawnData.get(server.overworld());
            ensureWorldSpawnState(server, data);
            if (!data.isSetSpawnWorldEnabled()) {
                debug("createFreshLoginPlayer abort: legacy singleplayer data detected, custom spawn disabled");
                return Optional.empty();
            }
            debug("createFreshLoginPlayer continue: singleplayer data detected, but custom spawn is enabled");
        }

        Optional<Pair<ServerLevel, BlockPos>> spawn = getOrCreateGlobalSpawn(server);
        if (spawn.isEmpty()) {
            debug("createFreshLoginPlayer abort: getOrCreateGlobalSpawn returned empty");
            return Optional.empty();
        }

        debug("Created custom dimension player: dim=" + spawn.get().getFirst().dimension().location() + ", pos=" + posToString(spawn.get().getSecond()));
        FRESH_LOGIN_PLACEMENTS.put(uuid, spawn.get());
        return Optional.of(new ServerPlayer(server, spawn.get().getFirst(), profile));
    }

    public static boolean isFreshLoginPlayer(ServerPlayer player) {
        return FRESH_LOGIN_PLACEMENTS.containsKey(player.getUUID());
    }

    public static void applyCachedOverworldSpawnBeforeVanillaSpawnChunks(MinecraftServer server) {
        debug("prepareLevels HEAD triggered");
        if (!SetSpawnConfig.enableCustomSpawn) {
            debug("prepareLevels abort: setspawn.enable=false");
            return;
        }

        SetSpawnData data = SetSpawnData.get(server.overworld());
        debug("prepareLevels initial data: " + describeData(data) + ", hasAnyPlayerData=" + hasAnyPlayerData(server) + ", hasExistingWorldFootprint=" + hasExistingWorldFootprint(server));
        ensureWorldSpawnState(server, data);
        debug("prepareLevels validated data: " + describeData(data));

        if (!data.isSetSpawnWorldEnabled()) {
            debug("prepareLevels abort: custom spawn disabled for this world");
            return;
        }
        if (!data.isSpawnCalculated()) {
            debug("prepareLevels abort: spawn not calculated yet");
            return;
        }
        if (!Level.OVERWORLD.location().toString().equals(data.getSpawnDim())) {
            debug("prepareLevels abort: saved dimension is not overworld, dim=" + data.getSpawnDim());
            return;
        }

        BlockPos pos = new BlockPos(data.getSpawnX(), data.getSpawnY(), data.getSpawnZ());
        debug("prepareLevels applying cached overworld spawn: pos=" + posToString(pos));
        setWorldSpawn(server.overworld(), pos);
    }

    public static Optional<ServerLevel> ensureFreshPlayerPlacement(MinecraftServer server, ServerPlayer player) {
        Pair<ServerLevel, BlockPos> spawn = FRESH_LOGIN_PLACEMENTS.get(player.getUUID());
        if (spawn == null) {
            return Optional.empty();
        }

        ServerLevel level = spawn.getFirst();
        BlockPos requestedPos = spawn.getSecond();
        BlockPos pos = ensureFinalSafeSpawn(server, level, requestedPos, false);
        FRESH_LOGIN_PLACEMENTS.put(player.getUUID(), Pair.of(level, pos));
        placePlayerExactly(player, pos);

        return Optional.of(level);
    }

    public static void finishFreshPlayerPlacement(ServerPlayer player) {
        FRESH_LOGIN_PLACEMENTS.remove(player.getUUID());
    }

    public static void markPendingRespawnPlacement(Pair<ServerLevel, BlockPos> spawn) {
        PENDING_RESPAWN_PLACEMENT.set(spawn);
    }

    public static void clearPendingRespawnPlacement() {
        PENDING_RESPAWN_PLACEMENT.remove();
        APPLIED_RESPAWN_PLACEMENT.remove();
    }

    public static void applyPendingRespawnPlacement(ServerPlayer player) {
        Pair<ServerLevel, BlockPos> spawn = PENDING_RESPAWN_PLACEMENT.get();
        PENDING_RESPAWN_PLACEMENT.remove();

        if (spawn == null) {
            APPLIED_RESPAWN_PLACEMENT.remove();
            return;
        }

        ServerLevel level = spawn.getFirst();
        BlockPos requestedPos = spawn.getSecond();
        BlockPos pos = ensureFinalSafeSpawn(level.getServer(), level, requestedPos, false);
        APPLIED_RESPAWN_PLACEMENT.set(Pair.of(level, pos));
        placePlayerExactly(player, pos);
    }

    public static void syncAppliedRespawnPlacement(ServerPlayer player) {
        Pair<ServerLevel, BlockPos> spawn = APPLIED_RESPAWN_PLACEMENT.get();
        APPLIED_RESPAWN_PLACEMENT.remove();

        if (spawn == null) {
            return;
        }

        ServerLevel level = spawn.getFirst();
        BlockPos pos = spawn.getSecond();
        boolean dimMismatch = player.level() != level;

        if (dimMismatch) {
            return;
        }

        syncPlayerExactPosition(player, pos);
    }

    public static Optional<BlockPos> getSavedSpawnPosForLevel(MinecraftServer server, ServerLevel level) {
        if (!SetSpawnConfig.enableCustomSpawn) {
            return Optional.empty();
        }

        SetSpawnData data = SetSpawnData.get(server.overworld());
        ensureWorldSpawnState(server, data);
        if (!data.isSetSpawnWorldEnabled()) return Optional.empty();
        if (!data.isSpawnCalculated()) return Optional.empty();

        String currentDim = level.dimension().location().toString();
        if (!currentDim.equals(data.getSpawnDim())) {
            return Optional.empty();
        }

        return Optional.of(new BlockPos(data.getSpawnX(), data.getSpawnY(), data.getSpawnZ()));
    }

    public static boolean isCustomSpawnWorld(MinecraftServer server) {
        SetSpawnData data = SetSpawnData.get(server.overworld());
        ensureWorldSpawnState(server, data);
        return data.isSetSpawnWorldEnabled();
    }

    private static void ensureWorldSpawnState(MinecraftServer server, SetSpawnData data) {
        if (!data.isLoadedFromDisk() && !data.isSetSpawnWorldChecked() && !data.isSpawnCalculated()) {
            debug("ensureWorldSpawnState: Initializing new world spawn state");
            initializeNewDataWorldSpawnState(server, data);
            data.setDataVersion(SetSpawnData.CURRENT_DATA_VERSION);
            data.setInitialized(true);
            data.setDirty();
            return;
        }

        if (data.getDataVersion() < SetSpawnData.CURRENT_DATA_VERSION) {
            debug("ensureWorldSpawnState: Migrating legacy data from v" + data.getDataVersion() + " to v" + SetSpawnData.CURRENT_DATA_VERSION);
            migrateLegacyWorldSpawnState(server, data);
            data.setDataVersion(SetSpawnData.CURRENT_DATA_VERSION);
            data.setInitialized(true);
            data.setDirty();
        }

        if (!data.isSetSpawnWorldChecked()) {
            debug("ensureWorldSpawnState: Unchecked data, performing legacy protection initialization");
            initializeLegacyWorldSpawnState(server, data);
        }
    }

    private static void migrateLegacyWorldSpawnState(MinecraftServer server, SetSpawnData data) {
        if (data.isAdminSpawn()) {
            debug("migrateLegacyWorldSpawnState: Admin spawn detected, keeping fixed dimension");
            data.setSetSpawnWorldChecked(true);
            data.setSetSpawnWorldEnabled(data.isSpawnCalculated());
            data.setAutomaticSpawnWorld(false);
            return;
        }

        if (data.getDataVersion() == 3 && !data.isSpawnCalculated() && !data.isSetSpawnWorldEnabled() && !hasAnyPlayerData(server)) {
            debug("migrateLegacyWorldSpawnState: v3 false positive fix, re-enabling automatic spawn");
            data.setSetSpawnWorldChecked(true);
            data.setSetSpawnWorldEnabled(true);
            data.setAutomaticSpawnWorld(true);
            return;
        }

        if (hasExistingWorldFootprint(server)) {
            debug("migrateLegacyWorldSpawnState: Existing world footprint detected, disabling automatic spawn");
            disableAutomaticSpawnForExistingWorld(data);
            restoreOriginalOverworldSpawnIfPossible(server, data);
            return;
        }

        if (!data.isSetSpawnWorldChecked()) {
            debug("migrateLegacyWorldSpawnState: Unchecked data, performing legacy protection init");
            initializeLegacyWorldSpawnState(server, data);
            return;
        }

        if (!data.isSetSpawnWorldEnabled()) {
            debug("migrateLegacyWorldSpawnState: Custom spawn explicitly disabled");
            data.setSpawnCalculated(false);
            data.setAutomaticSpawnWorld(false);
            return;
        }

        debug("migrateLegacyWorldSpawnState: Keeping automatic spawn world state");
        data.setAutomaticSpawnWorld(true);
    }

    private static void initializeNewDataWorldSpawnState(MinecraftServer server, SetSpawnData data) {
        boolean hasPlayerData = hasAnyPlayerData(server);
        boolean isNewWorld = !hasPlayerData;
        debug("initializeNewDataWorldSpawnState: hasPlayerData=" + hasPlayerData + ", isNewWorld=" + isNewWorld);
        data.setSetSpawnWorldChecked(true);
        data.setSetSpawnWorldEnabled(isNewWorld);
        data.setAutomaticSpawnWorld(isNewWorld);

        if (!isNewWorld) {
            disableAutomaticSpawnForExistingWorld(data);
        }
    }

    private static void initializeLegacyWorldSpawnState(MinecraftServer server, SetSpawnData data) {
        boolean hasFootprint = hasExistingWorldFootprint(server);
        boolean isNewWorld = !hasFootprint;
        debug("initializeLegacyWorldSpawnState: hasExistingWorldFootprint=" + hasFootprint + ", isNewWorld=" + isNewWorld);
        data.setSetSpawnWorldChecked(true);
        data.setSetSpawnWorldEnabled(isNewWorld);
        data.setAutomaticSpawnWorld(isNewWorld);

        if (!isNewWorld) {
            disableAutomaticSpawnForExistingWorld(data);
        }
    }

    private static void disableAutomaticSpawnForExistingWorld(SetSpawnData data) {
        debug("disableAutomaticSpawnForExistingWorld: Disabling automatic spawn, data=" + describeData(data));
        data.setSetSpawnWorldChecked(true);
        data.setSetSpawnWorldEnabled(false);
        data.setSpawnCalculated(false);
        data.setAdminSpawn(false);
        data.setAutomaticSpawnWorld(false);
    }

    public static Optional<Pair<ServerLevel, BlockPos>> getOrCreateGlobalSpawn(MinecraftServer server) {
        debug("getOrCreateGlobalSpawn start: configEnable=" + SetSpawnConfig.enableCustomSpawn);
        if (!SetSpawnConfig.enableCustomSpawn) {
            return Optional.empty();
        }
        if (!isCustomSpawnWorld(server)) {
            debug("getOrCreateGlobalSpawn abort: custom spawn disabled for this world");
            return Optional.empty();
        }

        SetSpawnData data = SetSpawnData.get(server.overworld());

        if (data.isSpawnCalculated()) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.getSpawnDim()));
            ServerLevel level = server.getLevel(dimKey);
            if (level == null) {
                debug("getOrCreateGlobalSpawn abort: dimension not found, dim=" + data.getSpawnDim());
                return Optional.empty();
            }

            BlockPos savedPos = new BlockPos(data.getSpawnX(), data.getSpawnY(), data.getSpawnZ());
            BlockPos pos = ensureFinalSafeSpawn(server, level, savedPos, data.isAdminSpawn());
            debug("getOrCreateGlobalSpawn: Returning fixed spawn: dim=" + level.dimension().location() + ", pos=" + posToString(pos));
            return Optional.of(Pair.of(level, pos));
        }

        debug("getOrCreateGlobalSpawn: First time calculation required");
        Optional<Pair<ServerLevel, BlockPos>> calculated = calculateConfiguredSpawn(server);
        if (calculated.isEmpty()) {
            debug("getOrCreateGlobalSpawn abort: calculateConfiguredSpawn returned empty");
            return Optional.empty();
        }

        ServerLevel level = calculated.get().getFirst();
        BlockPos pos = ensureFinalSafeSpawn(server, level, calculated.get().getSecond(), false);
        debug("getOrCreateGlobalSpawn: Returning new calculated spawn: dim=" + level.dimension().location() + ", pos=" + posToString(pos));

        return Optional.of(Pair.of(level, pos));
    }

    public static Optional<Pair<ServerLevel, BlockPos>> getOrCreateRespawnSpawn(MinecraftServer server) {
        Optional<Pair<ServerLevel, BlockPos>> spawn = getOrCreateGlobalSpawn(server);
        if (spawn.isEmpty()) {
            return Optional.empty();
        }

        ServerLevel level = spawn.get().getFirst();
        BlockPos requestedPos = spawn.get().getSecond();
        BlockPos pos = ensureFinalSafeSpawn(server, level, requestedPos, false);

        return Optional.of(Pair.of(level, pos));
    }

    private static BlockPos ensureFinalSafeSpawn(MinecraftServer server, ServerLevel level, BlockPos pos, boolean adminSpawn) {
        debug("ensureFinalSafeSpawn start: dim=" + level.dimension().location() + ", requested=" + posToString(pos) + ", adminSpawn=" + adminSpawn);
        loadChunk(level, pos);
        boolean inputSafe = isFinalSpawnSafe(level, pos);

        BlockPos safePos = inputSafe ? pos : findHighOpenSpawnAround(level, pos, SAFE_SEARCH_RADIUS, false, FINAL_SAFE_SEARCH_TIMEOUT_MS);
        boolean createdPlatform = false;

        if (safePos == null) {
            debug("ensureFinalSafeSpawn: Safe pos not found quickly, creating protected platform");
            safePos = buildProtectedSpawnPlatform(level, pos);
            createdPlatform = true;
        }

        if (createdPlatform) {
            if (!isProtectedSpawnSafe(level, safePos)) {
                debug("ensureFinalSafeSpawn: Protected platform safety check failed, rebuilding at " + posToString(safePos));
                safePos = buildProtectedSpawnPlatform(level, safePos);
            }
        } else if (!isFinalSpawnSafe(level, safePos)) {
            debug("ensureFinalSafeSpawn: Selected safe pos became unsafe, creating protected platform at " + posToString(safePos));
            safePos = buildProtectedSpawnPlatform(level, safePos);
            createdPlatform = true;
        }

        debug("ensureFinalSafeSpawn final pos: dim=" + level.dimension().location() + ", pos=" + posToString(safePos) + ", createdPlatform=" + createdPlatform);
        saveSpawn(server, level, safePos, adminSpawn);
        setWorldSpawn(level, safePos);

        return safePos;
    }

    private static void placePlayerExactly(ServerPlayer player, BlockPos pos) {
        player.clearFire();
        player.fallDistance = 0.0F;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
        player.setPos(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D
        );
    }

    private static void syncPlayerExactPosition(ServerPlayer player, BlockPos pos) {
        placePlayerExactly(player, pos);
        player.connection.teleport(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }

    private static Optional<Pair<ServerLevel, BlockPos>> calculateConfiguredSpawn(MinecraftServer server) {
        String dimId = chooseTargetDimension();
        List<String> effectiveStructures = filterUsableStructureIds(SetSpawnConfig.setspawnStructures);
        boolean requestedStructures = SetSpawnConfig.enableStructures && !SetSpawnConfig.setspawnStructures.isEmpty();
        boolean useStructures = SetSpawnConfig.enableStructures && !effectiveStructures.isEmpty();
        boolean useBiomes = SetSpawnConfig.enableBiomes && !SetSpawnConfig.setspawnBiomes.isEmpty();
        boolean useDimensionFallback = SetSpawnConfig.enableDimensions && !SetSpawnConfig.setspawnDimensions.isEmpty();

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimId));
        ServerLevel level = server.getLevel(dimKey);
        if (level == null) {
            debug("calculateConfiguredSpawn abort: target dimension not loaded/exists: " + dimId);
            return Optional.empty();
        }

        if (requestedStructures && !useStructures) {
            debug("calculateConfiguredSpawn: Structure search skipped (all disabled/invalid), falling back to biome/dim logic");
        }

        if (useStructures && server.getWorldData().worldGenOptions().generateStructures()) {
            debug("calculateConfiguredSpawn: Starting structure search");
            BlockPos structurePos = findStructureSpawn(level, effectiveStructures);
            if (structurePos != null) {
                int radius = Math.max(64, Math.min(160, SetSpawnConfig.structureRadius));
                BlockPos safePos = findHighOpenSpawnAround(level, structurePos, radius, useBiomes, STRUCTURE_SAFE_SEARCH_TIMEOUT_MS);
                if (safePos == null && useBiomes) {
                    debug("calculateConfiguredSpawn: Safe pos with biome failed, retrying without biome restriction");
                    safePos = findHighOpenSpawnAround(level, structurePos, radius, false, STRUCTURE_SAFE_SEARCH_TIMEOUT_MS);
                }
                if (safePos == null) {
                    debug("calculateConfiguredSpawn: Safe pos near structure failed, falling back to biome/dim logic");
                } else {
                    debug("calculateConfiguredSpawn structure mode success: dim=" + level.dimension().location() + ", pos=" + posToString(safePos));
                    return Optional.of(Pair.of(level, safePos));
                }
            } else {
                debug("calculateConfiguredSpawn: Structure search failed or timed out, falling back to biome/dim logic");
            }
        } else if (useStructures) {
            debug("calculateConfiguredSpawn: Structure generation disabled in this world, falling back to biome/dim logic");
        }

        if (useBiomes) {
            debug("calculateConfiguredSpawn: Starting biome search");
            BlockPos biomePos = findBiomeSpawn(level);
            if (biomePos != null) {
                int radius = Math.max(64, Math.min(160, SetSpawnConfig.biomeStep * 6));
                BlockPos safePos = findHighOpenSpawnAround(level, biomePos, radius, true, BIOME_SAFE_SEARCH_TIMEOUT_MS);
                if (safePos == null) {
                    debug("calculateConfiguredSpawn: Safe pos near biome failed, falling back to dimension logic");
                } else {
                    debug("calculateConfiguredSpawn biome mode success: dim=" + level.dimension().location() + ", pos=" + posToString(safePos));
                    return Optional.of(Pair.of(level, safePos));
                }
            }
        }

        if (useDimensionFallback || (!requestedStructures && !useBiomes)) {
            BlockPos fallbackCenter = getDimensionFallbackCenter(level);
            BlockPos safePos = findHighOpenSpawnAround(level, fallbackCenter, SAFE_SEARCH_RADIUS, false, DIMENSION_SAFE_SEARCH_TIMEOUT_MS);
            if (safePos == null) {
                debug("calculateConfiguredSpawn: Safe pos near dimension fallback failed, using protected platform center");
                return Optional.of(Pair.of(level, fallbackCenter));
            }

            debug("calculateConfiguredSpawn fallback mode success: dim=" + level.dimension().location() + ", pos=" + posToString(safePos));
            return Optional.of(Pair.of(level, safePos));
        }

        BlockPos fallbackCenter = getDimensionFallbackCenter(level);
        debug("calculateConfiguredSpawn: All methods failed, using protected platform center: " + posToString(fallbackCenter));
        return Optional.of(Pair.of(level, fallbackCenter));
    }


    private static List<String> filterUsableStructureIds(List<String> rawIds) {
        List<String> result = new ArrayList<>();
        if (rawIds == null) {
            return result;
        }

        for (String raw : rawIds) {
            if (raw == null) {
                continue;
            }

            String id = raw.trim();
            if (!id.isEmpty() && !result.contains(id)) {
                result.add(id);
            }
        }

        return result;
    }

    private static String chooseTargetDimension() {
        List<String> dims = SetSpawnConfig.setspawnDimensions;
        if (SetSpawnConfig.enableDimensions && dims != null && !dims.isEmpty()) {
            List<String> valid = new ArrayList<>();
            for (String dim : dims) {
                if (dim != null && !dim.isBlank()) {
                    valid.add(dim.trim());
                }
            }
            if (!valid.isEmpty()) {
                return valid.get(new Random().nextInt(valid.size()));
            }
        }
        debug("chooseTargetDimension: dimensions disabled or empty, using minecraft:overworld");
        return "minecraft:overworld";
    }

    private static boolean hasAnyPlayerData(MinecraftServer server) {
        if (hasPlayerDataDirectoryEntries(server)) {
            return true;
        }

        return hasSingleplayerPlayerTag(server);
    }

    private static boolean hasExistingWorldFootprint(MinecraftServer server) {
        if (hasAnyPlayerData(server)) {
            return true;
        }

        return hasRegionOrDimensionChunkFiles(server);
    }

    private static boolean hasPlayerDataDirectoryEntries(MinecraftServer server) {
        try {
            Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            if (!Files.exists(playerDataDir)) {
                return false;
            }

            try (Stream<Path> stream = Files.list(playerDataDir)) {
                return stream.anyMatch(path -> path.getFileName().toString().endsWith(".dat"));
            }
        } catch (Exception e) {
            debug("hasPlayerDataDirectoryEntries error (assuming true): " + e.getMessage());
            return true;
        }
    }

    private static boolean hasSingleplayerPlayerTag(MinecraftServer server) {
        try {
            Path worldRoot = getWorldRoot(server);
            if (worldRoot == null) {
                return true;
            }

            Path levelDat = worldRoot.resolve("level.dat");
            if (!Files.exists(levelDat)) {
                return false;
            }

            try (InputStream inputStream = Files.newInputStream(levelDat)) {
                CompoundTag root = NbtIo.readCompressed(inputStream);
                CompoundTag data = root.getCompound("Data");
                return data.contains("Player", Tag.TAG_COMPOUND);
            }
        } catch (Exception e) {
            debug("hasSingleplayerPlayerTag error (assuming true): " + e.getMessage());
            return true;
        }
    }

    private static boolean hasRegionOrDimensionChunkFiles(MinecraftServer server) {
        try {
            Path worldRoot = getWorldRoot(server);
            if (worldRoot == null || !Files.exists(worldRoot)) {
                return false;
            }

            try (Stream<Path> stream = Files.find(
                    worldRoot,
                    6,
                    (path, attributes) -> attributes.isRegularFile()
                            && path.getFileName().toString().endsWith(".mca")
                            && isWorldGeneratedDataFile(worldRoot, path)
            )) {
                Optional<Path> first = stream.findAny();
                return first.isPresent();
            }
        } catch (Exception e) {
            debug("hasRegionOrDimensionChunkFiles error (assuming true): " + e.getMessage());
            return true;
        }
    }

    private static boolean isWorldGeneratedDataFile(Path worldRoot, Path path) {
        Path relative = worldRoot.relativize(path);
        for (Path part : relative) {
            String name = part.toString();
            if ("region".equals(name) || "entities".equals(name) || "poi".equals(name)) {
                return true;
            }
        }

        return false;
    }

    private static Path getWorldRoot(MinecraftServer server) {
        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        return playerDataDir.getParent();
    }

    private static void saveSpawn(MinecraftServer server, ServerLevel level, BlockPos pos, boolean adminSpawn) {
        SetSpawnData data = SetSpawnData.get(server.overworld());
        boolean wasSpawnCalculated = data.isSpawnCalculated();
        boolean wasAdminSpawn = data.isAdminSpawn();
        boolean wasAutomaticSpawnWorld = data.isAutomaticSpawnWorld();

        captureOriginalOverworldSpawnIfNeeded(server, data);
        data.setSetSpawnWorldChecked(true);
        data.setSetSpawnWorldEnabled(true);
        data.setSpawnCalculated(true);

        if (adminSpawn) {
            data.setAdminSpawn(true);
            data.setAutomaticSpawnWorld(false);
        } else if (!wasSpawnCalculated) {
            data.setAdminSpawn(false);
            data.setAutomaticSpawnWorld(true);
        } else {
            data.setAdminSpawn(wasAdminSpawn);
            data.setAutomaticSpawnWorld(wasAutomaticSpawnWorld);
        }

        data.setDataVersion(SetSpawnData.CURRENT_DATA_VERSION);
        data.setInitialized(true);
        data.setSpawnDim(level.dimension().location().toString());
        data.setSpawnX(pos.getX());
        data.setSpawnY(pos.getY());
        data.setSpawnZ(pos.getZ());
        data.setDirty();
    }

    private static void captureOriginalOverworldSpawnIfNeeded(MinecraftServer server, SetSpawnData data) {
        if (data.isOriginalSpawnCaptured()) {
            return;
        }

        ServerLevel overworld = server.overworld();
        BlockPos original = getVanillaSharedSpawn(overworld);
        data.setOriginalSpawnCaptured(true);
        data.setOriginalSpawnDim(overworld.dimension().location().toString());
        data.setOriginalSpawnX(original.getX());
        data.setOriginalSpawnY(original.getY());
        data.setOriginalSpawnZ(original.getZ());
    }

    private static void restoreOriginalOverworldSpawnIfPossible(MinecraftServer server, SetSpawnData data) {
        if (!data.isOriginalSpawnCaptured()) {
            return;
        }

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.getOriginalSpawnDim()));
        ServerLevel level = server.getLevel(dimKey);
        if (level == null) {
            return;
        }

        setWorldSpawn(level, new BlockPos(data.getOriginalSpawnX(), data.getOriginalSpawnY(), data.getOriginalSpawnZ()));
    }

    private static void setWorldSpawn(ServerLevel level, BlockPos pos) {
        debug("setWorldSpawn: dim=" + level.dimension().location() + ", pos=" + posToString(pos));
        isInternalModifying = true;
        try {
            level.setDefaultSpawnPos(pos, 0.0F);
        } finally {
            isInternalModifying = false;
        }
    }

    private static void loadChunk(ServerLevel level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
    }

    private static void loadChunk(ServerLevel level, BlockPos pos, Set<ChunkPos> loadedChunks) {
        ChunkPos chunkPos = new ChunkPos(pos);
        if (loadedChunks.add(chunkPos)) {
            loadChunk(level, pos);
        }
    }

    private static BlockPos getVanillaSharedSpawn(ServerLevel level) {
        return new BlockPos(level.getLevelData().getXSpawn(), level.getLevelData().getYSpawn(), level.getLevelData().getZSpawn());
    }

    private static BlockPos getDimensionFallbackCenter(ServerLevel level) {
        BlockPos vanilla = getVanillaSharedSpawn(level);
        int x = vanilla.getX();
        int z = vanilla.getZ();
        int minY = level.getMinBuildHeight() + 4;
        int maxY = Math.min(level.getMaxBuildHeight() - 4, level.getLogicalHeight() - 4);
        int y = vanilla.getY();

        if (level.dimensionType().hasCeiling()) {
            if (y < minY || y > maxY) {
                y = defaultSafeY(minY, maxY);
            }
        } else {
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (surfaceY >= minY && surfaceY <= maxY) {
                y = surfaceY;
            } else if (y < minY || y > maxY) {
                y = defaultSafeY(minY, maxY);
            }
        }

        return new BlockPos(x, y, z);
    }

    private static BlockPos findStructureSpawn(ServerLevel level, List<String> targetStructures) {
        if (targetStructures == null || targetStructures.isEmpty()) {
            return null;
        }

        List<Holder<Structure>> holders = new ArrayList<>();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (String id : targetStructures) {
            if (id == null || id.isBlank()) continue;

            try {
                ResourceLocation loc = new ResourceLocation(id.trim());
                Optional<Holder.Reference<Structure>> holder = registry.getHolder(ResourceKey.create(Registries.STRUCTURE, loc));
                if (holder.isPresent()) {
                    holders.add(holder.get());
                } else {
                    debug("findStructureSpawn invalid/unregistered ID: " + id.trim());
                }
            } catch (Exception e) {
                debug("findStructureSpawn ID parse error: " + id + ", error=" + e.getMessage());
            }
        }

        if (holders.isEmpty()) {
            debug("findStructureSpawn abort: no valid structure holders");
            return null;
        }

        int maxRadius = Math.max(1, SetSpawnConfig.structureRadius);
        int step = Math.min(STRUCTURE_SEARCH_STEP_CHUNKS, maxRadius);
        long timeoutMs = Math.max(1000L, (long) Math.max(1, SetSpawnConfig.structureSearchTimeoutSeconds) * 1000L);
        long deadline = System.currentTimeMillis() + timeoutMs;

        int radius = step;
        while (radius <= maxRadius) {
            if (System.currentTimeMillis() > deadline) {
                debug("findStructureSpawn timeout: radius=" + radius + ", maxRadius=" + maxRadius + ", timeoutMs=" + timeoutMs);
                return null;
            }

            int currentRadius = radius;
            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator().findNearestMapStructure(
                    level,
                    HolderSet.direct(holders),
                    BlockPos.ZERO,
                    currentRadius,
                    false
            );

            BlockPos pos = result != null ? result.getFirst() : null;
            if (pos != null) {
                return pos;
            }

            if (currentRadius >= maxRadius) {
                break;
            }
            radius = currentRadius + step;
        }

        return null;
    }

    private static BlockPos findBiomeSpawn(ServerLevel level) {
        if (SetSpawnConfig.setspawnBiomes.isEmpty()) {
            return null;
        }

        Pair<BlockPos, Holder<Biome>> result = level.getChunkSource().getGenerator().getBiomeSource().findBiomeHorizontal(
                0,
                64,
                0,
                SetSpawnConfig.spawnSearchRadius,
                SetSpawnConfig.biomeStep,
                SetSpawnHandler::matchesTargetBiomeHolder,
                level.getRandom(),
                true,
                level.getChunkSource().randomState().sampler()
        );

        return result != null ? result.getFirst() : null;
    }

    private static boolean matchesTargetBiomeHolder(Holder<Biome> holder) {
        return holder.unwrapKey()
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .map(SetSpawnConfig.setspawnBiomes::contains)
                .orElse(false);
    }

    private static boolean shouldRejectBiome(ServerLevel level, BlockPos pos) {
        return SetSpawnConfig.enableBiomes
                && !SetSpawnConfig.setspawnBiomes.isEmpty()
                && !matchesTargetBiomeHolder(level.getBiome(pos));
    }

    private static BlockPos findHighOpenSpawnAround(ServerLevel level, BlockPos center, int maxRadius, boolean requireTargetBiome, long timeoutMs) {
        if (center == null) return null;

        int radiusLimit = Math.max(0, maxRadius);
        boolean hasCeiling = level.dimensionType().hasCeiling();
        long deadline = System.currentTimeMillis() + Math.max(1L, timeoutMs);
        Set<ChunkPos> loadedChunks = new HashSet<>();

        for (int r = 0; r <= radiusLimit; r++) {
            if (isSearchTimedOut(deadline)) {
                debug("findHighOpenSpawnAround timeout: center=" + posToString(center) + ", radius=" + r + ", maxRadius=" + radiusLimit + ", timeoutMs=" + timeoutMs);
                return null;
            }

            BlockPos bestPos = null;
            int bestY = Integer.MIN_VALUE;

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (isSearchTimedOut(deadline)) {
                        debug("findHighOpenSpawnAround timeout: center=" + posToString(center) + ", radius=" + r + ", maxRadius=" + radiusLimit + ", timeoutMs=" + timeoutMs);
                        return null;
                    }
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;

                    int targetX = center.getX() + x;
                    int targetZ = center.getZ() + z;
                    BlockPos columnCenter = new BlockPos(targetX, center.getY(), targetZ);

                    loadChunk(level, columnCenter, loadedChunks);

                    BlockPos candidate = hasCeiling
                            ? findHighestCaveSpawnInColumn(level, targetX, targetZ, center.getY(), deadline)
                            : findHighestSurfaceSpawnInColumn(level, targetX, targetZ, deadline);

                    if (candidate == null) continue;
                    if (requireTargetBiome && shouldRejectBiome(level, candidate)) continue;
                    if (candidate.getY() <= bestY) continue;

                    bestPos = candidate;
                    bestY = candidate.getY();
                }
            }

            if (bestPos != null) {
                return bestPos;
            }
        }

        return null;
    }

    private static BlockPos findHighestSurfaceSpawnInColumn(ServerLevel level, int x, int z, long deadline) {
        int minY = level.getMinBuildHeight() + 4;
        int maxY = Math.min(level.getMaxBuildHeight() - 4, level.getLogicalHeight() - 4);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        int startY = Math.min(surfaceY + 8, maxY);
        int endY = Math.max(surfaceY - 16, minY);

        for (int y = startY; y >= endY; y--) {
            if (isSearchTimedOut(deadline)) return null;

            BlockPos pos = new BlockPos(x, y, z);
            if (isFinalSpawnSafe(level, pos)) return pos;
        }

        return null;
    }

    private static BlockPos findHighestCaveSpawnInColumn(ServerLevel level, int x, int z, int centerY, long deadline) {
        int minY = level.getMinBuildHeight() + 4;
        int maxY = Math.min(level.getMaxBuildHeight() - 4, level.getLogicalHeight() - 4);

        int preferredTop = Math.min(maxY, Math.max(centerY + 72, 96));
        int preferredBottom = Math.max(minY, Math.min(centerY - 24, 32));

        BlockPos preferred = scanColumnDown(level, x, z, preferredTop, preferredBottom, deadline);
        if (preferred != null) return preferred;

        return scanColumnDown(level, x, z, maxY, minY, deadline);
    }

    private static BlockPos scanColumnDown(ServerLevel level, int x, int z, int topY, int bottomY, long deadline) {
        for (int y = topY; y >= bottomY; y--) {
            if (isSearchTimedOut(deadline)) return null;

            BlockPos pos = new BlockPos(x, y, z);
            if (isFinalSpawnSafe(level, pos)) return pos;
        }

        return null;
    }

    private static boolean isSearchTimedOut(long deadline) {
        return System.currentTimeMillis() > deadline;
    }

    private static boolean isFinalSpawnSafe(ServerLevel level, BlockPos centerPos) {
        return isSolidSafeFloor(level, centerPos.below()) && isPlayerBodySpaceSafe(level, centerPos);
    }

    private static boolean isProtectedSpawnSafe(ServerLevel level, BlockPos centerPos) {
        return isSolidSafeFloor(level, centerPos.below()) && isForcedAirSpaceReady(level, centerPos);
    }

    private static boolean isPlayerBodySpaceSafe(ServerLevel level, BlockPos centerPos) {
        for (int y = 0; y < 2; y++) {
            BlockPos pos = centerPos.above(y);
            BlockState state = level.getBlockState(pos);
            if (!level.getFluidState(pos).isEmpty()) return false;
            if (isDangerousBlock(state)) return false;
            if (!state.getCollisionShape(level, pos).isEmpty()) return false;
        }

        return true;
    }

    private static boolean isForcedAirSpaceReady(ServerLevel level, BlockPos centerPos) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                for (int y = 0; y < FORCED_AIR_HEIGHT; y++) {
                    if (!isPureAir(level, centerPos.offset(x, y, z))) return false;
                }
            }
        }

        return true;
    }

    private static boolean isSolidSafeFloor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        return !state.isAir()
                && level.getFluidState(pos).isEmpty()
                && state.isFaceSturdy(level, pos, Direction.UP)
                && !state.getCollisionShape(level, pos).isEmpty()
                && !isDangerousBlock(state);
    }

    private static boolean isPureAir(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() && level.getFluidState(pos).isEmpty();
    }

    private static boolean isDangerousBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static BlockPos buildProtectedSpawnPlatform(ServerLevel level, BlockPos originalPos) {
        BlockPos platformPos = normalizePlatformPos(level, originalPos);
        Set<ChunkPos> loadedChunks = new HashSet<>();

        loadChunk(level, platformPos, loadedChunks);
        placePlatformFloor(level, platformPos, loadedChunks);
        clearForcedAirSpace(level, platformPos, loadedChunks);

        return platformPos;
    }

    private static void placePlatformFloor(ServerLevel level, BlockPos centerPos, Set<ChunkPos> loadedChunks) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                BlockPos floorPos = centerPos.offset(x, -1, z);
                loadChunk(level, floorPos, loadedChunks);
                level.setBlockAndUpdate(floorPos, Blocks.OBSIDIAN.defaultBlockState());
            }
        }
    }

    private static void clearForcedAirSpace(ServerLevel level, BlockPos centerPos, Set<ChunkPos> loadedChunks) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                for (int y = 0; y < FORCED_AIR_HEIGHT; y++) {
                    BlockPos airPos = centerPos.offset(x, y, z);
                    loadChunk(level, airPos, loadedChunks);
                    level.setBlockAndUpdate(airPos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static BlockPos normalizePlatformPos(ServerLevel level, BlockPos originalPos) {
        int x = originalPos.getX();
        int z = originalPos.getZ();

        int minY = level.getMinBuildHeight() + 4;
        int maxY = Math.min(level.getMaxBuildHeight() - 4, level.getLogicalHeight() - 4);
        int y = originalPos.getY();

        if (level.dimensionType().hasCeiling()) {
            if (y < minY || y > maxY) {
                y = defaultSafeY(minY, maxY);
            }
        } else {
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (surfaceY >= minY && surfaceY <= maxY) {
                y = surfaceY;
            } else if (y < minY || y > maxY) {
                y = defaultSafeY(minY, maxY);
            }
        }

        return new BlockPos(x, y, z);
    }


    private static void debug(String message) {
        KineticCore.LOGGER.info("{} {}", LOG_PREFIX, message);
    }

    private static String posToString(BlockPos pos) {
        if (pos == null) {
            return "null";
        }
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String describeData(SetSpawnData data) {
        return "loadedFromDisk=" + data.isLoadedFromDisk()
                + ", dataVersion=" + data.getDataVersion()
                + ", initialized=" + data.isInitialized()
                + ", checked=" + data.isSetSpawnWorldChecked()
                + ", enabled=" + data.isSetSpawnWorldEnabled()
                + ", calculated=" + data.isSpawnCalculated()
                + ", admin=" + data.isAdminSpawn()
                + ", automatic=" + data.isAutomaticSpawnWorld()
                + ", spawn=" + data.getSpawnDim() + "@" + data.getSpawnX() + "," + data.getSpawnY() + "," + data.getSpawnZ()
                + ", originalCaptured=" + data.isOriginalSpawnCaptured()
                + ", original=" + data.getOriginalSpawnDim() + "@" + data.getOriginalSpawnX() + "," + data.getOriginalSpawnY() + "," + data.getOriginalSpawnZ();
    }

    private static int defaultSafeY(int min, int max) {
        return Math.max(min, Math.min(64, max));
    }
}
