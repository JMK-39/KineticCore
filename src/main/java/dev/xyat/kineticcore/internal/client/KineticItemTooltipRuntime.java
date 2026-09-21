package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;
import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class KineticItemTooltipRuntime {
    private static final CopyOnWriteArrayList<KineticItemTooltips.Builder> BUILDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticItemTooltips.RenderObserver> RENDER_OBSERVERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticItemTooltips.Gatherer> GATHERERS = new CopyOnWriteArrayList<>();
    private static final Map<Class<? extends TooltipComponent>, Function<TooltipComponent, ? extends ClientTooltipComponent>> COMPONENT_FACTORIES = new LinkedHashMap<>();
    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;
    private static boolean componentFactoryRegistrationClosed;

    private KineticItemTooltipRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticItemTooltipRuntime::onBuildTooltip));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, KineticItemTooltipRuntime::onRenderTooltip));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticItemTooltipRuntime::onGatherTooltip));
        attempt.install(slot++, () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticItemTooltipRuntime::onRegisterTooltipComponentFactories));
        attempt.finish();
        initialized = true;
    }

    public static List<Component> textLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        return List.copyOf(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
    }

    public static KineticEventSubscription registerBuild(KineticItemTooltips.Builder listener) {
        initialize();
        BUILDERS.add(listener);
        return KineticEventSubscription.once(() -> BUILDERS.remove(listener));
    }

    public static KineticEventSubscription registerRender(KineticItemTooltips.RenderObserver listener) {
        initialize();
        RENDER_OBSERVERS.add(listener);
        return KineticEventSubscription.once(() -> RENDER_OBSERVERS.remove(listener));
    }

    public static KineticEventSubscription registerGather(KineticItemTooltips.Gatherer listener) {
        initialize();
        GATHERERS.add(listener);
        return KineticEventSubscription.once(() -> GATHERERS.remove(listener));
    }

    public static synchronized <T extends TooltipComponent> void registerComponentFactory(
            Class<T> componentType,
            Function<T, ? extends ClientTooltipComponent> factory
    ) {
        initialize();
        if (componentFactoryRegistrationClosed) {
            throw new IllegalStateException("Tooltip component-factory registration window has already closed");
        }
        if (COMPONENT_FACTORIES.containsKey(componentType)) {
            throw new IllegalStateException("Tooltip component factory already registered: " + componentType.getName());
        }
        COMPONENT_FACTORIES.put(componentType, value -> factory.apply(componentType.cast(value)));
    }

    private static synchronized void onRegisterTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        try {
            KineticCallbackBatch.runAll(COMPONENT_FACTORIES.entrySet(),
                    entry -> registerComponentFactory(event, entry.getKey(), entry.getValue()));
        } finally {
            componentFactoryRegistrationClosed = true;
            COMPONENT_FACTORIES.clear();
        }
    }

    private static <T extends TooltipComponent> void registerComponentFactory(
            RegisterClientTooltipComponentFactoriesEvent event,
            Class<T> componentType,
            Function<TooltipComponent, ? extends ClientTooltipComponent> factory
    ) {
        event.register(componentType, value -> factory.apply(value));
    }

    private static void onBuildTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        KineticCallbackBatch.runAll(BUILDERS, listener -> listener.build(stack, event.getToolTip()));
    }

    private static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        KineticCallbackBatch.runAll(RENDER_OBSERVERS, listener -> listener.observe(stack));
    }

    private static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        GatherContextImpl context = new GatherContextImpl(event);
        KineticCallbackBatch.runAll(GATHERERS, listener -> listener.gather(context));
    }

    private record GatherContextImpl(RenderTooltipEvent.GatherComponents event) implements KineticItemTooltips.GatherContext {
        @Override
        public ItemStack stack() {
            return event.getItemStack();
        }

        @Override
        public void keepOnlyFirst() {
            var elements = event.getTooltipElements();
            if (elements.isEmpty()) return;
            var first = elements.get(0);
            elements.clear();
            elements.add(first);
        }

        @Override
        public void addText(net.minecraft.network.chat.FormattedText text) {
            event.getTooltipElements().add(com.mojang.datafixers.util.Either.left(java.util.Objects.requireNonNull(text, "text")));
        }

        @Override
        public void addComponent(TooltipComponent component) {
            event.getTooltipElements().add(com.mojang.datafixers.util.Either.right(java.util.Objects.requireNonNull(component, "component")));
        }
    }
}
