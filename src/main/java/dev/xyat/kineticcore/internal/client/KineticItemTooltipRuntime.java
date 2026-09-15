package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class KineticItemTooltipRuntime {
    private static final CopyOnWriteArrayList<KineticItemTooltips.Builder> BUILDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticItemTooltips.RenderObserver> RENDER_OBSERVERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticItemTooltips.Gatherer> GATHERERS = new CopyOnWriteArrayList<>();
    private static boolean initialized;

    private KineticItemTooltipRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        MinecraftForge.EVENT_BUS.addListener(KineticItemTooltipRuntime::onBuildTooltip);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, KineticItemTooltipRuntime::onRenderTooltip);
        MinecraftForge.EVENT_BUS.addListener(KineticItemTooltipRuntime::onGatherTooltip);
    }

    public static HookRegistration registerBuild(KineticItemTooltips.Builder listener) {
        initialize();
        BUILDERS.add(listener);
        return () -> BUILDERS.remove(listener);
    }

    public static HookRegistration registerRender(KineticItemTooltips.RenderObserver listener) {
        initialize();
        RENDER_OBSERVERS.add(listener);
        return () -> RENDER_OBSERVERS.remove(listener);
    }

    public static HookRegistration registerGather(KineticItemTooltips.Gatherer listener) {
        initialize();
        GATHERERS.add(listener);
        return () -> GATHERERS.remove(listener);
    }

    public static <T extends TooltipComponent> void registerComponentFactory(
            Class<T> componentType,
            Function<T, ? extends ClientTooltipComponent> factory
    ) {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (RegisterClientTooltipComponentFactoriesEvent event) ->
                        event.register(componentType, value -> factory.apply(value))
        );
    }

    private static void onBuildTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        for (KineticItemTooltips.Builder listener : BUILDERS) {
            listener.build(stack, event.getToolTip());
        }
    }

    private static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        for (KineticItemTooltips.RenderObserver listener : RENDER_OBSERVERS) {
            listener.observe(stack);
        }
    }

    private static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        GatherContextImpl context = new GatherContextImpl(event);
        for (KineticItemTooltips.Gatherer listener : GATHERERS) {
            listener.gather(context);
        }
    }

    private record GatherContextImpl(RenderTooltipEvent.GatherComponents event) implements KineticItemTooltips.GatherContext {
        @Override
        public ItemStack stack() {
            return event.getItemStack();
        }

        @Override
        public java.util.List<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText, TooltipComponent>> elements() {
            return event.getTooltipElements();
        }
    }
}
