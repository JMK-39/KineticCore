package dev.xyat.kineticcore.feature.logcleaner;

import dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

public class DuplicateLogFilter extends AbstractFilter {
    private static boolean hasInjected = false;
    private static DuplicateLogFilter INSTANCE;

    private LogEvent bufferedEvent = null;
    private int repeatCount = 0;

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
        if (bufferedEvent == null) return;

        LogEvent eventToLog = bufferedEvent;
        if (repeatCount > 1) {
            String suffix = " [重复 " + repeatCount + " 次 / Repeated " + repeatCount + " times]";
            eventToLog = new Log4jLogEvent.Builder()
                    .setLoggerName(bufferedEvent.getLoggerName())
                    .setMarker(bufferedEvent.getMarker())
                    .setLoggerFqcn(bufferedEvent.getLoggerFqcn())
                    .setLevel(bufferedEvent.getLevel())
                    .setMessage(new SimpleMessage(bufferedEvent.getMessage().getFormattedMessage() + suffix))
                    .setThrown(bufferedEvent.getThrown())
                    .setContextStack(bufferedEvent.getContextStack())
                    .setThreadName(bufferedEvent.getThreadName())
                    .setSource(bufferedEvent.getSource())
                    .setTimeMillis(bufferedEvent.getTimeMillis())
                    .build();
        }

        isInjecting.set(true);
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(eventToLog.getLoggerName());
            loggerConfig.log(eventToLog);
        } catch (Exception ignored) {
        } finally {
            isInjecting.set(false);
        }

        bufferedEvent = null;
        repeatCount = 0;
    }

    @Override
    public Result filter(LogEvent event) {
        if (isInjecting.get()) return Result.NEUTRAL;
        if (event == null || event.getMessage() == null) return Result.NEUTRAL;

        String msg = event.getMessage().getFormattedMessage();
        if (msg == null) return Result.NEUTRAL;

        if (LogCleanerConfig.filteredKeywords != null) {
            for (String keyword : LogCleanerConfig.filteredKeywords) {
                if (!keyword.isEmpty() && msg.contains(keyword)) {
                    return Result.DENY;
                }
            }
        }

        if (!LogCleanerConfig.enableLogDeduplication) {
            return Result.NEUTRAL;
        }

        synchronized (this) {
            if (bufferedEvent != null && msg.equals(bufferedEvent.getMessage().getFormattedMessage())) {
                repeatCount++;
            } else {
                flush();
                bufferedEvent = event.toImmutable();
                repeatCount = 1;
            }
            return Result.DENY;
        }
    }
}