package dev.xyat.kineticcore.feature.startup.config;

import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import net.minecraft.network.chat.Component;

/** Visual editor for the title-screen startup overlay's CLIENT spec. */
@KTClientModule
public final class StartupConfigGui {
    public static final String PAGE_ID = "kineticcore:startup_overlay";

    private StartupConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.startup.title"),
                        StartupConfig.SPEC
                )
                .pageDescription(Component.translatable("cfg.kineticcore.startup.description"))
                .build());
    }
}
