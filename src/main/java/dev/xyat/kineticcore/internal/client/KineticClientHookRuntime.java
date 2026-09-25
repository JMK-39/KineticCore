package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;
import dev.xyat.kineticcore.internal.runtime.KineticCallbackQueries;
import dev.xyat.kineticcore.api.hook.ClientHooks;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticClientHookRuntime {
    private static final CopyOnWriteArrayList<ClientHooks.OptionsLoadHandler> OPTIONS_LOADING =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ClientHooks.ResourceReloadUi> RESOURCE_RELOAD_UI =
            new CopyOnWriteArrayList<>();

    private KineticClientHookRuntime() {
    }

    public static HookRegistration registerOptionsLoading(ClientHooks.OptionsLoadHandler handler) {
        OPTIONS_LOADING.add(handler);
        return HookRegistration.once(() -> OPTIONS_LOADING.remove(handler));
    }

    public static HookRegistration registerResourceReloadUi(ClientHooks.ResourceReloadUi handler) {
        RESOURCE_RELOAD_UI.add(handler);
        return HookRegistration.once(() -> RESOURCE_RELOAD_UI.remove(handler));
    }

    public static void fireOptionsLoading(Options options) {
        // This mixin can run before client feature hooks are registered by Forge.
        // Recover the file here so Options reads the configured defaults either way.
        DefaultOptionsRecoveryRuntime.restoreConfiguredDefaults();
        KineticCallbackBatch.runAll(OPTIONS_LOADING, handler -> handler.beforeLoad(options));
    }

    public static boolean interceptResourceReloadStart() {
        return KineticCallbackQueries.anyMatch(RESOURCE_RELOAD_UI, ClientHooks.ResourceReloadUi::interceptReloadStart);
    }

    public static void setPackScreenClosing(boolean closing) {
        KineticCallbackBatch.runAll(RESOURCE_RELOAD_UI, handler -> handler.setPackScreenClosing(closing));
    }

    public static void renderResourceReloadUi(GuiGraphics graphics, int width, int height) {
        KineticCallbackBatch.runAll(RESOURCE_RELOAD_UI, handler -> handler.render(graphics, width, height));
    }
}
