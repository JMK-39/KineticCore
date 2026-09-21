package dev.xyat.kineticcore.api.event;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cancellable handle returned by runtime Kinetic event subscriptions. */
@FunctionalInterface
public interface KineticEventSubscription extends AutoCloseable {
    @Override
    void close();

    /** 创建仅执行一次注销操作的订阅句柄；重复关闭不会取消其他同名监听注册。 */
    static KineticEventSubscription once(Runnable cleanup) {
        Objects.requireNonNull(cleanup, "cleanup");
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) cleanup.run();
        };
    }
}
