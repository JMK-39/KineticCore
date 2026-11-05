package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Names of the effects and attributes listed in the potion and spawn-egg editors. */
final class ItemCreatorEntryText {
    private ItemCreatorEntryText() {
    }

    /**
     * The row label: the ID in white and, outside English, the translated name in gold after it. In English the ID
     * already describes the entry, so no translation is repeated.
     */
    static Component label(boolean effects, String id) {
        var label = Component.literal(id).withStyle(ChatFormatting.WHITE);
        if (dev.xyat.kineticcore.api.runtime.KineticClientRuntime.selectedLanguage().startsWith("en_")) return label;
        var name = name(effects, id);
        return name.getString().equals(id) ? label : label.append(Component.literal(" — ").withStyle(ChatFormatting.GRAY)).append(name.copy().withStyle(ChatFormatting.GOLD));
    }

    static Component name(boolean effects, String id) {
        var key = KineticResourceIds.tryParse(id);
        if (key == null) return Component.literal(id);
        if (effects) {
            var effect = KineticRegistries.mobEffects().get(key);
            return effect == null || !KineticI18n.hasTranslation(effect.getDescriptionId()) ? Component.literal(id) : effect.getDisplayName();
        }
        var attribute = KineticRegistries.attributes().get(key);
        return attribute == null || !KineticI18n.hasTranslation(attribute.getDescriptionId()) ? Component.literal(id) : Component.translatable(attribute.getDescriptionId());
    }
}
