package dev.xyat.kineticcore.api.compat.curios;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.compat.curios.KineticCuriosEventRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class KineticCuriosEvents {
    public interface ChangeContext {
        LivingEntity entity();

        String identifier();

        int slotIndex();

        ItemStack from();

        ItemStack to();
    }

    @FunctionalInterface
    public interface ChangeHandler {
        void handle(ChangeContext context);
    }

    private KineticCuriosEvents() {
    }

    public static HookRegistration onChange(ChangeHandler handler) {
        return KineticCuriosEventRuntime.registerChange(Objects.requireNonNull(handler, "handler"));
    }
}
