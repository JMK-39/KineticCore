package dev.xyat.kineticcore.feature.attribute.config;

import net.minecraft.ChatFormatting;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import dev.xyat.kineticcore.config.client.KTConfigScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Registers the registry-backed attribute editor in KT's single config hub. */
@KTClientModule
public final class AttributeConfigGui {
    public static final String PAGE_ID = "kineticcore:attributes";

    private static boolean pageRegistered;

    private AttributeConfigGui() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(AttributeConfigGui::onLoadComplete);
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            AttributeConfig.loadAndApply();
            registerPage();
        });
    }

    private static synchronized void registerPage() {
        if (pageRegistered) return;

        KTConfigPage page = KTConfigPage.builder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.attribute.title")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .pageDescription(Component.translatable("cfg.kineticcore.attribute.description"))
                .applyTiming(KTConfigPage.ApplyTiming.RESTART_GAME)
                .applyNotice(Component.translatable("cfg.kineticcore.attribute.restart_notice"))
                .section(Component.translatable("cfg.kineticcore.attribute.section.global"))
                .booleanValue(
                        "auto_scan",
                        Component.translatable("cfg.kineticcore.attribute.auto_scan"),
                        AttributeConfig::isAutoScanEnabled,
                        AttributeConfig::setAutoScanEnabled,
                        true,
                        Component.translatable("cfg.kineticcore.attribute.auto_scan.tooltip")
                )
                .description(Component.translatable("cfg.kineticcore.attribute.restart_notice"))
                .action(
                        "edit_attributes",
                        Component.translatable("cfg.kineticcore.attribute.edit"),
                        KTConfigApi.screenAction(parent -> new KTConfigScreen(parent, buildAttributeEditorPage())),
                        Component.translatable("cfg.kineticcore.attribute.edit.tooltip")
                )
                .build();

        KTConfigApi.register(page);
        pageRegistered = true;
    }

    private static KTConfigPage buildAttributeEditorPage() {
        KTConfigPage.Builder page = KTConfigPage.builder(
                        PAGE_ID + "/editor",
                        Component.translatable("cfg.kineticcore.attribute.section.attributes")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .pageDescription(Component.translatable("cfg.kineticcore.attribute.description"))
                .applyTiming(KTConfigPage.ApplyTiming.RESTART_GAME)
                .applyNotice(Component.translatable("cfg.kineticcore.attribute.restart_notice"))
                .section(Component.translatable("cfg.kineticcore.attribute.section.attributes"));

        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : sortedRangedAttributes()) {
            ResourceLocation id = entry.getKey().location();
            RangedAttribute attribute = (RangedAttribute) entry.getValue();
            AttributeConfig.AttributeSettings defaults = AttributeConfig.getDefaultSettings(id);
            AttributeConfig.AttributeSettings current = AttributeConfig.getAttributeSettings(id);
            String entryPrefix = AttributeConfig.stableEntryPrefix(id);
            Component displayName = Component.translatable(attribute.getDescriptionId());

            page.booleanValue(
                    entryPrefix + "_enabled",
                    Component.translatable(
                            "cfg.kineticcore.attribute.enabled",
                            displayName.copy().withStyle(ChatFormatting.AQUA)
                    ),
                    () -> AttributeConfig.getAttributeSettings(id).enabled(),
                    value -> AttributeConfig.setAttributeEnabled(id, value),
                    defaults.enabled(),
                    Component.translatable(
                            "cfg.kineticcore.attribute.enabled.tooltip",
                            Component.literal(id.toString()).withStyle(ChatFormatting.GOLD)
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
        Component label = Component.translatable(
                minimum ? "cfg.kineticcore.attribute.minimum" : "cfg.kineticcore.attribute.maximum",
                displayName.copy().withStyle(ChatFormatting.AQUA)
        );
        Component tooltip = Component.translatable(
                minimum ? "cfg.kineticcore.attribute.minimum.tooltip" : "cfg.kineticcore.attribute.maximum.tooltip",
                Component.literal(id.toString()).withStyle(ChatFormatting.GOLD)
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
                tooltip.copy().append("\n").append(Component.translatable(
                        "cfg.kineticcore.attribute.infinity.tooltip"))
        );
    }

    private static List<Map.Entry<ResourceKey<Attribute>, Attribute>> sortedRangedAttributes() {
        return ForgeRegistries.ATTRIBUTES.getEntries().stream()
                .filter(entry -> entry.getValue() instanceof RangedAttribute)
                .sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
                .toList();
    }

}
