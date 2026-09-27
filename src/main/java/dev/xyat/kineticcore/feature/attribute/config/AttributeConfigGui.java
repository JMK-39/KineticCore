package dev.xyat.kineticcore.feature.attribute.config;

import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.runtime.KineticModLifecycle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Registers the registry-backed attribute editor in KT's single config hub. */
public final class AttributeConfigGui {
    public static final String PAGE_ID = "kineticcore:attributes";

    private static boolean pageRegistered;

    private AttributeConfigGui() {
    }

    public static void register() {
        KineticModLifecycle.onLoadComplete(AttributeConfigGui::registerPage);
    }

    private static synchronized void registerPage() {
        if (pageRegistered) return;

        KTConfigPage page = KTConfigPage.builder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.attribute.title")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.attribute.description"))
                .applyTiming(KTConfigPage.ApplyTiming.RESTART_GAME)
                .applyNotice(KineticI18n.translatable("cfg.kineticcore.attribute.restart_notice"))
                .booleanValue(
                        "auto_scan",
                        KineticI18n.translatable("cfg.kineticcore.attribute.auto_scan"),
                        AttributeConfig::isAutoScanEnabled,
                        AttributeConfig::setAutoScanEnabled,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.attribute.auto_scan.tooltip")
                )
                .description(KineticI18n.translatable("cfg.kineticcore.attribute.restart_notice"))
                .action(
                        "edit_attributes",
                        KineticI18n.translatable("cfg.kineticcore.attribute.edit"),
                        KTConfigApi.configPageAction(AttributeConfigGui::buildAttributeEditorPage),
                        KineticI18n.translatable("cfg.kineticcore.attribute.edit.tooltip")
                )
                .build();

        KTConfigApi.register(page);
        pageRegistered = true;
    }

    private static KTConfigPage buildAttributeEditorPage() {
        KTConfigPage.Builder page = KTConfigPage.builder(
                        PAGE_ID + "/editor",
                        KineticI18n.translatable("cfg.kineticcore.attribute.section.attributes")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .pageDescription(KineticI18n.translatable("cfg.kineticcore.attribute.description"))
                .applyTiming(KTConfigPage.ApplyTiming.RESTART_GAME)
                .applyNotice(KineticI18n.translatable("cfg.kineticcore.attribute.restart_notice"));

        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : sortedRangedAttributes()) {
            ResourceLocation id = entry.getKey().location();
            RangedAttribute attribute = (RangedAttribute) entry.getValue();
            AttributeConfig.AttributeSettings defaults = AttributeConfig.getDefaultSettings(id);
            AttributeConfig.AttributeSettings current = AttributeConfig.getAttributeSettings(id);
            String entryPrefix = AttributeConfig.stableEntryPrefix(id);
            Component displayName = KineticI18n.translatable(attribute.getDescriptionId());

            page.booleanValue(
                    entryPrefix + "_enabled",
                    KineticI18n.translatable(
                            "cfg.kineticcore.attribute.enabled",
                            displayName
                    ),
                    () -> AttributeConfig.getAttributeSettings(id).enabled(),
                    value -> AttributeConfig.setAttributeEnabled(id, value),
                    defaults.enabled(),
                    KineticI18n.translatable(
                            "cfg.kineticcore.attribute.enabled.tooltip",
                            Component.literal(id.toString())
                    )
            );
            addBoundary(page, id, entryPrefix, displayName, defaults, current, true);
            addBoundary(page, id, entryPrefix, displayName, defaults, current, false);
        }

        return page.build();
    }

    /**
     * Compact finite attributes keep using the shared NumericEditBox. Extreme
     * or infinite bounds use a local text codec so their canonical scientific
     * notation can round-trip without weakening the common numeric validator.
     */
    private static void addBoundary(
            KTConfigPage.Builder page,
            ResourceLocation id,
            String entryPrefix,
            Component displayName,
            AttributeConfig.AttributeSettings defaults,
            AttributeConfig.AttributeSettings current,
            boolean minimum
    ) {
        String suffix = minimum ? "minimum" : "maximum";
        double defaultValue = minimum ? defaults.minimum() : defaults.maximum();
        Component label = KineticI18n.translatable(
                minimum ? "cfg.kineticcore.attribute.minimum" : "cfg.kineticcore.attribute.maximum",
                displayName
        );
        Component tooltip = KineticI18n.translatable(
                minimum ? "cfg.kineticcore.attribute.minimum.tooltip" : "cfg.kineticcore.attribute.maximum.tooltip",
                Component.literal(id.toString())
        );

        page.stringValue(
                entryPrefix + '_' + suffix,
                label,
                () -> AttributeConfig.formatEditableBoundary(minimum
                        ? AttributeConfig.getAttributeSettings(id).minimum()
                        : AttributeConfig.getAttributeSettings(id).maximum()),
                value -> {
                    if (minimum) AttributeConfig.setAttributeMinimumText(id, value);
                    else AttributeConfig.setAttributeMaximumText(id, value);
                },
                AttributeConfig.formatEditableBoundary(defaultValue),
                tooltip.copy().append("\n").append(KineticI18n.translatable(
                        "cfg.kineticcore.attribute.infinity.tooltip"))
        );
    }

    private static List<Map.Entry<ResourceKey<Attribute>, Attribute>> sortedRangedAttributes() {
        return KineticRegistries.attributes().entries().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof RangedAttribute)
                .map(entry -> Map.entry(ResourceKey.create(Registries.ATTRIBUTE, entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
                .toList();
    }

}
