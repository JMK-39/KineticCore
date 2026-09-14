package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;
import dev.xyat.kineticcore.internal.client.gpu.GpuMemLeakFixHandler;
import dev.xyat.kineticcore.internal.client.input.KineticKeyBindingRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

public final class KineticClientRuntimeImpl {
    private static boolean initialized;

    private KineticClientRuntimeImpl() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        KineticKeyBindingRuntime.initialize();
        KineticClientEventRuntime.initialize();
        KineticItemTooltipRuntime.initialize();
        ServerConfigClientRuntime.initialize();
        MinecraftForge.EVENT_BUS.addListener(GuiOverlayBridge::onRenderGui);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GuiOverlayBridge::onRenderScreenPre);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, GuiOverlayBridge::onRenderScreenPost);
        MinecraftForge.EVENT_BUS.addListener(GuiSessionBridge::onScreenOpening);
        MinecraftForge.EVENT_BUS.addListener(GuiSessionBridge::onPlainScreenEscape);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, KineticEffectDisplayBridge::onInventoryEffects);
        GpuMemLeakFixHandler.register();
    }

    public static void execute(Runnable action) {
        if (action == null) return;
        Minecraft.getInstance().execute(action);
    }

    public static Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    public static void openScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }
}
