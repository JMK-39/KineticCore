package dev.xyat.kineticcore.api.client.tooltip;

import com.mojang.datafixers.util.Either;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.client.KineticItemTooltipRuntime;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class KineticItemTooltips {
    @FunctionalInterface
    public interface Builder {
        void build(ItemStack stack, List<Component> lines);
    }

    @FunctionalInterface
    public interface RenderObserver {
        void observe(ItemStack stack);
    }

    public interface GatherContext {
        ItemStack stack();

        List<Either<FormattedText, TooltipComponent>> elements();

        default void keepOnlyFirst() {
            if (elements().isEmpty()) return;
            Either<FormattedText, TooltipComponent> first = elements().get(0);
            elements().clear();
            elements().add(first);
        }

        default void addText(FormattedText text) {
            elements().add(Either.left(Objects.requireNonNull(text, "text")));
        }

        default void addComponent(TooltipComponent component) {
            elements().add(Either.right(Objects.requireNonNull(component, "component")));
        }
    }

    @FunctionalInterface
    public interface Gatherer {
        void gather(GatherContext context);
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

    public static HookRegistration onGather(Gatherer listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticItemTooltipRuntime.registerGather(listener);
    }

    public static <T extends TooltipComponent> void registerComponentFactory(
            Class<T> componentType,
            Function<T, ? extends ClientTooltipComponent> factory
    ) {
        KineticItemTooltipRuntime.registerComponentFactory(
                Objects.requireNonNull(componentType, "componentType"),
                Objects.requireNonNull(factory, "factory")
        );
    }
}
