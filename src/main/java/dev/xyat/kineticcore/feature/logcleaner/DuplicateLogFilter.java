package dev.xyat.kineticcore.feature.logcleaner;

import dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.Objects;

public class DuplicateLogFilter extends AbstractFilter {
    private static boolean hasInjected = false;
    private static DuplicateLogFilter INSTANCE;

    private LogEvent lastEvent = null;
    private EventKey lastKey = null;
    private int suppressedCount = 0;

    private final ThreadLocal<Boolean> isInjecting = ThreadLocal.withInitial(() -> false);

    public DuplicateLogFilter() {
        INSTANCE = this;
    }

    public static void inject() {
        if (hasInjected) return;
        hasInjected = true;

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig rootConfig = config.getRootLogger();

        rootConfig.addFilter(new DuplicateLogFilter());
        ctx.updateLoggers();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (INSTANCE != null) INSTANCE.flush();
        }, "KineticCore-LogCleaner-Flusher-Shutdown"));
    }

    private synchronized void flush() {
        if (lastEvent == null || suppressedCount <= 0) return;

        String suffix = " [重复 " + suppressedCount + " 次 / Repeated " + suppressedCount + " additional times]";
        LogEvent summaryEvent = new Log4jLogEvent.Builder()
                .setLoggerName(lastEvent.getLoggerName())
                .setMarker(lastEvent.getMarker())
                .setLoggerFqcn(lastEvent.getLoggerFqcn())
                .setLevel(lastEvent.getLevel())
                .setMessage(new SimpleMessage(lastEvent.getMessage().getFormattedMessage() + suffix))
                .setContextStack(lastEvent.getContextStack())
                .setThreadName(lastEvent.getThreadName())
                .setSource(lastEvent.getSource())
                .setTimeMillis(System.currentTimeMillis())
                .build();

        isInjecting.set(true);
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(summaryEvent.getLoggerName());
            loggerConfig.log(summaryEvent);
        } catch (Exception ignored) {
        } finally {
            isInjecting.set(false);
        }

        suppressedCount = 0;
    }

    private synchronized void resetDeduplicationState() {
        lastEvent = null;
        lastKey = null;
        suppressedCount = 0;
    }

    @Override
    public Result filter(LogEvent event) {
        if (isInjecting.get()) return Result.NEUTRAL;
        if (event == null || event.getMessage() == null || event.getLevel() == null) return Result.NEUTRAL;
        if (!event.getLevel().isMoreSpecificThan(Level.ERROR)) return Result.DENY;

        String msg = event.getMessage().getFormattedMessage();
        if (msg == null) return Result.DENY;

        if (LogCleanerConfig.filteredKeywords != null) {
            for (String keyword : LogCleanerConfig.filteredKeywords) {
                if (!keyword.isEmpty() && msg.contains(keyword)) {
                    return Result.DENY;
                }
            }
        }

        if (!LogCleanerConfig.enableLogDeduplication) {
            flush();
            resetDeduplicationState();
            return Result.NEUTRAL;
        }

        EventKey key = EventKey.from(event, msg);
        synchronized (this) {
            if (lastEvent != null && key.equals(lastKey)) {
                suppressedCount++;
                return Result.DENY;
            }

            flush();
            lastEvent = event.toImmutable();
            lastKey = key;
            suppressedCount = 0;
            return Result.NEUTRAL;
        }
    }

    private record EventKey(
            String loggerName,
            Level level,
            String message,
            String thrownType,
            String thrownMessage,
            String thrownOrigin
    ) {
        private static EventKey from(LogEvent event, String message) {
            Throwable thrown = event.getThrown();
            if (thrown == null) {
                return new EventKey(event.getLoggerName(), event.getLevel(), message, "", "", "");
            }

            StackTraceElement[] stack = thrown.getStackTrace();
            String origin = stack.length > 0 ? stack[0].toString() : "";
            return new EventKey(
                    event.getLoggerName(),
                    event.getLevel(),
                    message,
                    thrown.getClass().getName(),
                    Objects.toString(thrown.getMessage(), ""),
                    origin
            );
        }
    }
}
