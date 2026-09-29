package dev.xyat.kineticcore.api.resource.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticResourceEventRuntime;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Objects;

/** Public Kinetic API facade for resource events. */
public final class KineticResourceEvents {
    /** Server data is about to (re)load; add reload listeners for your own data files. */
    public interface ReloadRegistrationContext {
        /** Returns the server resources being loaded, for access to recipes and other managers. */
        ReloadableServerResources serverResources();

        /** Returns the registries available to the reload, including data-pack registries. */
        RegistryAccess registryAccess();

        /** Adds a listener that runs with the data-pack reload, after vanilla's own listeners. */
        void addListener(PreparableReloadListener listener);
    }

    /** Callback contract for reload registration notifications. */
    @FunctionalInterface
    public interface ReloadRegistrationHandler {
        /** Called on server start and on every {@code /reload}. */
        void handle(ReloadRegistrationContext context);
    }

    private KineticResourceEvents() {
    }

    /**
     * 注册资源重载监听器；单个注册回调失败不阻断其他回调，异常在分发结束后报告。
     *
     * <p>Subscribes to data reload listener registration ({@code AddReloadListenerEvent}). One failing callback
     * does not stop the others; failures are reported after dispatch.
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onAddReloadListener(KineticEventPriority priority, ReloadRegistrationHandler handler) {
        return KineticResourceEventRuntime.register(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
