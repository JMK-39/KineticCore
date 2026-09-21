package dev.xyat.kineticcore.api.client.tooltip;

import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.client.KineticItemTooltipRuntime;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Public Kinetic client API for item tooltips.
 */
public final class KineticItemTooltips {
    /** 注册文本 Tooltip 回调；各监听器独立执行，末尾汇总报告回调异常。 */
    @FunctionalInterface
    public interface Builder {
        /** Receives the displayed stack and the mutable text-line list for this tooltip build. */
        void build(ItemStack stack, List<Component> lines);
    }
    /** Observes an item immediately before its tooltip is rendered. */
    @FunctionalInterface
    public interface RenderObserver {
        /** Receives the stack whose tooltip is about to render; the callback does not own the stack. */
        void observe(ItemStack stack);
    }

    /** Context supplied to gather handlers dispatched by Kinetic Item Tooltips. */
    public interface GatherContext {
        /** Returns the stack associated with this gather pass. */
        ItemStack stack();

        /** Keeps only the first tooltip entry in the current gather pass. */
        void keepOnlyFirst();

        /** Appends one formatted-text element to the current tooltip. */
        void addText(FormattedText text);

        /** Appends one structured tooltip component to the current tooltip. */
        void addComponent(TooltipComponent component);
    }
    /** Contract used by Kinetic Item Tooltips for gatherer behavior. */
    @FunctionalInterface
    public interface Gatherer {
        /** Gathers the tooltip entries produced by the supplied callback. */
        void gather(GatherContext context);
    }

    private KineticItemTooltips() {
    }

    /** Returns the current client tooltip text lines for the supplied item stack. */
    public static List<Component> textLines(ItemStack stack) {
        return KineticItemTooltipRuntime.textLines(stack);
    }

    /** Registers a listener that may edit ordinary text lines during tooltip construction. */
    public static KineticEventSubscription onBuild(Builder listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticItemTooltipRuntime.registerBuild(listener);
    }

    /** 注册 Tooltip 绘制观察器；单个观察器异常不阻断后续观察器。 */
    public static KineticEventSubscription onRender(RenderObserver listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticItemTooltipRuntime.registerRender(listener);
    }

    /** 注册 Tooltip 元素收集回调；独立执行并在末尾报告异常。 */
    public static KineticEventSubscription onGather(Gatherer listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticItemTooltipRuntime.registerGather(listener);
    }

    /**
     * Registers the client renderer factory for one custom tooltip-component type during Forge's loading-phase factory-registration window.
     * Each component type has one owner and cannot be registered again through Kinetic.
     * 注册时逐项提交；某个工厂失败后仍尝试后续工厂，最后抛出首个异常并附带其他错误。
     *
     * @throws IllegalStateException if the component type already has a factory or the registration window has already closed
     */
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
