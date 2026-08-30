package dev.xyat.kineticcore.feature.spawnegg.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

@KTModule
public final class SpawnEggConfig {
    public static final boolean DEFAULT_ENABLED = true;
    public static final double DEFAULT_SPEED = 1.5D;
    public static final double DEFAULT_INACCURACY = 0.2D;

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/spawnegg.toml");
    private static CommentedFileConfig configData;

    public static boolean enableSpawnEggThrow = DEFAULT_ENABLED;
    public static double spawnEggThrowSpeed = DEFAULT_SPEED;
    public static double spawnEggThrowInaccuracy = DEFAULT_INACCURACY;

    private SpawnEggConfig() {
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync()
                    .preserveInsertionOrder()
                    .writingMode(WritingMode.REPLACE)
                    .build();
            configData.load();
            setupConfig();
            configData.save();
            readValues();
            registerServerConfig();
        } catch (Exception exception) {
            KineticCore.LOGGER.error("SpawnEggConfig Load Failed", exception);
        }
    }

    private static void registerServerConfig() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticcore:spawn_egg")
                .booleanValue("enable", () -> enableSpawnEggThrow, value -> enableSpawnEggThrow = value)
                .doubleValue("speed", () -> spawnEggThrowSpeed, value -> spawnEggThrowSpeed = value, 0.05D, 20.0D)
                .doubleValue("inaccuracy", () -> spawnEggThrowInaccuracy, value -> spawnEggThrowInaccuracy = value, 0.0D, 20.0D)
                .onSave(SpawnEggConfig::save)
                .build());
    }

    private static void setupConfig() {
        configData.setComment("spawn_eggs", """
                 投掷刷怪蛋
                 Throwable Spawn Eggs""");
        define("spawn_eggs.enable", DEFAULT_ENABLED, """
                 是否允许像雪球一样投掷刷怪蛋来生成实体
                 Allow throwing spawn eggs like snowballs to spawn entities.""");
        define("spawn_eggs.speed", DEFAULT_SPEED, """
                 刷怪蛋投射物飞行速度
                 Throwing speed of the spawn egg projectile.""");
        define("spawn_eggs.inaccuracy", DEFAULT_INACCURACY, """
                 刷怪蛋投射物散布值，数值越低越精准
                 Throwing inaccuracy. Lower values are more accurate.""");
    }

    private static void define(String path, Object defaultValue, String comment) {
        if (!configData.contains(path)) {
            configData.set(path, defaultValue);
        }
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enableSpawnEggThrow = configData.getOrElse("spawn_eggs.enable", DEFAULT_ENABLED);
        spawnEggThrowSpeed = readDouble("spawn_eggs.speed", DEFAULT_SPEED);
        spawnEggThrowInaccuracy = readDouble("spawn_eggs.inaccuracy", DEFAULT_INACCURACY);
    }

    private static double readDouble(String path, double defaultValue) {
        Object value = configData.getOrElse(path, defaultValue);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public static void save() {
        if (configData == null) {
            throw new IllegalStateException("Spawn egg config is not loaded");
        }
        configData.set("spawn_eggs.enable", enableSpawnEggThrow);
        configData.set("spawn_eggs.speed", spawnEggThrowSpeed);
        configData.set("spawn_eggs.inaccuracy", spawnEggThrowInaccuracy);
        configData.save();
        readValues();
    }
}
