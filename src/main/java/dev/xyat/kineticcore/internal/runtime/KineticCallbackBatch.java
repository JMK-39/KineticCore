package dev.xyat.kineticcore.internal.runtime;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/**
 * Dispatches every independent callback even when another callback fails. The first
 * exception remains primary and later distinct failures are attached as suppressed.
 */
public final class KineticCallbackBatch {
    private KineticCallbackBatch() {
    }

    public static <T> void runAll(Iterable<? extends T> handlers, Consumer<? super T> callback) {
        Objects.requireNonNull(handlers, "handlers");
        Objects.requireNonNull(callback, "callback");
        RuntimeException failure = null;
        for (T handler : handlers) {
            try {
                callback.accept(handler);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    /**
     * Runs cancellable callbacks in registration order, retaining the original
     * stop-after-cancellation rule. One faulty callback does not prevent other
     * callbacks from running while the event remains uncancelled; the first
     * RuntimeException is reported after dispatch, with later failures suppressed.
     * Even an already-cancelled event invokes its first callback, matching the
     * existing per-priority event dispatch behavior.
     */
    public static <T> void runUntilCancelled(
            Iterable<? extends T> handlers,
            Consumer<? super T> callback,
            BooleanSupplier cancelled
    ) {
        Objects.requireNonNull(handlers, "handlers");
        Objects.requireNonNull(callback, "callback");
        Objects.requireNonNull(cancelled, "cancelled");
        RuntimeException failure = null;
        for (T handler : handlers) {
            try {
                callback.accept(handler);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
            if (cancelled.getAsBoolean()) break;
        }
        if (failure != null) throw failure;
    }

}
