package dev.xyat.kineticcore.bootstrap.config.client;

import dev.xyat.kineticcore.MixinPlugin;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;

/** Startup-only core switches. Changes are persisted now and applied on restart. */
@KTClientModule
public final class MixinConfigGui {
    public static final String PAGE_ID = "kineticcore:mixin_toggles";

    private MixinConfigGui() {
    }

    public static void load() {
        java.util.List<MixinPlugin.ToggleDescriptor> descriptors = MixinPlugin.toggleDescriptors();
        if (descriptors.isEmpty()) return;

        Component restartNotice = Component.translatable("cfg.kineticcore.mixin_toggles.restart");
        KTConfigPage.Builder page = KTConfigPage.builder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.mixin_toggles.title")
                )
                .scope(KTConfigScope.LOCAL_INSTALLATION)
                .applyTiming(KTConfigPage.ApplyTiming.RESTART_GAME)
                .applyNotice(restartNotice)
                .pageDescription(restartNotice)
                .description(restartNotice);

        int index = 0;
        for (MixinPlugin.ToggleDescriptor descriptor : descriptors) {
            if (descriptor.header()) {
                page.section(Component.translatable(sectionTranslationKey(descriptor.titleEn())));
                continue;
            }
            String key = descriptor.key();
            page.booleanValue(
                    "mixin_" + index++,
                    Component.literal(key),
                    () -> MixinPlugin.configuredFeatureEnabled(key),
                    value -> MixinPlugin.setConfiguredFeatureEnabled(key, value),
                    descriptor.defaultValue(),
                    Component.translatable(toggleTooltipTranslationKey(key))
                            .copy()
                            .append("\n")
                            .append(restartNotice)
            );
        }

        KTConfigApi.register(page.onSave(MixinPlugin::saveConfiguredToggles).build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, PAGE_ID);
    }

    private static String sectionTranslationKey(String englishTitle) {
        String slug = englishTitle.toLowerCase(Locale.ROOT)
                .replace("&", "and")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return "cfg.kineticcore.mixin_toggles.section." + slug;
    }

    private static String toggleTooltipTranslationKey(String key) {
        return "cfg.kineticcore.mixin_toggles.entry." + key + ".tooltip";
    }
}
