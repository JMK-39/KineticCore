package dev.xyat.kineticcore.config.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

public final class KTModuleDisplay {
    private KTModuleDisplay() {
    }

    public static Component moduleName(String modId) {
        String key = nameKey(modId);
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        String fallback = ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
        return Component.literal(removeWhitespace(fallback));
    }

    public static Component moduleFunction(String modId) {
        String key = functionKey(modId);
        return I18n.exists(key) ? Component.translatable(key) : Component.empty();
    }

    public static Component moduleTooltip(String modId) {
        String key = tooltipKey(modId);
        return I18n.exists(key) ? Component.translatable(key) : null;
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
