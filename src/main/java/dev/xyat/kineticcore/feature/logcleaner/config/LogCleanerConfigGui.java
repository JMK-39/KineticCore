package dev.xyat.kineticcore.feature.logcleaner.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import net.minecraft.client.gui.screens.Screen;

public final class LogCleanerConfigGui {
    public static final String PAGE_ID = "kineticcore:log_cleaner";

    private LogCleanerConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(buildPage());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createRegisteredPageScreen(parent, PAGE_ID);
    }

    private static KTConfigPage buildPage() {
        return KTConfigPage.builder(PAGE_ID, KineticI18n.translatable("cfg.kineticcore.logcleaner.title"))
                .scope(KTConfigScope.LOCAL_INSTALLATION)
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.logcleaner.description"))
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .booleanValue(
                        "enable",
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.enable"),
                        () -> LogCleanerConfig.enableCleanup,
                        value -> LogCleanerConfig.enableCleanup = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.enable.tooltip")
                )
                .booleanValue(
                        "deduplication",
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.deduplication"),
                        () -> LogCleanerConfig.enableLogDeduplication,
                        value -> LogCleanerConfig.enableLogDeduplication = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.deduplication.tooltip")
                )
                .longTextValue(
                        "filtered_keywords",
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.filtered_keywords"),
                        () -> LogCleanerConfig.rawFilteredKeywords,
                        value -> LogCleanerConfig.rawFilteredKeywords = value,
                        "Tried to load a block entity for block",
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.filtered_keywords.tooltip")
                )
                .intValue(
                        "max_crash_reports",
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.max_crash_reports"),
                        () -> LogCleanerConfig.maxCrashReports,
                        value -> LogCleanerConfig.maxCrashReports = value,
                        3,
                        1,
                        Integer.MAX_VALUE,
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.max_crash_reports.tooltip")
                )
                .intValue(
                        "max_logs",
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.max_logs"),
                        () -> LogCleanerConfig.maxLogs,
                        value -> LogCleanerConfig.maxLogs = value,
                        3,
                        1,
                        Integer.MAX_VALUE,
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.max_logs.tooltip")
                )
                .intValue(
                        "max_debug_logs",
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.max_debug_logs"),
                        () -> LogCleanerConfig.maxDebugLogs,
                        value -> LogCleanerConfig.maxDebugLogs = value,
                        3,
                        1,
                        Integer.MAX_VALUE,
                        KineticI18n.translatable("cfg.kineticcore.logcleaner.max_debug_logs.tooltip")
                )
                .onSave(LogCleanerConfig::save)
                .build();
    }
}
