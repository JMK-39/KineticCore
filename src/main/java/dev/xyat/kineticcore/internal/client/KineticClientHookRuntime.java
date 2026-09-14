package dev.xyat.kineticcore.internal.client;

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
        return () -> OPTIONS_LOADING.remove(handler);
    }

    public static HookRegistration registerResourceReloadUi(ClientHooks.ResourceReloadUi handler) {
        RESOURCE_RELOAD_UI.add(handler);
        return () -> RESOURCE_RELOAD_UI.remove(handler);
    }

    public static void fireOptionsLoading(Options options) {
        for (ClientHooks.OptionsLoadHandler handler : OPTIONS_LOADING) {
            handler.beforeLoad(options);
        }
    }

    public static boolean interceptResourceReloadStart() {
        for (ClientHooks.ResourceReloadUi handler : RESOURCE_RELOAD_UI) {
            if (handler.interceptReloadStart()) return true;
        }
        return false;
    }

    public static void setPackScreenClosing(boolean closing) {
        for (ClientHooks.ResourceReloadUi handler : RESOURCE_RELOAD_UI) {
            handler.setPackScreenClosing(closing);
        }
    }

    public static void renderResourceReloadUi(GuiGraphics graphics, int width, int height) {
        for (ClientHooks.ResourceReloadUi handler : RESOURCE_RELOAD_UI) {
            handler.render(graphics, width, height);
        }
    }
}
