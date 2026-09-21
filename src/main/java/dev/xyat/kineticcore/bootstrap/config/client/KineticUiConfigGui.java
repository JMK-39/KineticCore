package dev.xyat.kineticcore.bootstrap.config.client;

import dev.xyat.kineticcore.api.client.widget.scroll.KineticScrollSettings;
import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.text.KineticI18n;

/** Registers the core UI interaction settings page. */
public final class KineticUiConfigGui {
    public static final String PAGE_ID = "kineticcore:ui";

    private KineticUiConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.ui.title"),
                        KineticScrollSettings.configSpec()
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.ui.description"))
                .onSave(KineticScrollSettings::save)
                .build());
    }
}
