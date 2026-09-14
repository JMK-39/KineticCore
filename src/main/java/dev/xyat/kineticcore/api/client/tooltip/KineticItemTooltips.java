package dev.xyat.kineticcore.api.client.tooltip;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.client.KineticItemTooltipRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

public final class KineticItemTooltips {
    @FunctionalInterface
    public interface Builder {
        void build(ItemStack stack, List<Component> lines);
    }

    @FunctionalInterface
    public interface RenderObserver {
        void observe(ItemStack stack);
    }

    private KineticItemTooltips() {
    }

    public static HookRegistration onBuild(Builder listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticItemTooltipRuntime.registerBuild(listener);
    }

    public static HookRegistration onRender(RenderObserver listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticItemTooltipRuntime.registerRender(listener);
    }
}
