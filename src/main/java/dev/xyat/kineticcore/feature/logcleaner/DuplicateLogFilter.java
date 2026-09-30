package dev.xyat.kineticcore.feature.logcleaner;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
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
    private static final DuplicateLogFilter INSTANCE = new DuplicateLogFilter();
    private static final KineticRegistrationBatch INJECTION = new KineticRegistrationBatch();

    private LogEvent lastEvent = null;
    private final ConsecutiveLogState<EventKey> state = new ConsecutiveLogState<>();

    private final ThreadLocal<Boolean> isInjecting = ThreadLocal.withInitial(() -> false);

    public DuplicateLogFilter() {
    }

    public static void inject() {
        INJECTION.runSequential(
                () -> rootLoggerConfig().addFilter(INSTANCE),
                () -> loggerContext().updateLoggers(),
                () -> Runtime.getRuntime().addShutdownHook(new Thread(
                        INSTANCE::flush,
                        "KineticCore-LogCleaner-Flusher-Shutdown"
                ))
        );
    }

    private static LoggerContext loggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

    private static LoggerConfig rootLoggerConfig() {
        Configuration config = loggerContext().getConfiguration();
        return config.getRootLogger();
    }

    private synchronized void flush() {
        publishSummary(state.drain());
        lastEvent = null;
    }

    private synchronized void resetDeduplicationState() {
        lastEvent = null;
        state.drain();
    }

    @Override
    public Result filter(LogEvent event) {
        if (isInjecting.get()) return Result.NEUTRAL;
        if (event == null || event.getMessage() == null || event.getLevel() == null) return Result.NEUTRAL;
        if (!event.getLevel().isMoreSpecificThan(Level.ERROR)) {
            flush();
            return Result.DENY;
        }

        String msg = event.getMessage().getFormattedMessage();
        if (msg == null) return Result.DENY;

        if (LogCleanerConfig.filteredKeywords != null) {
            for (String keyword : LogCleanerConfig.filteredKeywords) {
                if (!keyword.isEmpty() && msg.contains(keyword)) {
                    flush();
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
            int repetitions = state.accept(key);
            if (repetitions < 0) return Result.DENY;
            if (repetitions > 0) publishSummary(repetitions);
            lastEvent = event.toImmutable();
            return Result.NEUTRAL;
        }
    }

    private void publishSummary(int repetitions) {
        if (lastEvent == null || repetitions <= 0) return;
        LogEvent summaryEvent = new Log4jLogEvent.Builder()
                .setLoggerName(lastEvent.getLoggerName())
                .setMarker(lastEvent.getMarker())
                .setLoggerFqcn(lastEvent.getLoggerFqcn())
                .setLevel(lastEvent.getLevel())
                .setMessage(new SimpleMessage("上一条日志重复 " + repetitions
                        + " 次 / Previous log repeated " + repetitions + " additional times"))
                .setContextStack(lastEvent.getContextStack())
                .setThreadName(lastEvent.getThreadName())
                .setSource(lastEvent.getSource())
                .setTimeMillis(System.currentTimeMillis())
                .build();
        isInjecting.set(true);
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.getConfiguration().getLoggerConfig(summaryEvent.getLoggerName()).log(summaryEvent);
        } catch (Exception ignored) {
        } finally {
            isInjecting.set(false);
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
