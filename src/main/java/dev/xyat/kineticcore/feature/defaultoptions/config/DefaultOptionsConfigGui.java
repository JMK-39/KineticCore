package dev.xyat.kineticcore.feature.defaultoptions.config;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.feature.defaultoptions.OptionsManager;
import net.minecraft.network.chat.Component;

public final class DefaultOptionsConfigGui {
    public static final String PAGE_ID = "kineticcore:default_options";

    private static final String TOAST_ID = "kineticcore_default_options_save";

    private DefaultOptionsConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.default_options")
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.default_options.description"))
                .scope(KTConfigScope.LOCAL_INSTALLATION)
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .action(
                        "save_current_options",
                        KineticI18n.translatable("cfg.kineticcore.default_options.save"),
                        DefaultOptionsConfigGui::confirmSave,
                        KineticI18n.translatable("cfg.kineticcore.default_options.save.tooltip")
                )
                .build());
    }

    private static void confirmSave() {
        KineticOverlays.openCurrentDialog(
                KineticI18n.translatable("gui.kineticcore.default_options.confirm.title"),
                KineticI18n.translatable("gui.kineticcore.default_options.confirm.message"),
                KineticI18n.translatable("gui.yes"),
                KineticI18n.translatable("gui.no"),
                DefaultOptionsConfigGui::saveCurrentOptions,
                () -> { }
        );
    }

    private static void saveCurrentOptions() {
        try {
            OptionsManager.saveAllSettingsAsDefault();
            KineticOverlays.toast(TOAST_ID, KineticI18n.translatable("gui.kineticcore.default_options.save.success"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        } catch (Exception exception) {
            KineticRuntime.logger().error("Failed to save the current options as defaults", exception);

            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = exception.getClass().getSimpleName();
            }
            KineticOverlays.toast(TOAST_ID, KineticI18n.translatable(
                            "gui.kineticcore.default_options.save.failure",
                            Component.literal(detail)
                    ), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        }
    }
}
