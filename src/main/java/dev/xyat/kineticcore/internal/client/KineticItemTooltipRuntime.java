package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticItemTooltipRuntime {
    private static final CopyOnWriteArrayList<KineticItemTooltips.Builder> BUILDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticItemTooltips.RenderObserver> RENDER_OBSERVERS = new CopyOnWriteArrayList<>();
    private static boolean initialized;

    private KineticItemTooltipRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        MinecraftForge.EVENT_BUS.addListener(KineticItemTooltipRuntime::onBuildTooltip);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, KineticItemTooltipRuntime::onRenderTooltip);
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
}
