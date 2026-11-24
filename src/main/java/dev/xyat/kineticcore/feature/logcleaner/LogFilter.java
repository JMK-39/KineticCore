package dev.xyat.kineticcore.feature.logcleaner;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.List;

/** Filters by severity and keywords without suppressing or rewriting repeated events. */
public final class LogFilter extends AbstractFilter {
    /**
     * The lines launchers read on the console to follow a game, as vanilla's log calls start before their values are
     * filled in. PCL counts them (user set, LWJGL version, OpenAL loaded, textures loaded) to tell that a game has
     * finished loading and reports a game that ends before that as crashed; "Stopping!" tells a normal close from a
     * crash in an exported log. They pass when only errors are logged.
     */
    private static final String[] LAUNCHER_LINES = {"Setting user: ", "Backend library: ", "OpenAL initialized", "Starting up SoundSystem", "Stopping!"};
    /**
     * The lines PCL reads anywhere in a console line to tell a crash from a close: where the crash report went (the
     * game prints it through standard output, which reaches the log at INFO) and the close request of the window.
     */
    private static final String[] LAUNCHER_MARKERS = {"Crash report saved to", "This crash report has been saved to:", "Could not save crash report to",
            "Someone is closing me!"};
    private static final LogFilter INSTANCE = new LogFilter();
    private static final KineticRegistrationBatch INJECTION = new KineticRegistrationBatch();

    public static void inject() {
        INJECTION.runSequential(
                () -> loggerContext().getConfiguration().getRootLogger().addFilter(INSTANCE),
                () -> loggerContext().updateLoggers()
        );
    }

    private static LoggerContext loggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

    @Override
    public Result filter(LogEvent event) {
        if (event == null || event.getMessage() == null || event.getLevel() == null) return Result.NEUTRAL;
        // Keyword filtering below still applies to the launcher lines kept here.
        if (LogCleanerConfig.errorsOnly && !event.getLevel().isMoreSpecificThan(Level.ERROR) && !launcherLine(event)) {
            return Result.DENY;
        }
        String message = event.getMessage().getFormattedMessage();
        if (message == null) return Result.DENY;
        List<String> keywords = LogCleanerConfig.filteredKeywords;
        if (keywords != null) {
            for (String keyword : keywords) {
                if (!keyword.isEmpty() && message.contains(keyword)) return Result.DENY;
            }
        }
        return Result.NEUTRAL;
    }

    /** Whether launchers read this line; looks at the unformatted text only, so nothing is formatted for the check. */
    private static boolean launcherLine(LogEvent event) {
        if (event.getLevel() != Level.INFO) return false;
        String text = event.getMessage().getFormat();
        if (text == null || text.isEmpty()) return false;
        // "Created: 1024x512x4 minecraft:textures/atlas/blocks.png-atlas"
        if (text.startsWith("Created: ")) return text.endsWith("-atlas");
        for (String line : LAUNCHER_LINES) {
            if (text.startsWith(line)) return true;
        }
        for (String marker : LAUNCHER_MARKERS) {
            if (text.contains(marker)) return true;
        }
        return false;
    }
}
