package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.resource.event.KineticResourceEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticResourceEventRuntime {
    private static final EnumMap<KineticResourceEvents.Priority, CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler>> LISTENERS = buckets();
    private static boolean initialized;

    private KineticResourceEventRuntime() {
    }

    public static HookRegistration register(KineticResourceEvents.Priority priority, KineticResourceEvents.ReloadRegistrationHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler> bucket = LISTENERS.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        for (KineticResourceEvents.Priority priority : KineticResourceEvents.Priority.values()) {
            MinecraftForge.EVENT_BUS.addListener(toForge(priority), (AddReloadListenerEvent event) -> dispatch(priority, event));
        }
    }

    private static void dispatch(KineticResourceEvents.Priority priority, AddReloadListenerEvent event) {
        Context context = new Context(event);
        for (KineticResourceEvents.ReloadRegistrationHandler handler : LISTENERS.get(priority)) {
            handler.handle(context);
        }
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

    private static EventPriority toForge(KineticResourceEvents.Priority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private static EnumMap<KineticResourceEvents.Priority, CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler>> buckets() {
        EnumMap<KineticResourceEvents.Priority, CopyOnWriteArrayList<KineticResourceEvents.ReloadRegistrationHandler>> result = new EnumMap<>(KineticResourceEvents.Priority.class);
        for (KineticResourceEvents.Priority priority : KineticResourceEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
