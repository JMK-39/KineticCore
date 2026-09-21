package dev.xyat.kineticcore.api.client.event;

import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import dev.xyat.kineticcore.internal.client.KineticClientEventRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;

/**
 * Public Kinetic event contract for client events.
 * 非取消型通知按订阅顺序执行全部回调；单个 RuntimeException 不阻断后续回调，
 * 首个异常在分发结束后抛出，后续不同异常通过 suppressed 保留。
 * 可取消事件亦隔离单个监听器的 RuntimeException，但一旦取消就立即停止分发；
 * 取消后抛异常时不继续调用下一监听器，错误仍在结束时报告。
 */
public final class KineticClientEvents {
    /** Defines the tick phase values supported by Kinetic Client Events. */
    public enum TickPhase {
        START,
        END
    }

    /** Defines the hud stage values supported by Kinetic Client Events. */
    public enum HudStage {
        HOTBAR,
        AFTER_CHAT,
        END
    }

    /** Defines the level render stage values supported by Kinetic Client Events. */
    public enum LevelRenderStage {
        AFTER_SKY,
        AFTER_SOLID_BLOCKS,
        AFTER_CUTOUT_MIPPED_BLOCKS,
        AFTER_CUTOUT_BLOCKS,
        AFTER_ENTITIES,
        AFTER_BLOCK_ENTITIES,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_TRIPWIRE_BLOCKS,
        AFTER_PARTICLES,
        AFTER_WEATHER,
        AFTER_LEVEL
    }

    /** Context supplied to mouse button handlers dispatched by Kinetic Client Events. */
    public interface MouseButtonContext {
        /** Returns the raw mouse-button code associated with this input event. */
        int button();

        /** Returns the raw press/release action code for advanced input handling. */
        int action();

        /** Returns whether this event represents a button press. */
        default boolean pressed() {
            return action() == 1;
        }

        /** Returns whether this event represents a button release. */
        default boolean released() {
            return action() == 0;
        }

        /** Returns whether this event targets the primary mouse button. */
        default boolean leftButton() {
            return button() == 0;
        }

        /** Returns whether this event targets the secondary mouse button. */
        default boolean rightButton() {
            return button() == 1;
        }

        /** Returns whether this event targets the middle mouse button. */
        default boolean middleButton() {
            return button() == 2;
        }

        /** Returns whether this event has been cancelled. */
        boolean cancelled();

