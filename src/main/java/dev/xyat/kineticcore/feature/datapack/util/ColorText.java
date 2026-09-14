package dev.xyat.kineticcore.feature.datapack.util;

import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.MutableComponent;

public final class ColorText {
    private ColorText() {
    }

    public static MutableComponent translatable(String key, Object... args) {
        return KineticI18n.translatable(key, args);
    }
}
