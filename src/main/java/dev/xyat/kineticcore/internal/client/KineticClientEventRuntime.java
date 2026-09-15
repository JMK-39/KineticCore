package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticClientEventRuntime {
    private static final CopyOnWriteArrayList<Runnable> TICK_START = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> TICK_END = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LOGIN = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LOGOUT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<PreparableReloadListener> CLIENT_RELOAD_LISTENERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenHandler> SCREEN_INIT_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenHandler> SCREEN_INIT_AFTER = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenInitHandler> SCREEN_INIT_AFTER_WITH_CONTROLS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenRenderHandler> SCREEN_RENDER_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenRenderHandler> SCREEN_RENDER_AFTER = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenMouseButtonHandler> SCREEN_MOUSE_PRESSED_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenMouseButtonHandler> SCREEN_MOUSE_RELEASED_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.HudRenderHandler> HUD_HOTBAR = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.HudRenderHandler> HUD_AFTER_CHAT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.HudRenderHandler> HUD_END = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.MouseButtonHandler> MOUSE_BUTTON_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.InteractionKeyHandler> INTERACTION_KEY = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.PlayerRenderBeforeHandler> PLAYER_RENDER_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.PlayerRenderAfterHandler> PLAYER_RENDER_AFTER = new CopyOnWriteArrayList<>();
    private static final EnumMap<KineticClientEvents.LevelRenderStage, CopyOnWriteArrayList<KineticClientEvents.LevelRenderHandler>> LEVEL_RENDER = levelRenderHandlers();

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
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenRenderBefore);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenRenderAfter);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenMousePressedBefore);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenMouseReleasedBefore);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onHudOverlayRender);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onHudRenderEnd);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onMouseButtonBefore);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onInteractionKey);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onPlayerRenderBefore);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onPlayerRenderAfter);
        MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onLevelRender);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticClientEventRuntime::onRegisterClientReloadListeners);
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

    public static HookRegistration registerReloadListener(PreparableReloadListener listener) {
        initialize();
        return add(CLIENT_RELOAD_LISTENERS, listener);
    }

    public static HookRegistration registerScreenInitBefore(KineticClientEvents.ScreenHandler listener) {
        initialize();
        return add(SCREEN_INIT_BEFORE, listener);
    }

    public static HookRegistration registerScreenInitAfter(KineticClientEvents.ScreenHandler listener) {
        initialize();
        return add(SCREEN_INIT_AFTER, listener);
    }

    public static HookRegistration registerScreenInitAfterWithControls(KineticClientEvents.ScreenInitHandler listener) {
        initialize();
        return add(SCREEN_INIT_AFTER_WITH_CONTROLS, listener);
    }

    public static HookRegistration registerScreenRenderBefore(KineticClientEvents.ScreenRenderHandler listener) {
        initialize();
        return add(SCREEN_RENDER_BEFORE, listener);
    }

    public static HookRegistration registerScreenRenderAfter(KineticClientEvents.ScreenRenderHandler listener) {
        initialize();
        return add(SCREEN_RENDER_AFTER, listener);
    }

    public static HookRegistration registerScreenMouseButtonPressedBefore(KineticClientEvents.ScreenMouseButtonHandler listener) {
        initialize();
        return add(SCREEN_MOUSE_PRESSED_BEFORE, listener);
    }

    public static HookRegistration registerScreenMouseButtonReleasedBefore(KineticClientEvents.ScreenMouseButtonHandler listener) {
        initialize();
        return add(SCREEN_MOUSE_RELEASED_BEFORE, listener);
    }

    public static HookRegistration registerMouseButtonBefore(KineticClientEvents.MouseButtonHandler listener) {
        initialize();
        return add(MOUSE_BUTTON_BEFORE, listener);
    }

    public static HookRegistration registerInteractionKey(KineticClientEvents.InteractionKeyHandler listener) {
        initialize();
        return add(INTERACTION_KEY, listener);
    }

    public static HookRegistration registerPlayerRenderBefore(KineticClientEvents.PlayerRenderBeforeHandler listener) {
        initialize();
        return add(PLAYER_RENDER_BEFORE, listener);
    }

    public static HookRegistration registerPlayerRenderAfter(KineticClientEvents.PlayerRenderAfterHandler listener) {
        initialize();
        return add(PLAYER_RENDER_AFTER, listener);
    }

    public static HookRegistration registerLevelRender(KineticClientEvents.LevelRenderStage stage, KineticClientEvents.LevelRenderHandler listener) {
        initialize();
        return add(LEVEL_RENDER.get(stage), listener);
    }

    public static HookRegistration registerHudRender(KineticClientEvents.HudStage stage, KineticClientEvents.HudRenderHandler listener) {
        initialize();
        return add(switch (stage) {
            case HOTBAR -> HUD_HOTBAR;
            case AFTER_CHAT -> HUD_AFTER_CHAT;
            case END -> HUD_END;
        }, listener);
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

    private static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        for (PreparableReloadListener listener : CLIENT_RELOAD_LISTENERS) {
            event.registerReloadListener(listener);
        }
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
        if (!SCREEN_INIT_AFTER_WITH_CONTROLS.isEmpty()) {
            ScreenInitContextImpl context = new ScreenInitContextImpl(
                    event.getScreen(),
                    event.getListenersList(),
                    event::addListener,
                    event::removeListener
            );
            for (KineticClientEvents.ScreenInitHandler listener : SCREEN_INIT_AFTER_WITH_CONTROLS) {
                listener.handle(context);
            }
        }
    }

    private static void onScreenRenderBefore(ScreenEvent.Render.Pre event) {
        for (KineticClientEvents.ScreenRenderHandler listener : SCREEN_RENDER_BEFORE) {
            listener.render(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    private static void onScreenRenderAfter(ScreenEvent.Render.Post event) {
        for (KineticClientEvents.ScreenRenderHandler listener : SCREEN_RENDER_AFTER) {
            listener.render(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    private static void onScreenMousePressedBefore(ScreenEvent.MouseButtonPressed.Pre event) {
        fireScreenMouse(SCREEN_MOUSE_PRESSED_BEFORE, event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton(), event.isCanceled(), event::setCanceled);
    }

    private static void onScreenMouseReleasedBefore(ScreenEvent.MouseButtonReleased.Pre event) {
        fireScreenMouse(SCREEN_MOUSE_RELEASED_BEFORE, event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton(), event.isCanceled(), event::setCanceled);
    }

    private static void onHudOverlayRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            for (KineticClientEvents.HudRenderHandler listener : HUD_HOTBAR) {
                listener.render(event.getGuiGraphics(), event.getPartialTick());
            }
        }
        if (event.getOverlay() == VanillaGuiOverlay.CHAT_PANEL.type()) {
            for (KineticClientEvents.HudRenderHandler listener : HUD_AFTER_CHAT) {
                listener.render(event.getGuiGraphics(), event.getPartialTick());
            }
        }
    }

    private static void onMouseButtonBefore(InputEvent.MouseButton.Pre event) {
        if (MOUSE_BUTTON_BEFORE.isEmpty()) return;
        MouseButtonContextImpl context = new MouseButtonContextImpl(event);
        for (KineticClientEvents.MouseButtonHandler listener : MOUSE_BUTTON_BEFORE) {
            listener.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (INTERACTION_KEY.isEmpty()) return;
        InteractionKeyContextImpl context = new InteractionKeyContextImpl(event);
        for (KineticClientEvents.InteractionKeyHandler listener : INTERACTION_KEY) {
            listener.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onPlayerRenderBefore(RenderPlayerEvent.Pre event) {
        if (PLAYER_RENDER_BEFORE.isEmpty()) return;
        PlayerRenderBeforeContextImpl context = new PlayerRenderBeforeContextImpl(event);
        for (KineticClientEvents.PlayerRenderBeforeHandler listener : PLAYER_RENDER_BEFORE) {
            listener.render(context);
            if (context.cancelled()) break;
        }
    }

    private static void onPlayerRenderAfter(RenderPlayerEvent.Post event) {
        if (PLAYER_RENDER_AFTER.isEmpty()) return;
        PlayerRenderContextImpl context = new PlayerRenderContextImpl(event);
        for (KineticClientEvents.PlayerRenderAfterHandler listener : PLAYER_RENDER_AFTER) {
            listener.render(context);
        }
    }

    private static void onLevelRender(RenderLevelStageEvent event) {
        KineticClientEvents.LevelRenderStage stage = mapLevelRenderStage(event.getStage());
        if (stage == null) return;
        CopyOnWriteArrayList<KineticClientEvents.LevelRenderHandler> listeners = LEVEL_RENDER.get(stage);
        if (listeners == null || listeners.isEmpty()) return;
        LevelRenderContextImpl context = new LevelRenderContextImpl(event);
        for (KineticClientEvents.LevelRenderHandler listener : listeners) {
            listener.render(context);
        }
    }

    private static KineticClientEvents.LevelRenderStage mapLevelRenderStage(RenderLevelStageEvent.Stage stage) {
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) return KineticClientEvents.LevelRenderStage.AFTER_SKY;
        if (stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return KineticClientEvents.LevelRenderStage.AFTER_SOLID_BLOCKS;
        if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) return KineticClientEvents.LevelRenderStage.AFTER_CUTOUT_MIPPED_BLOCKS;
        if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return KineticClientEvents.LevelRenderStage.AFTER_CUTOUT_BLOCKS;
        if (stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES) return KineticClientEvents.LevelRenderStage.AFTER_ENTITIES;
        if (stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return KineticClientEvents.LevelRenderStage.AFTER_BLOCK_ENTITIES;
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return KineticClientEvents.LevelRenderStage.AFTER_TRANSLUCENT_BLOCKS;
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return KineticClientEvents.LevelRenderStage.AFTER_TRIPWIRE_BLOCKS;
        if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) return KineticClientEvents.LevelRenderStage.AFTER_PARTICLES;
        if (stage == RenderLevelStageEvent.Stage.AFTER_WEATHER) return KineticClientEvents.LevelRenderStage.AFTER_WEATHER;
        if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) return KineticClientEvents.LevelRenderStage.AFTER_LEVEL;
        return null;
    }

    private static void onHudRenderEnd(RenderGuiEvent.Post event) {
        for (KineticClientEvents.HudRenderHandler listener : HUD_END) {
            listener.render(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    private record ScreenInitContextImpl(
            Screen screen,
            List<GuiEventListener> listeners,
            Consumer<GuiEventListener> listenerAdder,
            Consumer<GuiEventListener> listenerRemover
    ) implements KineticClientEvents.ScreenInitContext {
        ScreenInitContextImpl {
            listeners = listeners == null ? List.of() : List.copyOf(listeners);
        }

        @Override
        public void addListener(GuiEventListener listener) {
            if (listener != null) listenerAdder.accept(listener);
        }

        @Override
        public void removeListener(GuiEventListener listener) {
            if (listener != null) listenerRemover.accept(listener);
        }
    }


    private static void fireScreenMouse(
            CopyOnWriteArrayList<KineticClientEvents.ScreenMouseButtonHandler> listeners,
            Screen screen,
            double mouseX,
            double mouseY,
            int button,
            boolean initiallyCancelled,
            java.util.function.Consumer<Boolean> cancelAction
    ) {
        if (listeners.isEmpty()) return;
        ScreenMouseButtonContextImpl context = new ScreenMouseButtonContextImpl(
                screen, mouseX, mouseY, button, initiallyCancelled, cancelAction
        );
        for (KineticClientEvents.ScreenMouseButtonHandler listener : listeners) {
            listener.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static final class ScreenMouseButtonContextImpl implements KineticClientEvents.ScreenMouseButtonContext {
        private final Screen screen;
        private final double mouseX;
        private final double mouseY;
        private final int button;
        private final java.util.function.Consumer<Boolean> cancelAction;
        private boolean cancelled;

        private ScreenMouseButtonContextImpl(
                Screen screen,
                double mouseX,
                double mouseY,
                int button,
                boolean cancelled,
                java.util.function.Consumer<Boolean> cancelAction
        ) {
            this.screen = screen;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.button = button;
            this.cancelled = cancelled;
            this.cancelAction = cancelAction;
        }

        @Override
        public Screen screen() {
            return screen;
        }

        @Override
        public double mouseX() {
            return mouseX;
        }

        @Override
        public double mouseY() {
            return mouseY;
        }

        @Override
        public int button() {
            return button;
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            if (cancelled) return;
            cancelled = true;
            cancelAction.accept(true);
        }
    }

    private record MouseButtonContextImpl(InputEvent.MouseButton.Pre event) implements KineticClientEvents.MouseButtonContext {
        @Override
        public int button() {
            return event.getButton();
        }

        @Override
        public int action() {
            return event.getAction();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record PlayerRenderBeforeContextImpl(RenderPlayerEvent.Pre event) implements KineticClientEvents.PlayerRenderBeforeContext {
        @Override
        public net.minecraft.client.player.AbstractClientPlayer player() {
            return (net.minecraft.client.player.AbstractClientPlayer) event.getEntity();
        }

        @Override
        public com.mojang.blaze3d.vertex.PoseStack poseStack() {
            return event.getPoseStack();
        }

        @Override
        public float partialTick() {
            return event.getPartialTick();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record PlayerRenderContextImpl(RenderPlayerEvent.Post event) implements KineticClientEvents.PlayerRenderContext {
        @Override
        public net.minecraft.client.player.AbstractClientPlayer player() {
            return (net.minecraft.client.player.AbstractClientPlayer) event.getEntity();
        }

        @Override
        public com.mojang.blaze3d.vertex.PoseStack poseStack() {
            return event.getPoseStack();
        }

        @Override
        public float partialTick() {
            return event.getPartialTick();
        }
    }

    private record InteractionKeyContextImpl(InputEvent.InteractionKeyMappingTriggered event) implements KineticClientEvents.InteractionKeyContext {
        @Override
        public boolean attack() {
            return event.isAttack();
        }

        @Override
        public boolean useItem() {
            return event.isUseItem();
        }

        @Override
        public net.minecraft.world.InteractionHand hand() {
            return event.getHand();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }

        @Override
        public void swingHand(boolean swingHand) {
            event.setSwingHand(swingHand);
        }
    }

    private record LevelRenderContextImpl(RenderLevelStageEvent event) implements KineticClientEvents.LevelRenderContext {
        @Override
        public com.mojang.blaze3d.vertex.PoseStack poseStack() {
            return event.getPoseStack();
        }

        @Override
        public net.minecraft.client.Camera camera() {
            return event.getCamera();
        }

        @Override
        public net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource() {
            return net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        }

        @Override
        public org.joml.Matrix4f projectionMatrix() {
            return event.getProjectionMatrix();
        }

        @Override
        public float partialTick() {
            return event.getPartialTick();
        }
    }

    private static EnumMap<KineticClientEvents.LevelRenderStage, CopyOnWriteArrayList<KineticClientEvents.LevelRenderHandler>> levelRenderHandlers() {
        EnumMap<KineticClientEvents.LevelRenderStage, CopyOnWriteArrayList<KineticClientEvents.LevelRenderHandler>> result = new EnumMap<>(KineticClientEvents.LevelRenderStage.class);
        for (KineticClientEvents.LevelRenderStage stage : KineticClientEvents.LevelRenderStage.values()) {
            result.put(stage, new CopyOnWriteArrayList<>());
        }
        return result;
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