        /** Cancels this event. */
        void cancel();
    }
    /** Handles mouse button callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface MouseButtonHandler {
        /** Handles one cancellable raw mouse-button input event. */
        void handle(MouseButtonContext context);
    }

    /** Context supplied to interaction key handlers dispatched by Kinetic Client Events. */
    public interface InteractionKeyContext {
        /** Returns whether this interaction was triggered by the attack key mapping. */
        boolean attack();

        /** Returns whether this interaction was triggered by the use-item key mapping. */
        boolean useItem();

        /** Returns the interaction hand associated with the key mapping event. */
        InteractionHand hand();

        /** Returns whether this event has been cancelled. */
        boolean cancelled();

        /** Cancels this event. */
        void cancel();

        /** Applies the configured hand-swing behavior for this event. */
        void swingHand(boolean swingHand);
    }
    /** Handles interaction key callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface InteractionKeyHandler {
        /** Handles one cancellable interaction-key mapping event. */
        void handle(InteractionKeyContext context);
    }

    /** Context supplied to level render handlers dispatched by Kinetic Client Events. */
    public interface LevelRenderContext {
        /** Returns the pose stack for the current render callback. */
        PoseStack poseStack();

        /** Returns the active level-render camera. */
        Camera camera();

        /** Returns the shared buffer source for the current level render stage. */
        MultiBufferSource.BufferSource bufferSource();

        /** Returns the projection matrix active for the current level render stage. */
        Matrix4f projectionMatrix();

        /** Returns the partial-tick value for interpolation during this render callback. */
        float partialTick();
    }
    /** Handles level render callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface LevelRenderHandler {
        /** Renders custom level content at the selected Kinetic render stage. */
        void render(LevelRenderContext context);
    }

    /** Context supplied while the client computes final camera yaw, pitch and roll. */
    public interface CameraAnglesContext {
        /** Returns the active render camera. */
        Camera camera();

        /** Returns the current render partial tick. */
        float partialTick();

        /** Returns the current camera yaw. */
        float yaw();

        /** Replaces the current camera yaw. */
        void setYaw(float yaw);

        /** Returns the current camera pitch. */
        float pitch();

        /** Replaces the current camera pitch. */
        void setPitch(float pitch);

        /** Returns the current camera roll. */
        float roll();

        /** Replaces the current camera roll. */
        void setRoll(float roll);
    }

    /** Handles final camera-angle callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface CameraAnglesHandler {
        /** Adjusts the final camera angles for the current rendered frame. */
        void handle(CameraAnglesContext context);
    }

    /** Context supplied to player render handlers dispatched by Kinetic Client Events. */
    public interface PlayerRenderContext {
        /** Returns the client player currently being rendered. */
        AbstractClientPlayer player();

        /** Returns the pose stack for the current render callback. */
        PoseStack poseStack();

        /** Returns the partial-tick value for interpolation during this render callback. */
        float partialTick();
    }

    /** Context supplied to player render before handlers dispatched by Kinetic Client Events. */
    public interface PlayerRenderBeforeContext extends PlayerRenderContext {
        /** Returns whether this event has been cancelled. */
        boolean cancelled();

        /** Cancels this event. */
        void cancel();
    }
    /** Handles player render before callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface PlayerRenderBeforeHandler {
        /** Renders or cancels custom behavior before the player model is rendered. */
        void render(PlayerRenderBeforeContext context);
    }
    /** Handles player render after callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface PlayerRenderAfterHandler {
        /** Renders custom behavior after the player model has rendered. */
        void render(PlayerRenderContext context);
    }
    /** Context supplied while an item tooltip is being assembled on the client. */
    public interface ItemTooltipContext {
        /** Returns the item stack whose tooltip is being assembled. */
        ItemStack itemStack();

        /** Returns the mutable tooltip lines for this item. */
        List<net.minecraft.network.chat.Component> tooltip();
    }

    /** Handles item-tooltip callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface ItemTooltipHandler {
        /** Adds, removes, or updates lines in one item tooltip. */
        void handle(ItemTooltipContext context);
    }

    /** Handles a Screen before its listener list is initialized. */
    @FunctionalInterface
    public interface ScreenInitBeforeHandler {
        /** Handles the screen about to begin initialization. */
        void handle(Screen screen);
    }

    /** Context supplied to screen init handlers dispatched by Kinetic Client Events. */
    public interface ScreenInitContext {
        /** Returns the screen associated with this client event. */
        Screen screen();

        /** Returns an immutable snapshot of listeners present after screen initialization. */
        List<GuiEventListener> listeners();

        /** Finds the first existing vanilla or third-party listener of the requested type. */
        default <T extends GuiEventListener> T findExistingListener(Class<T> type) {
            Objects.requireNonNull(type, "type");
            return listeners().stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .findFirst()
                    .orElse(null);
        }

        /** Removes the first existing vanilla or third-party listener of the requested type. */
        default <T extends GuiEventListener> boolean removeFirstExistingListener(Class<T> type) {
            T listener = findExistingListener(type);
            if (listener == null) return false;
            removeListener(listener);
            return true;
        }

        /** Adds one standard Kinetic control to this vanilla or third-party screen. */
        default <T extends KineticControl> T addControl(T control) {
            if (control != null) addListener(KineticControlBridge.widget(control));
            return control;
        }

        /** Removes one standard Kinetic control from this vanilla or third-party screen. */
        default void removeControl(KineticControl control) {
            if (control != null) removeListener(KineticControlBridge.widget(control));
        }

        /** Adds one raw listener only for vanilla or third-party listener types without a Kinetic control equivalent. */
        void addListener(GuiEventListener listener);

        /** Removes one raw listener, primarily when modifying a listener already owned by the target screen. */
        void removeListener(GuiEventListener listener);
    }
    /** Handles screen init callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface ScreenInitHandler {
        /** Handles a screen after initialization with controlled listener-list mutation access. */
        void handle(ScreenInitContext context);
    }
    /** Handles screen render callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface ScreenRenderHandler {
        /** Renders custom content before or after the supplied Screen using current mouse coordinates. */
        void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    }

    /** Context supplied to screen mouse button handlers dispatched by Kinetic Client Events. */
    public interface ScreenMouseButtonContext {
        /** Returns the screen associated with this client event. */
        Screen screen();

        /** Returns the mouse X coordinate associated with this event. */
        double mouseX();

        /** Returns the mouse Y coordinate associated with this event. */
        double mouseY();

        /** Returns the mouse-button code associated with this screen event. */
        int button();

        /** Returns whether this event has been cancelled. */
        boolean cancelled();

        /** Cancels this event. */
        void cancel();
    }
    /** Handles screen mouse button callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface ScreenMouseButtonHandler {
        /** Handles one cancellable screen mouse-button event before vanilla processing. */
        void handle(ScreenMouseButtonContext context);
    }
    /** Handles hud render callbacks dispatched by Kinetic Client Events. */
    @FunctionalInterface
    public interface HudRenderHandler {
        /** Renders custom HUD content at the selected Kinetic HUD stage. */
        void render(GuiGraphics graphics, float partialTick);
    }

    /** Mutable context fired while the client is about to render the in-block screen effect. */
    public interface BlockScreenEffectContext {
        /** Returns whether the block screen effect has been cancelled. */
        boolean cancelled();

        /** Cancels the block screen effect for this frame. */
        void cancel();
    }
    /** Handles the vanilla in-block screen effect before it is rendered. */
    @FunctionalInterface
    public interface BlockScreenEffectHandler {
        /** Handles the block screen effect. */
        void handle(BlockScreenEffectContext context);
    }

    /** Mutable layout decision for the vanilla inventory status-effect panel. */
    public interface InventoryEffectLayoutContext {
        /** Returns the horizontal space vanilla calculated for the inventory status-effect panel. */
        int availableSpace();

        /** Returns whether vanilla should use the compact status-effect layout. */
        boolean compact();

        /** Replaces vanilla's compact-layout decision for this render pass. */
        void setCompact(boolean compact);
    }

    /** Configures the vanilla inventory status-effect panel layout. */
    @FunctionalInterface
    public interface InventoryEffectLayoutHandler {
        /** Adjusts the inventory status-effect layout decision for the current render pass. */
        void configure(InventoryEffectLayoutContext context);
    }

    private KineticClientEvents() {
    }

    /** Registers a client tick listener for the requested start or end phase. */
    public static KineticEventSubscription onTick(TickPhase phase, Runnable listener) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerTick(phase, listener);
    }

    /** Registers a callback when the client finishes logging in to a server connection. */
    public static KineticEventSubscription onLogin(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerLogin(listener);
    }

    /** Registers a callback when the client disconnects from its current server connection. */
    public static KineticEventSubscription onLogout(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerLogout(listener);
    }

    /**
     * Registers a client resource reload listener during the Forge loading-phase registration window.
     * This is a one-time registration, not a runtime-cancellable subscription.
     * Each pending listener is submitted independently; a failed submission does not prevent
     * the remaining listeners from being submitted. Registration failures are rethrown after the batch.
     *
     * @throws IllegalStateException if the client reload-listener registration window has already closed
     */
    public static void registerReloadListener(PreparableReloadListener listener) {
        Objects.requireNonNull(listener, "listener");
        KineticClientEventRuntime.registerReloadListener(listener);
    }

    /** Registers a listener before a screen initializes its GUI listeners. */
    public static KineticEventSubscription onScreenInitBefore(ScreenInitBeforeHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenInitBefore(listener);
    }

    /** Registers a callback while the client assembles an item tooltip. */
    public static KineticEventSubscription onItemTooltip(ItemTooltipHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerItemTooltip(listener);
    }

    /**
     * Registers a post-initialization screen listener with access to the final listener list.
     * The context may add or remove GUI listeners before initialization completes.
     */
    public static KineticEventSubscription onScreenInitAfter(ScreenInitHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenInitAfter(listener);
    }

    /** Registers a listener before a Screen renders. */
    public static KineticEventSubscription onScreenRenderBefore(ScreenRenderHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenRenderBefore(listener);
    }

    /** Registers a listener after a Screen renders. */
    public static KineticEventSubscription onScreenRenderAfter(ScreenRenderHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenRenderAfter(listener);
    }

    /** Registers a cancellable listener before a Screen processes a mouse press. */
    public static KineticEventSubscription onScreenMouseButtonPressedBefore(ScreenMouseButtonHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenMouseButtonPressedBefore(listener);
    }

    /** Registers a cancellable listener before a Screen processes a mouse release. */
    public static KineticEventSubscription onScreenMouseButtonReleasedBefore(ScreenMouseButtonHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenMouseButtonReleasedBefore(listener);
    }

    /** Registers a cancellable raw mouse-button input listener. */
    public static KineticEventSubscription onMouseButtonBefore(MouseButtonHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerMouseButtonBefore(listener);
    }

    /** Registers a cancellable attack/use interaction-key listener. */
    public static KineticEventSubscription onInteractionKey(InteractionKeyHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerInteractionKey(listener);
    }

    /** Registers a level-render callback at one explicit Kinetic render stage. */
    public static KineticEventSubscription onLevelRender(LevelRenderStage stage, LevelRenderHandler listener) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerLevelRender(stage, listener);
    }

    /** Registers a final camera-angle listener that may adjust yaw, pitch and roll. */
    public static KineticEventSubscription onCameraAngles(CameraAnglesHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerCameraAngles(listener);
    }

    /** Registers a cancellable callback before a client player renders. */
    public static KineticEventSubscription onPlayerRenderBefore(PlayerRenderBeforeHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerPlayerRenderBefore(listener);
    }

    /** Registers a callback after a client player renders. */
    public static KineticEventSubscription onPlayerRenderAfter(PlayerRenderAfterHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerPlayerRenderAfter(listener);
    }

    /** Registers a HUD render callback at one explicit Kinetic HUD stage. */
    public static KineticEventSubscription onHudRender(HudStage stage, HudRenderHandler listener) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerHudRender(stage, listener);
    }

    /** Registers an in-block screen-effect listener. */
    public static KineticEventSubscription onBlockScreenEffect(BlockScreenEffectHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerBlockScreenEffect(listener);
    }

    /**
     * Registers a vanilla inventory status-effect layout listener. The configured
     * {@link dev.xyat.kineticcore.api.client.effect.KineticEffectDisplay} compact rule
     * is applied first, including when no listeners are registered. Listeners can
     * override that initial value through the supplied context.
     * 即使某个布局回调失败，也会先写回其他回调确定的最终布局，再报告异常。
     */
    public static KineticEventSubscription onInventoryEffectLayout(InventoryEffectLayoutHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerInventoryEffectLayout(listener);
    }
}
