package dev.xyat.kineticcore.api.resource.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.runtime.event.KineticResourceEventRuntime;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Objects;

public final class KineticResourceEvents {
    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

    public interface ReloadRegistrationContext {
        ReloadableServerResources serverResources();

        RegistryAccess registryAccess();

        void addListener(PreparableReloadListener listener);
    }

    @FunctionalInterface
    public interface ReloadRegistrationHandler {
        void handle(ReloadRegistrationContext context);
    }

    private KineticResourceEvents() {
    }

    public static HookRegistration onAddReloadListener(ReloadRegistrationHandler handler) {
        return onAddReloadListener(Priority.NORMAL, handler);
    }

    public static HookRegistration onAddReloadListener(Priority priority, ReloadRegistrationHandler handler) {
        return KineticResourceEventRuntime.register(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
