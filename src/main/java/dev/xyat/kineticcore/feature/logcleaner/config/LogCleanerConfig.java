package dev.xyat.kineticcore.feature.logcleaner.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticcore.KineticCore;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LogCleanerConfig {
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("kineticcore");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("log_cleaner.toml");
    private static CommentedFileConfig configData;

    public static boolean enableCleanup = true;
    public static boolean enableLogDeduplication = true;
    public static int maxCrashReports = 3;
    public static int maxLogs = 3;
    public static int maxDebugLogs = 3;

    public static String rawFilteredKeywords = "Tried to load a block entity for block";
    public static List<String> filteredKeywords = new ArrayList<>();

    public static void load() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build();
            configData.load();
            setupConfig();
            configData.save();
            readValues();
        } catch (Exception e) {
            KineticCore.LOGGER.error("LogCleanerConfig Load Failed", e);
        }
    }

    private static void setupConfig() {
        configData.setComment("log_cleaner", "日志与崩溃报告自动清理系统\nLog & Crash Report Auto-Cleaner");

        define("log_cleaner.enable", true,
                "是否在游戏关闭时自动在后台清理旧的日志和崩溃报告。\nWhether to auto-clean old logs and crash reports asynchronously on game shutdown.");

        define("log_cleaner.deduplication", true,
                "是否开启控制台与文件日志的自动去重功能。连续重复的日志将在末尾加上 *2, *3 等。\nWhether to enable log deduplication. Repeated consecutive logs will be appended with *2, *3, etc.");

        define("log_cleaner.filtered_keywords", "Tried to load a block entity for block",
                """
                        包含以下关键词的日志将被彻底屏蔽（不在控制台输出，也不写入日志文件）。
                        支持配置多个关键词，请务必使用英文逗号 [,] 进行分隔！
                        示例 (多个过滤词): "Tried to load a block entity,Connection reset,Another boring log"
                        Logs containing these keywords will be completely hidden.
                        You can configure multiple keywords, please use English commas [,] to separate them!""");

        define("log_cleaner.max_crash_reports", 3,
                "最大保留的崩溃报告数量 (设定的值不可低于 1)。\nMax amount of crash reports to keep (minimum value is 1).");

        define("log_cleaner.max_logs", 3,
                "最大保留的旧普通日志数量 (如 2024-xx-xx.log.gz，不包括 latest.log)。\nMax amount of old regular logs to keep (excluding latest.log).");

        define("log_cleaner.max_debug_logs", 3,
                "最大保留的旧调试日志数量 (如 debug-x.log.gz，不包括 debug.log)。\nMax amount of old debug logs to keep (excluding debug.log).");
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) configData.set(path, def);
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enableCleanup = configData.getOrElse("log_cleaner.enable", true);
        enableLogDeduplication = configData.getOrElse("log_cleaner.deduplication", true);

        rawFilteredKeywords = configData.getOrElse("log_cleaner.filtered_keywords", "Tried to load a block entity for block");
        filteredKeywords.clear();
        if (rawFilteredKeywords != null && !rawFilteredKeywords.isEmpty()) {
            String[] split = rawFilteredKeywords.split(",");
            for (String s : split) {
                if (!s.trim().isEmpty()) {
                    filteredKeywords.add(s.trim());
                }
            }
        }

        maxCrashReports = Math.max(1, configData.getIntOrElse("log_cleaner.max_crash_reports", 3));
        maxLogs = Math.max(1, configData.getIntOrElse("log_cleaner.max_logs", 3));
        maxDebugLogs = Math.max(1, configData.getIntOrElse("log_cleaner.max_debug_logs", 3));
    }

    public static void save() {
        if (configData == null) return;
        configData.set("log_cleaner.enable", enableCleanup);
        configData.set("log_cleaner.deduplication", enableLogDeduplication);
        configData.set("log_cleaner.filtered_keywords", rawFilteredKeywords);
        configData.set("log_cleaner.max_crash_reports", Math.max(1, maxCrashReports));
        configData.set("log_cleaner.max_logs", Math.max(1, maxLogs));
        configData.set("log_cleaner.max_debug_logs", Math.max(1, maxDebugLogs));
        configData.save();
        readValues();
    }
}