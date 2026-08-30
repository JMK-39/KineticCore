package dev.xyat.kineticcore.feature.effects.config;

import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.feature.effects.client.MiniEffectsFeature;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@KTClientModule
public final class MiniEffectsConfigGui {
    public static final String PAGE_ID = "kineticcore:mini_effects";

    private MiniEffectsConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.mini_effects.title"),
                        MiniEffectsFeature.CLIENT_SPEC
                )
                .pageDescription(Component.translatable("cfg.kineticcore.mini_effects.description"))
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, PAGE_ID);
    }
}
