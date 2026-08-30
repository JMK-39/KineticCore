package dev.xyat.kineticcore.feature.datapack;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackErrorAppender extends AbstractAppender {
    private static boolean registered = false;
    // 匹配 ID 的正则
    private static final Pattern ID_PATTERN = Pattern.compile("([a-z0-9_.-]+):([a-z0-9_.-/]+)");
    // 匹配致命错误列表开头的正则 (例如 ]: [philipsruins:ancient_ruin )
    private static final Pattern FATAL_PATTERN = Pattern.compile("]:\\s*\\[([a-z0-9_.-]+):");
    private static boolean FOUND_FATAL = false;

    public PackErrorAppender() {
        super("KTPackErrorAppender", null, null, true, Property.EMPTY_ARRAY);
    }

    public static void register() {
        if (registered) return;
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        PackErrorAppender appender = new PackErrorAppender();
        appender.start();
        ctx.getRootLogger().addAppender(appender);
        ctx.updateLoggers();
        registered = true;
    }

    @Override
    public void append(LogEvent event) {
        if (FOUND_FATAL) return; // 已经抓到最致命的了，直接退出

        String logger = event.getLoggerName() != null ? event.getLoggerName() : "";
        String msg = event.getMessage() != null ? event.getMessage().getFormattedMessage() : "";
        Throwable thrown = event.getThrown();

        // 1. 【核心逻辑】优先检测致命错误 (Unbound values)
        // 这种错误有时在日志正文，有时在 Throwable 的 stacktrace 里
        if (tryCaptureFatal(msg) || tryCaptureFatalFromThrowable(thrown)) {
            FOUND_FATAL = true; // 锁定状态，不再接受任何其他 ID
            return;
        }

        // 2. 【兜底逻辑】如果是普通 ERROR 且目前还没抓到任何 ID，才记录普通 ID
        // 屏蔽噪音
        if (logger.contains("ModernFix") || logger.contains("mixin") || msg.contains("KubeJS")) return;

        if (PackModule.LAST_ERROR_ID == null && event.getLevel().isMoreSpecificThan(Level.ERROR)) {
            // 仅关注原版资源加载系统报出的错误
            if (logger.contains("RecipeManager") || logger.contains("TagLoader") ||
                    logger.contains("LootDataManager") || logger.contains("ModelBakery") ||
                    logger.contains("MappedRegistry") || logger.contains("RegistryDataLoader")) {

                Matcher m = ID_PATTERN.matcher(msg);
                if (m.find()) {
                    String ns = m.group(1);
                    if (isValid(ns)) {
                        PackModule.LAST_ERROR_ID = ns;
                    }
                }
            }
        }
    }

    private boolean tryCaptureFatal(String text) {
        if (text == null) return false;
        if (text.contains("Unbound values in registry")) {
            Matcher m = FATAL_PATTERN.matcher(text);
            if (m.find()) {
                String id = m.group(1).trim();
                if (isValid(id)) {
                    PackModule.LAST_ERROR_ID = id;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryCaptureFatalFromThrowable(Throwable t) {
        while (t != null) {
            if (tryCaptureFatal(t.getMessage())) return true;
            t = t.getCause(); // 递归查找原因链
        }
        return false;
    }

    private boolean isValid(String ns) {
        return !ns.equals("minecraft") && !ns.equals("forge") && !ns.endsWith(".json") && !ns.contains("mixin");
    }
}