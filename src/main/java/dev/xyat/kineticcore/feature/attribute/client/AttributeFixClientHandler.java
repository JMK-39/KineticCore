package dev.xyat.kineticcore.feature.attribute.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.feature.attribute.config.AttributeConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/** Owns the client-only attribute translation refresh hook. */
public final class AttributeFixClientHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    private static boolean translationsRefreshed;

    private AttributeFixClientHandler() {
    }

    /** Registers the title-screen translation refresh hook on the physical client. */
    public static void register() {
        REGISTRATION.run(() -> KineticClientEvents.onScreenInitAfter(context -> onScreenInit(context.screen())));
    }

    private static void onScreenInit(Screen screen) {
        if (!(screen instanceof TitleScreen) || translationsRefreshed) {
            return;
        }

        try {
            AttributeConfig.refreshTranslationsAndSave();
            translationsRefreshed = true;
        } catch (Exception exception) {
            KineticRuntime.logger().error("AttributeConfig: 刷新翻译时发生错误", exception);
        }
    }
}
