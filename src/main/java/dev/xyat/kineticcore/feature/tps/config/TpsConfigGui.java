package dev.xyat.kineticcore.feature.tps.config;

import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.feature.tps.client.TpsHudEditorScreen;
import dev.xyat.kineticcore.feature.tps.network.TpsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@KTClientModule
public final class TpsConfigGui {
    public static final String PAGE_ID = "kineticcore:tps";

    private TpsConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.tps.title"),
                        TpsClientConfig.SPEC,
                        "TpsHud.enabled"::equals
                )
                .pageDescription(Component.translatable("cfg.kineticcore.tps.description"))
                .applyNotice(Component.translatable("cfg.kineticcore.tps.apply_notice"))
                .onSave(TpsConfigGui::saveAndSync)
                .action(
                        "open_editor",
                        Component.translatable("cfg.kineticcore.hud.open_editor"),
                        KTConfigApi.screenAction(TpsHudEditorScreen::new),
                        Component.translatable("cfg.kineticcore.hud.open_editor.tooltip")
                )
                .build());
    }

    private static void saveAndSync() {
        TpsClientConfig.save();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null && minecraft.player != null) {
            TpsNetwork.sendSubscription(TpsClientConfig.isHudEnabled());
        }
    }
}
