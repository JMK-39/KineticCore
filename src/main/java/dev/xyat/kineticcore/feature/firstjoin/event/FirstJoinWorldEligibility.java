package dev.xyat.kineticcore.feature.firstjoin.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

/** Conservative first-run check. An uncertain or previously played save never receives a starter kit. */
final class FirstJoinWorldEligibility {
    static final long MAX_WORLD_AGE_MILLIS = 10 * 60 * 1000L;
    private static final long MAX_PLAY_TICKS = 12_000L;

    private FirstJoinWorldEligibility() {
    }

    static boolean isNewWorld(long gameTimeTicks, long ageMillis, boolean hasSavedPlayerData) {
        return !hasSavedPlayerData
                && gameTimeTicks >= 0 && gameTimeTicks <= MAX_PLAY_TICKS
                && ageMillis >= 0 && ageMillis <= MAX_WORLD_AGE_MILLIS;
    }

    static boolean isStillNew(long gameTimeTicks, long nowMillis, long validUntilMillis) {
        return validUntilMillis > 0 && nowMillis <= validUntilMillis
                && gameTimeTicks >= 0 && gameTimeTicks <= MAX_PLAY_TICKS;
    }

    static Inspection inspect(Path worldRoot, Path playerData, long gameTimeTicks, long nowMillis) throws IOException {
        boolean savedPlayers = false;
        if (Files.isDirectory(playerData)) {
            try (Stream<Path> files = Files.list(playerData)) {
                savedPlayers = files.anyMatch(path -> path.getFileName().toString().endsWith(".dat"));
            }
        }
        long rootAge = nowMillis - Files.readAttributes(worldRoot, BasicFileAttributes.class).creationTime().toMillis();
        Path levelDat = worldRoot.resolve("level.dat");
        if (!Files.isRegularFile(levelDat)) {
            return new Inspection(isNewWorld(gameTimeTicks, rootAge, savedPlayers), rootAge);
        }
        long fileAge = nowMillis - Files.readAttributes(levelDat, BasicFileAttributes.class).creationTime().toMillis();
        long ageMillis = Math.max(rootAge, fileAge);
        try (InputStream input = Files.newInputStream(levelDat)) {
            CompoundTag root = NbtIo.readCompressed(input);
            if (!root.contains("Data", Tag.TAG_COMPOUND)) return new Inspection(false, ageMillis);
            CompoundTag data = root.getCompound("Data");
            savedPlayers |= data.contains("Player", Tag.TAG_COMPOUND);
            long lastPlayed = data.getLong("LastPlayed");
            if (lastPlayed > 0) ageMillis = Math.max(ageMillis, nowMillis - lastPlayed);
            long gameTime = Math.max(gameTimeTicks, data.getLong("Time"));
            return new Inspection(isNewWorld(gameTime, ageMillis, savedPlayers), ageMillis);
        }
    }

    record Inspection(boolean eligible, long ageMillis) {
    }
}
