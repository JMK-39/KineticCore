package dev.xyat.kineticcore.feature.setspawn.config;

import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import dev.xyat.kineticcore.feature.setspawn.network.SetSpawnNetwork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SetSpawnConfigGui {
    public static final String PAGE_ID = "kineticcore:setspawn";
    public static final String RULES_PAGE_ID = "kineticcore:setspawn_rules";

    private SetSpawnConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(buildSettingsPage());
        KTConfigApi.register(buildRulesPage());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreenForOwner(parent, "kineticcore");
    }

    private static KTConfigPage buildSettingsPage() {
        return KTConfigPage.builder(PAGE_ID, Component.translatable("cfg.kineticcore.setspawn"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(Component.translatable("cfg.kineticcore.setspawn.apply_notice"))
                .pageDescription(Component.translatable("cfg.kineticcore.setspawn.description"))
                .booleanValue(
                        "enable",
                        Component.translatable("cfg.kineticcore.setspawn.enable"),
                        () -> SetSpawnConfig.enableCustomSpawn,
                        value -> SetSpawnConfig.enableCustomSpawn = value,
                        true,
                        Component.translatable("cfg.kineticcore.setspawn.enable.tooltip")
                )
                .intValue(
                        "radius",
                        Component.translatable("cfg.kineticcore.setspawn.radius"),
                        () -> SetSpawnConfig.spawnSearchRadius,
                        value -> SetSpawnConfig.spawnSearchRadius = value,
                        10000,
                        0,
                        Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.setspawn.radius.tooltip")
                )
                .intValue(
                        "structure_radius",
                        Component.translatable("cfg.kineticcore.setspawn.structure_radius"),
                        () -> SetSpawnConfig.structureRadius,
                        value -> SetSpawnConfig.structureRadius = value,
                        256,
                        0,
                        Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.setspawn.structure_radius.tooltip")
                )
                .intValue(
                        "structure_timeout_seconds",
                        Component.translatable("cfg.kineticcore.setspawn.structure_timeout_seconds"),
                        () -> SetSpawnConfig.structureSearchTimeoutSeconds,
                        value -> SetSpawnConfig.structureSearchTimeoutSeconds = value,
                        12,
                        1,
                        Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.setspawn.structure_timeout_seconds.tooltip")
                )
                .intValue(
                        "biome_step",
                        Component.translatable("cfg.kineticcore.setspawn.biome_step"),
                        () -> SetSpawnConfig.biomeStep,
                        value -> SetSpawnConfig.biomeStep = value,
                        48,
                        1,
                        Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.setspawn.biome_step.tooltip")
                )
                .build();
    }

    private static KTConfigPage buildRulesPage() {
        return KTConfigPage.builder(
                        RULES_PAGE_ID,
                        Component.translatable("cfg.kineticcore.setspawn.rules.title")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(Component.translatable("cfg.kineticcore.setspawn.apply_notice"))
                .pageDescription(Component.translatable("cfg.kineticcore.setspawn.rules.description"))
                .action(
                        "open_rule_editor",
                        Component.translatable("cfg.kineticcore.setspawn.rules.open"),
                        SetSpawnNetwork::requestOpenEditor,
                        Component.translatable("cfg.kineticcore.setspawn.rules.open.tooltip")
                )
                .build();
    }
}
