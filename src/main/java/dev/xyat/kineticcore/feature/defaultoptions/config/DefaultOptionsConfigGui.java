package dev.xyat.kineticcore.feature.defaultoptions.config;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
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
                        KineticText.translatable("cfg.kineticcore.default_options")
                )
                .pageDescription(KineticText.translatable("cfg.kineticcore.default_options.description"))
                .scope(KTConfigScope.LOCAL_INSTALLATION)
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .action(
                        "save_current_options",
                        KineticText.translatable("cfg.kineticcore.default_options.save"),
                        DefaultOptionsConfigGui::confirmSave,
                        KineticText.translatable("cfg.kineticcore.default_options.save.tooltip")
                )
                .build());
    }

    private static void confirmSave() {
        GuiOverlay.openCurrentDialog(
                KineticText.translatable("gui.kineticcore.default_options.confirm.title"),
                KineticText.translatable("gui.kineticcore.default_options.confirm.message"),
                KineticText.translatable("gui.yes"),
                KineticText.translatable("gui.no"),
                DefaultOptionsConfigGui::saveCurrentOptions,
                () -> { }
        );
    }

    private static void saveCurrentOptions() {
        try {
            OptionsManager.saveAllSettingsAsDefault();
            GuiOverlay.toast(
                    TOAST_ID,
                    KineticText.translatable("gui.kineticcore.default_options.save.success")
            );
        } catch (Exception exception) {
            KineticRuntime.logger().error("Failed to save the current options as defaults", exception);

            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = exception.getClass().getSimpleName();
            }
            GuiOverlay.toast(
                    TOAST_ID,
                    KineticText.translatable(
                            "gui.kineticcore.default_options.save.failure",
                            Component.literal(detail)
                    )
            );
        }
    }
}
