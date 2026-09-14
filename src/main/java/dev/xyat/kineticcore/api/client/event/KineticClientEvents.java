package dev.xyat.kineticcore.api.client.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.client.KineticClientEventRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

public final class KineticClientEvents {
    public enum TickPhase {
        START,
        END
    }

    public enum HudStage {
        AFTER_CHAT,
        END
    }

    @FunctionalInterface
    public interface ScreenHandler {
        void handle(Screen screen);
    }

    @FunctionalInterface
    public interface ScreenRenderHandler {
        void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
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

    public static HookRegistration onScreenInitBefore(ScreenHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenInitBefore(listener);
    }

    public static HookRegistration onScreenInitAfter(ScreenHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenInitAfter(listener);
    }

    public static HookRegistration onScreenRenderAfter(ScreenRenderHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerScreenRenderAfter(listener);
    }

    public static HookRegistration onHudRender(HudStage stage, HudRenderHandler listener) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(listener, "listener");
        return KineticClientEventRuntime.registerHudRender(stage, listener);
    }
}
