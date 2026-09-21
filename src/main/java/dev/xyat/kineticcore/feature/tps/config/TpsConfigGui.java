package dev.xyat.kineticcore.feature.tps.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.feature.tps.client.TpsHudEditorScreen;
import dev.xyat.kineticcore.feature.tps.network.TpsNetwork;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;

public final class TpsConfigGui {
    public static final String PAGE_ID = "kineticcore:tps";

    private TpsConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.filteredPageBuilder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.tps.title"),
                        TpsClientConfig.SPEC,
                        "TpsHud.enabled"::equals
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.tps.description"))
                .applyNotice(KineticI18n.translatable("cfg.kineticcore.tps.apply_notice"))
                .onSave(TpsConfigGui::saveAndSync)
                .action(
                        "open_editor",
                        KineticI18n.translatable("cfg.kineticcore.hud.open_editor"),
                        KTConfigApi.screenAction(TpsHudEditorScreen::new),
                        KineticI18n.translatable("cfg.kineticcore.hud.open_editor.tooltip")
                )
                .build());
    }

    private static void saveAndSync() {
        TpsClientConfig.save();
        if (KineticClientRuntime.connected() && KineticClientRuntime.localPlayer() != null) {
            TpsNetwork.sendSubscription(TpsClientConfig.isHudEnabled());
        }
    }
}
