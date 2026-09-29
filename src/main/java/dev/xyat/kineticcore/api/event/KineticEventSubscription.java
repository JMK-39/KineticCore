package dev.xyat.kineticcore.api.event;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Handle returned by Kinetic event subscriptions. Closing it unsubscribes; closing twice is harmless. */
@FunctionalInterface
public interface KineticEventSubscription extends AutoCloseable {
    /** Unsubscribes the handler. Does not throw checked exceptions. */
    @Override
    void close();

    /**
     * Creates a handle that runs {@code cleanup} only the first time it is closed, even when closed from several
     * threads.
     *
     * @param cleanup unsubscription action
     * @return the handle
     * @throws NullPointerException if {@code cleanup} is {@code null}
     */
    static KineticEventSubscription once(Runnable cleanup) {
        Objects.requireNonNull(cleanup, "cleanup");
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) cleanup.run();
        };
    }
}
