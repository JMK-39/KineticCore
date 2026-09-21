package dev.xyat.kineticcore.feature.effects.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.feature.effects.client.MiniEffectsFeature;
import net.minecraft.client.gui.screens.Screen;

public final class MiniEffectsConfigGui {
    public static final String PAGE_ID = "kineticcore:mini_effects";

    private MiniEffectsConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.mini_effects.title"),
                        MiniEffectsFeature.CLIENT_SPEC
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.mini_effects.description"))
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createRegisteredPageScreen(parent, PAGE_ID);
    }
}
