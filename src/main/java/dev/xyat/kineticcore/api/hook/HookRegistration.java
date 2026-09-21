package dev.xyat.kineticcore.api.hook;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Public API contract for hook registration. */
@FunctionalInterface
public interface HookRegistration extends AutoCloseable {
    @Override
    void close();

    /** 创建仅执行一次清理操作的 Hook 句柄；重复关闭不会影响其他注册。 */
    static HookRegistration once(Runnable cleanup) {
        Objects.requireNonNull(cleanup, "cleanup");
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) cleanup.run();
        };
    }
}
