package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;
import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.resource.event.KineticResourceEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticResourceEventRuntime {
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler>> LISTENERS = buckets();
    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;

    private KineticResourceEventRuntime() {
    }

    public static KineticEventSubscription register(KineticEventPriority priority, KineticResourceEvents.ReloadRegistrationHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler> bucket = LISTENERS.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    private static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(toForge(priority), (AddReloadListenerEvent event) -> dispatch(priority, event)));
        }
        attempt.finish();
        initialized = true;
    }

    private static void dispatch(KineticEventPriority priority, AddReloadListenerEvent event) {
        Context context = new Context(event);
        KineticCallbackBatch.runAll(LISTENERS.get(priority), handler -> handler.handle(context));
    }

    private record Context(AddReloadListenerEvent event) implements KineticResourceEvents.ReloadRegistrationContext {
        @Override
        public net.minecraft.server.ReloadableServerResources serverResources() {
            return event.getServerResources();
        }

        @Override
        public net.minecraft.core.RegistryAccess registryAccess() {
            return event.getRegistryAccess();
        }

        @Override
        public void addListener(net.minecraft.server.packs.resources.PreparableReloadListener listener) {
            event.addListener(listener);
        }
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

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler>> buckets() {
        EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler>> result = new EnumMap<>(KineticEventPriority.class);
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
