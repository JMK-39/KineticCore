package dev.xyat.kineticcore.feature.logcleaner.config;

import dev.xyat.kineticcore.ConfigGui;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@KTClientModule
public final class LogCleanerConfigGui {
    public static final String PAGE_ID = "kineticcore:log_cleaner";

    private LogCleanerConfigGui() {
    }

    public static void load() {
        ConfigGui.register(buildPage());
    }

    public static Screen create(Screen parent) {
        return ConfigGui.create(parent, PAGE_ID);
    }

    private static KTConfigPage buildPage() {
        return KTConfigPage.builder(PAGE_ID, Component.translatable("cfg.kineticcore.logcleaner.title"))
                .scope(KTConfigScope.LOCAL_INSTALLATION)
                .pageDescription(Component.translatable("cfg.kineticcore.logcleaner.description"))
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .booleanValue(
                        "enable",
                        Component.translatable("cfg.kineticcore.logcleaner.enable"),
                        () -> LogCleanerConfig.enableCleanup,
                        value -> LogCleanerConfig.enableCleanup = value,
                        true,
                        Component.translatable("cfg.kineticcore.logcleaner.enable.tooltip")
                )
                .booleanValue(
                        "deduplication",
                        Component.translatable("cfg.kineticcore.logcleaner.deduplication"),
                        () -> LogCleanerConfig.enableLogDeduplication,
                        value -> LogCleanerConfig.enableLogDeduplication = value,
                        true,
                        Component.translatable("cfg.kineticcore.logcleaner.deduplication.tooltip")
                )
                .stringValue(
                        "filtered_keywords",
                        Component.translatable("cfg.kineticcore.logcleaner.filtered_keywords"),
                        () -> LogCleanerConfig.rawFilteredKeywords,
                        value -> LogCleanerConfig.rawFilteredKeywords = value,
                        "Tried to load a block entity for block",
                        Component.translatable("cfg.kineticcore.logcleaner.filtered_keywords.tooltip")
                )
                .intValue(
                        "max_crash_reports",
                        Component.translatable("cfg.kineticcore.logcleaner.max_crash_reports"),
                        () -> LogCleanerConfig.maxCrashReports,
                        value -> LogCleanerConfig.maxCrashReports = value,
                        3,
                        1,
                        Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.logcleaner.max_crash_reports.tooltip")
                )
                .intValue(
                        "max_logs",
                        Component.translatable("cfg.kineticcore.logcleaner.max_logs"),
                        () -> LogCleanerConfig.maxLogs,
                        value -> LogCleanerConfig.maxLogs = value,
                        3,
                        1,
                        Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.logcleaner.max_logs.tooltip")
                )
                .intValue(
                        "max_debug_logs",
                        Component.translatable("cfg.kineticcore.logcleaner.max_debug_logs"),
                        () -> LogCleanerConfig.maxDebugLogs,
                        value -> LogCleanerConfig.maxDebugLogs = value,
                        3,
                        1,
                        Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.logcleaner.max_debug_logs.tooltip")
                )
                .onSave(LogCleanerConfig::save)
                .build();
    }
}
