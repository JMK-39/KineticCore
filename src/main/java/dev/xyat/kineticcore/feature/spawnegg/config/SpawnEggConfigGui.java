package dev.xyat.kineticcore.feature.spawnegg.config;

import dev.xyat.kineticcore.ConfigGui;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import net.minecraft.network.chat.Component;

@KTClientModule
public final class SpawnEggConfigGui {
    public static final String PAGE_ID = "kineticcore:spawn_egg";

    private SpawnEggConfigGui() {
    }

    public static void load() {
        ConfigGui.register(KTConfigPage.builder(PAGE_ID, Component.translatable("cfg.kineticcore.spawnegg.title"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .pageDescription(Component.translatable("cfg.kineticcore.spawnegg.description"))
                .section(Component.translatable("cfg.kineticcore.spawnegg.title"))
                .booleanValue(
                        "enable",
                        Component.translatable("cfg.kineticcore.spawnegg.enable"),
                        () -> SpawnEggConfig.enableSpawnEggThrow,
                        value -> SpawnEggConfig.enableSpawnEggThrow = value,
                        SpawnEggConfig.DEFAULT_ENABLED,
                        Component.translatable("cfg.kineticcore.spawnegg.enable.tooltip")
                )
                .doubleValue(
                        "speed",
                        Component.translatable("cfg.kineticcore.spawnegg.speed"),
                        () -> SpawnEggConfig.spawnEggThrowSpeed,
                        value -> SpawnEggConfig.spawnEggThrowSpeed = value,
                        SpawnEggConfig.DEFAULT_SPEED,
                        0.05D,
                        20.0D,
                        Component.translatable("cfg.kineticcore.spawnegg.speed.tooltip")
                )
                .doubleValue(
                        "inaccuracy",
                        Component.translatable("cfg.kineticcore.spawnegg.inaccuracy"),
                        () -> SpawnEggConfig.spawnEggThrowInaccuracy,
                        value -> SpawnEggConfig.spawnEggThrowInaccuracy = value,
                        SpawnEggConfig.DEFAULT_INACCURACY,
                        0.0D,
                        20.0D,
                        Component.translatable("cfg.kineticcore.spawnegg.inaccuracy.tooltip")
                )
                .build());
    }
}
