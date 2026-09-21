package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.effect.KineticEffectDisplay;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticClientEventRuntime {
    private static final CopyOnWriteArrayList<Runnable> TICK_START = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> TICK_END = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LOGIN = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LOGOUT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<PreparableReloadListener> CLIENT_RELOAD_LISTENERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenInitBeforeHandler> SCREEN_INIT_BEFORE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ItemTooltipHandler> ITEM_TOOLTIP = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.ScreenInitHandler> SCREEN_INIT_AFTER = new CopyOnWriteArrayList<>();
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
    private static final CopyOnWriteArrayList<KineticClientEvents.BlockScreenEffectHandler> BLOCK_SCREEN_EFFECT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.InventoryEffectLayoutHandler> INVENTORY_EFFECT_LAYOUT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticClientEvents.CameraAnglesHandler> CAMERA_ANGLES = new CopyOnWriteArrayList<>();
    private static final EnumMap<KineticClientEvents.LevelRenderStage, CopyOnWriteArrayList<KineticClientEvents.LevelRenderHandler>> LEVEL_RENDER = levelRenderHandlers();

    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;
    private static boolean reloadListenerRegistrationClosed;

    private KineticClientEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;

        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onClientTick));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onLogin));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onLogout));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onItemTooltip));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenInitBefore));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenInitAfter));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenRenderBefore));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenRenderAfter));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenMousePressedBefore));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onScreenMouseReleasedBefore));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onHudOverlayRender));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onHudRenderEnd));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onMouseButtonBefore));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onInteractionKey));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onPlayerRenderBefore));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onPlayerRenderAfter));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onLevelRender));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, KineticClientEventRuntime::onCameraAngles));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticClientEventRuntime::onBlockScreenEffect));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, KineticClientEventRuntime::onInventoryEffectLayout));
        attempt.install(slot++, () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticClientEventRuntime::onRegisterClientReloadListeners));
        attempt.finish();
        initialized = true;
    }

    public static KineticEventSubscription registerTick(KineticClientEvents.TickPhase phase, Runnable listener) {
        initialize();
        return add(phase == KineticClientEvents.TickPhase.START ? TICK_START : TICK_END, listener);
    }

    public static KineticEventSubscription registerLogin(Runnable listener) {
        initialize();
        return add(LOGIN, listener);
    }

    public static KineticEventSubscription registerLogout(Runnable listener) {
        initialize();
        return add(LOGOUT, listener);
    }

    public static KineticEventSubscription registerItemTooltip(KineticClientEvents.ItemTooltipHandler listener) {
        initialize();
        return add(ITEM_TOOLTIP, listener);
    }

    public static synchronized void registerReloadListener(PreparableReloadListener listener) {
        initialize();
        if (reloadListenerRegistrationClosed) {
            throw new IllegalStateException("Client reload-listener registration window has already closed");
        }
        CLIENT_RELOAD_LISTENERS.add(listener);
    }

    public static KineticEventSubscription registerScreenInitBefore(KineticClientEvents.ScreenInitBeforeHandler listener) {
        initialize();
        return add(SCREEN_INIT_BEFORE, listener);
    }

    public static KineticEventSubscription registerScreenInitAfter(KineticClientEvents.ScreenInitHandler listener) {
        initialize();
        return add(SCREEN_INIT_AFTER, listener);
    }

    public static KineticEventSubscription registerScreenRenderBefore(KineticClientEvents.ScreenRenderHandler listener) {
        initialize();
        return add(SCREEN_RENDER_BEFORE, listener);
    }

    public static KineticEventSubscription registerScreenRenderAfter(KineticClientEvents.ScreenRenderHandler listener) {
        initialize();
        return add(SCREEN_RENDER_AFTER, listener);
    }

    public static KineticEventSubscription registerScreenMouseButtonPressedBefore(KineticClientEvents.ScreenMouseButtonHandler listener) {
        initialize();
        return add(SCREEN_MOUSE_PRESSED_BEFORE, listener);
    }

    public static KineticEventSubscription registerScreenMouseButtonReleasedBefore(KineticClientEvents.ScreenMouseButtonHandler listener) {
        initialize();
        return add(SCREEN_MOUSE_RELEASED_BEFORE, listener);
    }

    public static KineticEventSubscription registerMouseButtonBefore(KineticClientEvents.MouseButtonHandler listener) {
        initialize();
        return add(MOUSE_BUTTON_BEFORE, listener);
    }

    public static KineticEventSubscription registerInteractionKey(KineticClientEvents.InteractionKeyHandler listener) {
        initialize();
        return add(INTERACTION_KEY, listener);
    }

    public static KineticEventSubscription registerPlayerRenderBefore(KineticClientEvents.PlayerRenderBeforeHandler listener) {
        initialize();
        return add(PLAYER_RENDER_BEFORE, listener);
    }

    public static KineticEventSubscription registerPlayerRenderAfter(KineticClientEvents.PlayerRenderAfterHandler listener) {
        initialize();
        return add(PLAYER_RENDER_AFTER, listener);
    }

    public static KineticEventSubscription registerLevelRender(KineticClientEvents.LevelRenderStage stage, KineticClientEvents.LevelRenderHandler listener) {
        initialize();
        return add(LEVEL_RENDER.get(stage), listener);
    }

    public static KineticEventSubscription registerCameraAngles(KineticClientEvents.CameraAnglesHandler listener) {
        initialize();
        return add(CAMERA_ANGLES, listener);
    }

    public static KineticEventSubscription registerHudRender(KineticClientEvents.HudStage stage, KineticClientEvents.HudRenderHandler listener) {
        initialize();
        return add(switch (stage) {
            case HOTBAR -> HUD_HOTBAR;
            case AFTER_CHAT -> HUD_AFTER_CHAT;
            case END -> HUD_END;
        }, listener);
    }

    public static KineticEventSubscription registerBlockScreenEffect(KineticClientEvents.BlockScreenEffectHandler listener) {
        initialize();
        return add(BLOCK_SCREEN_EFFECT, listener);
    }

    public static KineticEventSubscription registerInventoryEffectLayout(KineticClientEvents.InventoryEffectLayoutHandler listener) {
        initialize();
        return add(INVENTORY_EFFECT_LAYOUT, listener);
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

    private static void onItemTooltip(ItemTooltipEvent event) {
        if (ITEM_TOOLTIP.isEmpty()) return;
        ItemTooltipContextImpl context = new ItemTooltipContextImpl(event);
        KineticCallbackBatch.runAll(ITEM_TOOLTIP, listener -> listener.handle(context));
    }

    private static synchronized void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        reloadListenerRegistrationClosed = true;
        RuntimeException failure = null;
        try {
            for (PreparableReloadListener listener : CLIENT_RELOAD_LISTENERS) {
                try {
                    event.registerReloadListener(listener);
                } catch (RuntimeException exception) {
                    if (failure == null) failure = exception;
                    else if (failure != exception) failure.addSuppressed(exception);
                }
            }
        } finally {
            CLIENT_RELOAD_LISTENERS.clear();
        }
        if (failure != null) throw failure;
    }

    private static void onScreenInitBefore(ScreenEvent.Init.Pre event) {
        KineticCallbackBatch.runAll(SCREEN_INIT_BEFORE, listener -> listener.handle(event.getScreen()));
    }

    private static void onScreenInitAfter(ScreenEvent.Init.Post event) {
        if (SCREEN_INIT_AFTER.isEmpty()) return;
        ScreenInitContextImpl context = new ScreenInitContextImpl(
                event.getScreen(),
                event.getListenersList(),
                event::addListener,
                event::removeListener
        );
        KineticCallbackBatch.runAll(SCREEN_INIT_AFTER, listener -> listener.handle(context));
    }

    private static void onScreenRenderBefore(ScreenEvent.Render.Pre event) {
        KineticCallbackBatch.runAll(SCREEN_RENDER_BEFORE, listener -> listener.render(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick()));
    }

    private static void onScreenRenderAfter(ScreenEvent.Render.Post event) {
        KineticCallbackBatch.runAll(SCREEN_RENDER_AFTER, listener -> listener.render(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick()));
    }

    private static void onScreenMousePressedBefore(ScreenEvent.MouseButtonPressed.Pre event) {
        fireScreenMouse(SCREEN_MOUSE_PRESSED_BEFORE, event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton(), event.isCanceled(), event::setCanceled);
    }

    private static void onScreenMouseReleasedBefore(ScreenEvent.MouseButtonReleased.Pre event) {
        fireScreenMouse(SCREEN_MOUSE_RELEASED_BEFORE, event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton(), event.isCanceled(), event::setCanceled);
    }

    private static void onHudOverlayRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            KineticCallbackBatch.runAll(HUD_HOTBAR, listener -> listener.render(event.getGuiGraphics(), event.getPartialTick()));
        }
        if (event.getOverlay() == VanillaGuiOverlay.CHAT_PANEL.type()) {
            KineticCallbackBatch.runAll(HUD_AFTER_CHAT, listener -> listener.render(event.getGuiGraphics(), event.getPartialTick()));
        }
    }

    private static void onMouseButtonBefore(InputEvent.MouseButton.Pre event) {
        if (MOUSE_BUTTON_BEFORE.isEmpty()) return;
        MouseButtonContextImpl context = new MouseButtonContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                MOUSE_BUTTON_BEFORE,
                listener -> listener.handle(context),
                context::cancelled
        );
    }

    private static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (INTERACTION_KEY.isEmpty()) return;
        InteractionKeyContextImpl context = new InteractionKeyContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                INTERACTION_KEY,
                listener -> listener.handle(context),
                context::cancelled
        );
    }

    private static void onPlayerRenderBefore(RenderPlayerEvent.Pre event) {
        if (PLAYER_RENDER_BEFORE.isEmpty()) return;
        PlayerRenderBeforeContextImpl context = new PlayerRenderBeforeContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                PLAYER_RENDER_BEFORE,
                listener -> listener.render(context),
                context::cancelled
        );
    }

    private static void onPlayerRenderAfter(RenderPlayerEvent.Post event) {
        if (PLAYER_RENDER_AFTER.isEmpty()) return;
        PlayerRenderContextImpl context = new PlayerRenderContextImpl(event);
        KineticCallbackBatch.runAll(PLAYER_RENDER_AFTER, listener -> listener.render(context));
    }

    private static void onLevelRender(RenderLevelStageEvent event) {
        KineticClientEvents.LevelRenderStage stage = mapLevelRenderStage(event.getStage());
        if (stage == null) return;
        CopyOnWriteArrayList<KineticClientEvents.LevelRenderHandler> listeners = LEVEL_RENDER.get(stage);
        if (listeners == null || listeners.isEmpty()) return;
        LevelRenderContextImpl context = new LevelRenderContextImpl(event);
        KineticCallbackBatch.runAll(listeners, listener -> listener.render(context));
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (CAMERA_ANGLES.isEmpty()) return;

        CameraAnglesContextImpl context = new CameraAnglesContextImpl(event);
        KineticCallbackBatch.runAll(CAMERA_ANGLES, listener -> listener.handle(context));
        event.setRoll(context.roll());
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
        KineticCallbackBatch.runAll(HUD_END, listener -> listener.render(event.getGuiGraphics(), event.getPartialTick()));
    }

    private static void onBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        if (BLOCK_SCREEN_EFFECT.isEmpty()) return;
        BlockScreenEffectContextImpl context = new BlockScreenEffectContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                BLOCK_SCREEN_EFFECT,
                listener -> listener.handle(context),
                context::cancelled
        );
    }

    private static void onInventoryEffectLayout(ScreenEvent.RenderInventoryMobEffects event) {
        InventoryEffectLayoutContextImpl context = new InventoryEffectLayoutContextImpl(
                event.getAvailableSpace(), KineticEffectDisplay.compact(event.getAvailableSpace())
        );
        RuntimeException failure = null;
        try {
            KineticCallbackBatch.runAll(INVENTORY_EFFECT_LAYOUT, listener -> listener.configure(context));
        } catch (RuntimeException exception) {
            failure = exception;
        }
        // Always apply the final layout decision, even if an earlier addon failed.
        try {
            event.setCompact(context.compact());
        } catch (RuntimeException exception) {
            if (failure == null) failure = exception;
            else if (failure != exception) failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }

    private static final class BlockScreenEffectContextImpl implements KineticClientEvents.BlockScreenEffectContext {
        private final RenderBlockScreenEffectEvent event;

        private BlockScreenEffectContextImpl(RenderBlockScreenEffectEvent event) {
            this.event = event;
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

    private static final class InventoryEffectLayoutContextImpl implements KineticClientEvents.InventoryEffectLayoutContext {
        private final int availableSpace;
        private boolean compact;

        private InventoryEffectLayoutContextImpl(int availableSpace, boolean compact) {
            this.availableSpace = availableSpace;
            this.compact = compact;
        }

        @Override
        public int availableSpace() {
            return availableSpace;
        }

        @Override
        public boolean compact() {
            return compact;
        }

        @Override
        public void setCompact(boolean compact) {
            this.compact = compact;
        }
    }

    private record ItemTooltipContextImpl(ItemTooltipEvent event) implements KineticClientEvents.ItemTooltipContext {
        @Override
        public net.minecraft.world.item.ItemStack itemStack() {
            return event.getItemStack();
        }

        @Override
        public java.util.List<net.minecraft.network.chat.Component> tooltip() {
            return event.getToolTip();
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
        KineticCallbackBatch.runUntilCancelled(
                listeners,
                listener -> listener.handle(context),
                context::cancelled
        );
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

    private static final class CameraAnglesContextImpl implements KineticClientEvents.CameraAnglesContext {
        private final ViewportEvent.ComputeCameraAngles event;
        private float roll;

        private CameraAnglesContextImpl(ViewportEvent.ComputeCameraAngles event) {
            this.event = event;
            this.roll = event.getRoll();
        }

        @Override
        public net.minecraft.client.Camera camera() {
            return event.getCamera();
        }

        @Override
        public float partialTick() {
            return (float) event.getPartialTick();
        }

        @Override
        public float yaw() {
            return event.getYaw();
        }

        @Override
        public void setYaw(float yaw) {
            event.setYaw(yaw);
        }

        @Override
        public float pitch() {
            return event.getPitch();
        }

        @Override
        public void setPitch(float pitch) {
            event.setPitch(pitch);
        }

        @Override
        public float roll() {
            return roll;
        }

        @Override
        public void setRoll(float roll) {
            this.roll = roll;
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
        KineticCallbackBatch.runAll(listeners, Runnable::run);
    }

    private static <T> KineticEventSubscription add(CopyOnWriteArrayList<T> listeners, T listener) {
        listeners.add(listener);
        return KineticEventSubscription.once(() -> listeners.remove(listener));
    }
}
