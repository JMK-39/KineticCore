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
        if (LogCleanerConfig.errorsOnly && !event.getLevel().isMoreSpecificThan(Level.ERROR)) {
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
}
