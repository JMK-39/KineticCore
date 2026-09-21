package dev.xyat.kineticcore.api.event;

import dev.xyat.kineticcore.internal.runtime.event.KineticExternalEventRuntime;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Bridges addon-defined external platform events without making addons register directly on the platform event bus.
 * The event class itself remains owned by the addon or third-party integration; Kinetic only owns subscription and posting.
 */
public final class KineticExternalEvents {
    private KineticExternalEvents() {
    }

    /**
     * Subscribes to one external event type using Kinetic's shared priority vocabulary.
     *
     * @param eventType concrete external event class
     * @param priority listener priority
     * @param receiveCancelled whether cancelled platform events should still be delivered
     * @param listener event callback
     * @return cancellable subscription handle
     */
    public static <T> KineticEventSubscription subscribe(
            Class<T> eventType,
            KineticEventPriority priority,
            boolean receiveCancelled,
            Consumer<? super T> listener
    ) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticExternalEventRuntime.subscribe(eventType, priority, receiveCancelled, listener);
    }

    /** Subscribes at normal priority without receiving cancelled events. */
    public static <T> KineticEventSubscription subscribe(Class<T> eventType, Consumer<? super T> listener) {
        return subscribe(eventType, KineticEventPriority.NORMAL, false, listener);
    }

    /**
     * Posts one addon-defined external event through the platform event bus and returns the same event instance.
     */
    public static <T> T post(T event) {
        return KineticExternalEventRuntime.post(Objects.requireNonNull(event, "event"));
    }
}
