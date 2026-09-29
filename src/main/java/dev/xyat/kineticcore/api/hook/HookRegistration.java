package dev.xyat.kineticcore.api.hook;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Handle returned by hook registration. Closing it unregisters the hook; closing twice is harmless. */
@FunctionalInterface
public interface HookRegistration extends AutoCloseable {
    /** Unregisters the hook. Does not throw checked exceptions. */
    @Override
    void close();

    /**
     * Creates a handle that runs {@code cleanup} only the first time it is closed, even when closed from several
     * threads.
     *
     * @param cleanup unregistration action
     * @return the handle
     * @throws NullPointerException if {@code cleanup} is {@code null}
     */
    static HookRegistration once(Runnable cleanup) {
        Objects.requireNonNull(cleanup, "cleanup");
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) cleanup.run();
        };
    }
}
