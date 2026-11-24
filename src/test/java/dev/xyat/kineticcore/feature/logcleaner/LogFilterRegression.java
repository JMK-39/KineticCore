package dev.xyat.kineticcore.feature.logcleaner;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/** Runs in an isolated working directory; no game configuration is changed. */
public final class LogFilterRegression {
    public static void main(String[] args) throws Exception {
        FMLPaths.loadAbsolutePaths(Path.of("."));
        Path old = KineticPlatform.configDirectory().resolve("kineticcore/log_cleaner.toml");
        Path config = KineticPlatform.configDirectory().resolve("kineticcore/core/log_cleaner.toml");
        Files.createDirectories(old.getParent());
        Files.writeString(old, """
                [log_cleaner]
                enable = false
                errors_only = false
                deduplication = true
                filtered_keywords = "hidden noise, another ignored line"
                max_crash_reports = 7
                max_logs = 9
                max_debug_logs = 11
                """);
        LogCleanerConfig.load();
        check(!Files.exists(old) && Files.isRegularFile(config), "legacy log config is moved before loading");
        LogFilter filter = new LogFilter();
        LogEvent repeated = event(Level.INFO, "visible repeated message");
        for (int i = 0; i < 5; i++) {
            check(filter.filter(repeated) == Filter.Result.NEUTRAL,
                    "every repeated message must pass, including repetition " + i);
        }
        check(filter.filter(event(Level.ERROR, "hidden noise")) == Filter.Result.DENY,
                "keyword filtering must still hide matching errors");
        check(filter.filter(event(Level.INFO, "another ignored line")) == Filter.Result.DENY,
                "comma-separated keyword filtering must be preserved");
        LogCleanerConfig.errorsOnly = true;
        check(filter.filter(event(Level.INFO, "visible")) == Filter.Result.DENY, "errors-only hides INFO");
        check(filter.filter(event(Level.WARN, "visible")) == Filter.Result.DENY, "errors-only hides WARN");
        // The lines a launcher reads to follow the game stay, in the form the game logs them.
        for (String line : new String[] {"Setting user: Dev", "Backend library: LWJGL version 3.3.1 build 7", "OpenAL initialized on device OpenAL Soft",
                "Created: 1024x512x4 minecraft:textures/atlas/blocks.png-atlas", "Stopping!",
                "[net.minecraft.server.Bootstrap:m_135875_:151]: #@!@# Game crashed! Crash report saved to: #@!@# crash-reports/crash.txt"}) {
            check(filter.filter(event(Level.INFO, line)) == Filter.Result.NEUTRAL, "errors-only keeps what launchers read: " + line);
        }
        check(filter.filter(event(Level.INFO, "Created: a world")) == Filter.Result.DENY, "errors-only keeps only the atlas lines of Created:");
        check(filter.filter(event(Level.WARN, "Stopping!")) == Filter.Result.DENY, "errors-only keeps launcher lines at INFO only");
        check(filter.filter(event(Level.INFO, "Not Stopping!")) == Filter.Result.DENY, "errors-only matches launcher lines from their start");
        check(filter.filter(event(Level.INFO, "Stopping! hidden noise")) == Filter.Result.DENY, "keyword filtering still applies to launcher lines");
        for (Level level : new Level[] { Level.ERROR, Level.FATAL }) {
            LogEvent error = event(level, "visible error");
            for (int i = 0; i < 3; i++) {
                check(filter.filter(error) == Filter.Result.NEUTRAL, "every " + level + " is retained");
            }
        }
        check(filter.filter(null) == Filter.Result.NEUTRAL, "a missing event is safe");
        check(!LogCleanerConfig.enableCleanup && LogCleanerConfig.maxCrashReports == 7
                        && LogCleanerConfig.maxLogs == 9 && LogCleanerConfig.maxDebugLogs == 11,
                "loading preserves cleanup settings");
        verifyPersistedConfig(config, false);
        LogCleanerConfig.save();
        verifyPersistedConfig(config, true);
        System.out.println("PASS: repeated logs, keyword/level filtering and obsolete config migration");
    }

    private static void verifyPersistedConfig(Path path, boolean errorsOnly) {
        try (CommentedFileConfig config = CommentedFileConfig.of(path)) {
            config.load();
            check(!config.contains("log_cleaner.deduplication"), "obsolete deduplication is removed on load/save");
            check(Boolean.FALSE.equals(config.get("log_cleaner.enable")), "cleanup choice is preserved");
            check(Boolean.valueOf(errorsOnly).equals(config.get("log_cleaner.errors_only")), "error mode is saved");
            check("hidden noise, another ignored line".equals(config.get("log_cleaner.filtered_keywords")),
                    "keyword text is preserved");
            check(config.getInt("log_cleaner.max_logs") == 9, "retention choice is preserved");
        }
    }

    private static LogEvent event(Level level, String message) {
        return new Log4jLogEvent.Builder().setLoggerName("regression").setLevel(level)
                .setMessage(new SimpleMessage(message)).build();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
