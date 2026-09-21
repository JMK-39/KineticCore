package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import net.minecraft.network.chat.Component;

final class KTModuleDisplay {
    private KTModuleDisplay() {
    }

    public static Component moduleName(String modId) {
        String key = nameKey(modId);
        if (KineticText.hasTranslation(key)) {
            return KineticText.translatable(key);
        }
        String fallback = KineticPlatform.displayName(modId);
        return Component.literal(removeWhitespace(fallback));
    }

    public static Component moduleFunction(String modId) {
        String key = functionKey(modId);
        return KineticText.hasTranslation(key) ? KineticText.translatable(key) : Component.empty();
    }

    public static Component moduleTooltip(String modId) {
        String key = tooltipKey(modId);
        return KineticText.hasTranslation(key) ? KineticText.translatable(key) : null;
    }

    public static String nameKey(String modId) {
        return "gui." + modId + ".module.name";
    }

    public static String functionKey(String modId) {
        return "gui." + modId + ".module.function";
    }

    public static String tooltipKey(String modId) {
        return "gui." + modId + ".module.tooltip";
    }

    private static String removeWhitespace(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll("\\s+", "");
    }
}
