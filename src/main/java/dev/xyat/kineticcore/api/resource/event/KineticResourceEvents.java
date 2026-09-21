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
    /** Context exposed to reload registration callbacks. */
    public interface ReloadRegistrationContext {
        ReloadableServerResources serverResources();

        RegistryAccess registryAccess();

        void addListener(PreparableReloadListener listener);
    }

    /** Callback contract for reload registration notifications. */
    @FunctionalInterface
    public interface ReloadRegistrationHandler {
        void handle(ReloadRegistrationContext context);
    }

    private KineticResourceEvents() {
    }

    /**
     * 注册资源重载监听器；单个注册回调失败不阻断其他回调，异常在分发结束后报告。
     */
    public static KineticEventSubscription onAddReloadListener(KineticEventPriority priority, ReloadRegistrationHandler handler) {
        return KineticResourceEventRuntime.register(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
