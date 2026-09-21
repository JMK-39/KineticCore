package dev.xyat.kineticcore.bootstrap.config.client;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class StartupFeatureConfigGui {
    public static final String PAGE_ID = "kineticcore:startup_features";

    private StartupFeatureConfigGui() {
    }

    public static void load() {
        List<KineticFeatureSwitches.Descriptor> descriptors = KineticFeatureSwitches.descriptors();
        if (descriptors.isEmpty()) return;

        Component restartNotice = KineticI18n.translatable("cfg.kineticcore.startup_features.restart");
        KTConfigPage.Builder page = KTConfigPage.builder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.startup_features.title")
                )
                .scope(KTConfigScope.LOCAL_INSTALLATION)
                .applyTiming(KTConfigPage.ApplyTiming.RESTART_GAME)
                .applyNotice(restartNotice)
                .pageDescription(restartNotice)
                .description(restartNotice);

        String previousSection = null;
        int index = 0;
        for (KineticFeatureSwitches.Descriptor descriptor : descriptors) {
            if (!descriptor.sectionTranslationKey().equals(previousSection)) {
                page.section(KineticI18n.translatable(descriptor.sectionTranslationKey()));
                previousSection = descriptor.sectionTranslationKey();
            }

            String featureId = descriptor.id();
            page.booleanValue(
                    "startup_feature_" + index++,
                    KineticI18n.translatable(descriptor.nameTranslationKey()),
                    () -> KineticFeatureSwitches.configuredEnabled(featureId),
                    value -> KineticFeatureSwitches.setConfiguredEnabled(featureId, value),
                    descriptor.defaultEnabled(),
                    KineticI18n.translatable(descriptor.tooltipTranslationKey())
                            .copy()
                            .append("\n")
                            .append(restartNotice)
            );
        }

        KTConfigApi.register(page.onSave(KineticFeatureSwitches::saveConfigured).build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createRegisteredPageScreen(parent, PAGE_ID);
    }
}
