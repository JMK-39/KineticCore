package dev.xyat.kineticcore.feature.firstjoin.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FirstJoinWorldDiskRegression {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("kineticcore-firstjoin-");
        Path playerData = root.resolve("playerdata");
        Path playerFile = playerData.resolve("existing.dat");
        Path levelDat = root.resolve("level.dat");
        long now = System.currentTimeMillis();
        try {
            check(FirstJoinWorldEligibility.inspect(root, playerData, 0, now).eligible(), "new unsaved world");
            writeLevel(levelDat, 0, now, false);
            check(FirstJoinWorldEligibility.inspect(root, playerData, 0, now).eligible(), "new saved world");
            writeLevel(levelDat, 0, now, true);
            check(!FirstJoinWorldEligibility.inspect(root, playerData, 0, now).eligible(), "singleplayer saved player");
            Files.createDirectories(playerData);
            Files.createFile(playerFile);
            check(!FirstJoinWorldEligibility.inspect(root, playerData, 0, now).eligible(), "saved player");
            Files.delete(playerFile);
            writeLevel(levelDat, 20_000, now, false);
            check(!FirstJoinWorldEligibility.inspect(root, playerData, 0, now).eligible(), "old game time");
            writeLevel(levelDat, 0, now - 900_000, false);
            check(!FirstJoinWorldEligibility.inspect(root, playerData, 0, now).eligible(), "old last played");
            try (OutputStream output = Files.newOutputStream(levelDat)) {
                NbtIo.writeCompressed(new CompoundTag(), output);
            }
            check(!FirstJoinWorldEligibility.inspect(root, playerData, 0, now).eligible(), "missing world data");
        } finally {
            Files.deleteIfExists(playerFile);
            Files.deleteIfExists(playerData);
            Files.deleteIfExists(levelDat);
            Files.deleteIfExists(root);
        }
    }

    private static void writeLevel(Path file, long gameTime, long lastPlayed, boolean savedPlayer) throws Exception {
        CompoundTag data = new CompoundTag();
        data.putLong("Time", gameTime);
        data.putLong("LastPlayed", lastPlayed);
        if (savedPlayer) data.put("Player", new CompoundTag());
        CompoundTag root = new CompoundTag();
        root.put("Data", data);
        try (OutputStream output = Files.newOutputStream(file)) {
            NbtIo.writeCompressed(root, output);
        }
    }

    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
}
