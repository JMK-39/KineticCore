package dev.xyat.kineticcore.feature.fps.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.feature.fps.client.FpsHudEditorScreen;

public final class FpsConfigGui {
    public static final String PAGE_ID = "kineticcore:fps";

    private FpsConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.filteredPageBuilder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.fps.title"),
                        FpsClientConfig.SPEC,
                        "FpsHud.enabled"::equals
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.fps.description"))
                .applyNotice(KineticI18n.translatable("cfg.kineticcore.fps.apply_notice"))
                .onSave(FpsClientConfig::save)
                .action(
                        "open_editor",
                        KineticI18n.translatable("cfg.kineticcore.hud.open_editor"),
                        KTConfigApi.screenAction(FpsHudEditorScreen::new),
                        KineticI18n.translatable("cfg.kineticcore.hud.open_editor.tooltip")
                )
                .build());
    }
}
