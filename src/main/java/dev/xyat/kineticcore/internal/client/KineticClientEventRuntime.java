package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticClientEventRuntime {
    private static final CopyOnWriteArrayList<Runnable> TICK_START = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> TICK_END = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LOGIN = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LOGOUT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenHandler> SCREEN_INIT_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenHandler> SCREEN_INIT_AFTER = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenRenderHandler> SCREEN_RENDER_AFTER = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.HudRenderHandler> HUD_AFTER_CHAT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.HudRenderHandler> HUD_END = new CopyOnWriteArrayList<>();

    private static boolean initialized;

    private KineticClientEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onLogin);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onLogout);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenInitBefore);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenInitAfter);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenRenderAfter);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onHudOverlayRender);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onHudRenderEnd);
    }

    public static HookRegistration registerTick(KineticClientEvents.TickPhase phase, Runnable listener) {
        initialize();
        return add(phase == KineticClientEvents.TickPhase.START ? TICK_START : TICK_END, listener);
    }

    public static HookRegistration registerLogin(Runnable listener) {
        initialize();
        return add(LOGIN, listener);
    }

    public static HookRegistration registerLogout(Runnable listener) {
        initialize();
        return add(LOGOUT, listener);
    }

    public static HookRegistration registerScreenInitBefore(KineticClientEvents.ScreenHandler listener) {
        initialize();
        return add(SCREEN_INIT_BEFORE, listener);
    }

    public static HookRegistration registerScreenInitAfter(KineticClientEvents.ScreenHandler listener) {
        initialize();
        return add(SCREEN_INIT_AFTER, listener);
    }

    public static HookRegistration registerScreenRenderAfter(KineticClientEvents.ScreenRenderHandler listener) {
        initialize();
        return add(SCREEN_RENDER_AFTER, listener);
    }

    public static HookRegistration registerHudRender(KineticClientEvents.HudStage stage, KineticClientEvents.HudRenderHandler listener) {
        initialize();
        return add(stage == KineticClientEvents.HudStage.AFTER_CHAT ? HUD_AFTER_CHAT : HUD_END, listener);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        fire(event.phase == TickEvent.Phase.START ? TICK_START : TICK_END);
    }

    private static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        fire(LOGIN);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        fire(LOGOUT);
    }

    private static void onScreenInitBefore(ScreenEvent.Init.Pre event) {
        for (KineticClientEvents.ScreenHandler listener : SCREEN_INIT_BEFORE) {
            listener.handle(event.getScreen());
        }
    }

    private static void onScreenInitAfter(ScreenEvent.Init.Post event) {
        for (KineticClientEvents.ScreenHandler listener : SCREEN_INIT_AFTER) {
            listener.handle(event.getScreen());
        }
    }

    private static void onScreenRenderAfter(ScreenEvent.Render.Post event) {
        for (KineticClientEvents.ScreenRenderHandler listener : SCREEN_RENDER_AFTER) {
            listener.render(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    private static void onHudOverlayRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
        for (KineticClientEvents.HudRenderHandler listener : HUD_AFTER_CHAT) {
            listener.render(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    private static void onHudRenderEnd(RenderGuiEvent.Post event) {
        for (KineticClientEvents.HudRenderHandler listener : HUD_END) {
            listener.render(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    private static void fire(CopyOnWriteArrayList<Runnable> listeners) {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private static <T> HookRegistration add(CopyOnWriteArrayList<T> listeners, T listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
