package dev.xyat.kineticcore.api.client.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.client.KineticClientEventRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;

public final class KineticClientEvents {
    public enum TickPhase {
        START,
        END
    }

    public enum HudStage {
        HOTBAR,
        AFTER_CHAT,
        END
    }

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

    public interface MouseButtonContext {
        int button();

        int action();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface MouseButtonHandler {
        void handle(MouseButtonContext context);
    }

    public interface InteractionKeyContext {
        boolean attack();

        boolean useItem();

        InteractionHand hand();

        boolean cancelled();

        void cancel();

        void swingHand(boolean swingHand);
    }

    @FunctionalInterface
    public interface InteractionKeyHandler {
        void handle(InteractionKeyContext context);
    }

    public interface LevelRenderContext {
        PoseStack poseStack();

        Camera camera();

        MultiBufferSource.BufferSource bufferSource();

        Matrix4f projectionMatrix();

        float partialTick();
    }

    @FunctionalInterface
    public interface LevelRenderHandler {
        void render(LevelRenderContext context);
    }

    public interface PlayerRenderContext {
        AbstractClientPlayer player();

        PoseStack poseStack();

        float partialTick();
    }

    public interface PlayerRenderBeforeContext extends PlayerRenderContext {
        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface PlayerRenderBeforeHandler {
        void render(PlayerRenderBeforeContext context);
    }

    @FunctionalInterface
    public interface PlayerRenderAfterHandler {
        void render(PlayerRenderContext context);
    }

    @FunctionalInterface
    public interface ScreenHandler {
        void handle(Screen screen);
    }

    public interface ScreenInitContext {
        Screen screen();

        List<GuiEventListener> listeners();

        void addListener(GuiEventListener listener);

        void removeListener(GuiEventListener listener);
    }

    @FunctionalInterface
    public interface ScreenInitHandler {
        void handle(ScreenInitContext context);
    }

    @FunctionalInterface
    public interface ScreenRenderHandler {
        void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    }

    public interface ScreenMouseButtonContext {
        Screen screen();

        double mouseX();

        double mouseY();

        int button();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface ScreenMouseButtonHandler {
        void handle(ScreenMouseButtonContext context);
    }

    @FunctionalInterface
    public interface HudRenderHandler {
        void render(GuiGraphics graphics, float partialTick);
    }

    private KineticClientEvents() {
    }

    public static HookRegistration onTick(TickPhase phase, Runnable listener) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerTick(phase, listener);
    }

    public static HookRegistration onLogin(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerLogin(listener);
    }

    public static HookRegistration onLogout(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerLogout(listener);
    }

    public static HookRegistration onAddReloadListener(PreparableReloadListener listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerReloadListener(listener);
    }

    public static HookRegistration onScreenInitBefore(ScreenHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenInitBefore(listener);
    }

    public static HookRegistration onScreenInitAfter(ScreenHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenInitAfter(listener);
    }

    public static HookRegistration onScreenInitAfterWithControls(ScreenInitHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenInitAfterWithControls(listener);
    }

    public static HookRegistration onScreenRenderBefore(ScreenRenderHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenRenderBefore(listener);
    }

    public static HookRegistration onScreenRenderAfter(ScreenRenderHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenRenderAfter(listener);
    }

    public static HookRegistration onScreenMouseButtonPressedBefore(ScreenMouseButtonHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenMouseButtonPressedBefore(listener);
    }

    public static HookRegistration onScreenMouseButtonReleasedBefore(ScreenMouseButtonHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenMouseButtonReleasedBefore(listener);
    }

    public static HookRegistration onMouseButtonBefore(MouseButtonHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerMouseButtonBefore(listener);
    }

    public static HookRegistration onInteractionKey(InteractionKeyHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerInteractionKey(listener);
    }

    public static HookRegistration onLevelRender(LevelRenderStage stage, LevelRenderHandler listener) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerLevelRender(stage, listener);
    }

    public static HookRegistration onPlayerRenderBefore(PlayerRenderBeforeHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerPlayerRenderBefore(listener);
    }

    public static HookRegistration onPlayerRenderAfter(PlayerRenderAfterHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerPlayerRenderAfter(listener);
    }

    public static HookRegistration onHudRender(HudStage stage, HudRenderHandler listener) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerHudRender(stage, listener);
    }
}
