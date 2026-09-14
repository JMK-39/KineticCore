package dev.xyat.kineticcore.api.hook;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

public final class ClientHooks {
    private ClientHooks() {
    }

    public static HookRegistration onOptionsLoading(OptionsLoadHandler handler) {
        return KineticClientHookRuntime.registerOptionsLoading(
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onResourceReloadUi(ResourceReloadUi handler) {
        return KineticClientHookRuntime.registerResourceReloadUi(
                Objects.requireNonNull(handler, "handler")
        );
    }

    @FunctionalInterface
    public interface OptionsLoadHandler {
        void beforeLoad(Options options);
    }

    public interface ResourceReloadUi {
        void setPackScreenClosing(boolean closing);

        boolean interceptReloadStart();

        void render(GuiGraphics graphics, int width, int height);
    }
}
