package dev.xyat.kineticcore.feature.spawnegg.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;

public final class SpawnEggConfigGui {
    public static final String PAGE_ID = "kineticcore:spawn_egg";

    private SpawnEggConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(PAGE_ID, KineticI18n.translatable("cfg.kineticcore.spawnegg.title"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.spawnegg.description"))
                .section(KineticI18n.translatable("cfg.kineticcore.spawnegg.title"))
                .booleanValue(
                        "enable",
                        KineticI18n.translatable("cfg.kineticcore.spawnegg.enable"),
                        () -> SpawnEggConfig.enableSpawnEggThrow,
                        value -> SpawnEggConfig.enableSpawnEggThrow = value,
                        SpawnEggConfig.DEFAULT_ENABLED,
                        KineticI18n.translatable("cfg.kineticcore.spawnegg.enable.tooltip")
                )
                .doubleValue(
                        "speed",
                        KineticI18n.translatable("cfg.kineticcore.spawnegg.speed"),
                        () -> SpawnEggConfig.spawnEggThrowSpeed,
                        value -> SpawnEggConfig.spawnEggThrowSpeed = value,
                        SpawnEggConfig.DEFAULT_SPEED,
                        0.05D,
                        20.0D,
                        KineticI18n.translatable("cfg.kineticcore.spawnegg.speed.tooltip")
                )
                .doubleValue(
                        "inaccuracy",
                        KineticI18n.translatable("cfg.kineticcore.spawnegg.inaccuracy"),
                        () -> SpawnEggConfig.spawnEggThrowInaccuracy,
                        value -> SpawnEggConfig.spawnEggThrowInaccuracy = value,
                        SpawnEggConfig.DEFAULT_INACCURACY,
                        0.0D,
                        20.0D,
                        KineticI18n.translatable("cfg.kineticcore.spawnegg.inaccuracy.tooltip")
                )
                .build());
    }
}
