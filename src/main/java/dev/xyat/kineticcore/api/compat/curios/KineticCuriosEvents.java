package dev.xyat.kineticcore.api.compat.curios;

import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.internal.compat.curios.KineticCuriosEventRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Public Kinetic API facade for curios events. */
public final class KineticCuriosEvents {
    /** Context exposed to change callbacks. */
    public interface ChangeContext {
        LivingEntity entity();

        String identifier();

        int slotIndex();

        ItemStack from();

        ItemStack to();
    }

    /** Callback contract for change notifications. */
    @FunctionalInterface
    public interface ChangeHandler {
        void handle(ChangeContext context);
    }

    private KineticCuriosEvents() {
    }

    /**
     * 注册 Curios 变更监听器；单个回调抛异常不阻断后续回调，错误仍会向上传递。
     */
    public static KineticEventSubscription onChange(ChangeHandler handler) {
        Objects.requireNonNull(handler, "handler");
        if (!KineticPlatform.isModLoaded("curios")) {
            return KineticEventSubscription.once(() -> {
            });
        }
        return KineticCuriosEventRuntime.registerChange(handler);
    }
}
