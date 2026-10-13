package dev.xyat.kineticcore.api.client.gui.editor;

import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.config.client.*;
import dev.xyat.kineticcore.api.itemcreator.ItemCreatorStackData;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.internal.client.editor.itemcreator.ItemCreatorEditorPage;

/** Shared mob-template and custom-potion editors shown in the core configuration hub. */
public final class KineticItemCreatorEditors {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    private KineticItemCreatorEditors() { }
    /** Opens the spawn-egg editor and returns to the current screen on Back. */
    public static void openSpawnEgg() { KineticGui.openChild(new ItemCreatorEditorPage(false)); }
    /** Opens the custom-potion editor and returns to the current screen on Back. */
    public static void openPotion() { KineticGui.openChild(new ItemCreatorEditorPage(true)); }
    /** Installs the potion configuration entry and special-egg tooltip once on the client. */
    public static void register() {
        REGISTRATION.run(
            () -> dev.xyat.kineticcore.api.client.event.KineticClientEvents.onLogout(dev.xyat.kineticcore.internal.itemcreator.ItemCreatorNetwork::disconnected),
            () -> KTConfigApi.register(KTConfigPage.builder("kineticcore:potion_editor", KineticI18n.translatable("gui.kineticcore.item_creator.potion_title"))
                    .scope(KTConfigScope.LOCAL_INSTALLATION).applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                    .action("open_editor", KineticI18n.translatable("gui.kineticcore.item_creator.potion_title"), KineticItemCreatorEditors::openPotion,
                            KineticI18n.translatable("gui.kineticcore.item_creator.list_hint")).build()),
            () -> KineticItemTooltips.onBuild((stack, lines) -> {
                try {
                    var template = ItemCreatorStackData.readTemplate(stack).orElse(null);
                    if (template == null) return;
                    lines.add(KineticI18n.translatable("gui.kineticcore.item_creator.egg_hint"));
                    for (var spec : template.attributes()) {
                        var attribute = KineticRegistries.attributes().get(KineticResourceIds.tryParse(spec.id()));
                        if (attribute != null) lines.add(KineticI18n.translatable(attribute.getDescriptionId()).append(": ")
                                .append(KineticI18n.translatable("gui.kineticcore.item_creator." + spec.operation().name().toLowerCase(java.util.Locale.ROOT)))
                                .append(" " + (spec.operation() == dev.xyat.kineticcore.api.itemcreator.AttributeSpec.Operation.MULTIPLY_TOTAL ? spec.amount() + 1 : spec.amount())));
                    }
                    for (var spec : template.effects()) {
                        var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(spec.id()));
                        if (effect != null) lines.add(effect.getDisplayName().copy().append(" " + (spec.amplifier() + 1) + " · ").append(spec.durationTicks() == -1
                                ? KineticI18n.translatable("gui.kineticcore.item_creator.permanent") : net.minecraft.network.chat.Component.literal(spec.durationTicks() / 20D + "s")));
                    }
                } catch (IllegalArgumentException ignored) { }
            })
        );
    }
}
