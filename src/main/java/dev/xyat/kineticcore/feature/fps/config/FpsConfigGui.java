package dev.xyat.kineticcore.feature.fps.config;

import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.feature.fps.client.FpsHudEditorScreen;
import net.minecraft.network.chat.Component;

@KTClientModule
public final class FpsConfigGui {
    public static final String PAGE_ID = "kineticcore:fps";

    private FpsConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.fps.title"),
                        FpsClientConfig.SPEC,
                        "FpsHud.enabled"::equals
                )
                .pageDescription(Component.translatable("cfg.kineticcore.fps.description"))
                .applyNotice(Component.translatable("cfg.kineticcore.fps.apply_notice"))
                .onSave(FpsClientConfig::save)
                .action(
                        "open_editor",
                        Component.translatable("cfg.kineticcore.hud.open_editor"),
                        KTConfigApi.screenAction(FpsHudEditorScreen::new),
                        Component.translatable("cfg.kineticcore.hud.open_editor.tooltip")
                )
                .build());
    }
}
