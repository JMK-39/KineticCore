package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.Objects;
import java.util.function.Consumer;

public final class KineticExternalEventRuntime {
    private KineticExternalEventRuntime() {
    }

    public static <T> KineticEventSubscription subscribe(
            Class<T> eventType,
            KineticEventPriority priority,
            boolean receiveCancelled,
            Consumer<? super T> listener
    ) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        if (!Event.class.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException("External event type must extend Forge Event: " + eventType.getName());
        }

        @SuppressWarnings("unchecked")
        Class<? extends Event> forgeType = (Class<? extends Event>) eventType;
        Consumer<Event> bridge = event -> listener.accept(eventType.cast(event));
        register(forgeType, toForge(priority), receiveCancelled, bridge);
        return KineticEventSubscription.once(() -> MinecraftForge.EVENT_BUS.unregister(bridge));
    }

    public static <T> T post(T event) {
        Objects.requireNonNull(event, "event");
        if (!(event instanceof Event forgeEvent)) {
            throw new IllegalArgumentException("External event must extend Forge Event: " + event.getClass().getName());
        }
        MinecraftForge.EVENT_BUS.post(forgeEvent);
        return event;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(
            Class<? extends Event> eventType,
            EventPriority priority,
            boolean receiveCancelled,
            Consumer<Event> listener
    ) {
        MinecraftForge.EVENT_BUS.addListener(priority, receiveCancelled, (Class) eventType, (Consumer) listener);
    }

    private static EventPriority toForge(KineticEventPriority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }
}
