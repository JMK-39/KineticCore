package dev.xyat.kineticcore.internal.client.text;

import net.minecraft.client.resources.language.I18n;

/** Internal client-language implementation backing the public Kinetic text facade. */
public final class KineticTextRuntime {
    private KineticTextRuntime() {
    }

    /** Returns whether the active client language contains the supplied translation key. */
    public static boolean hasTranslation(String key) {
        return key != null && !key.isBlank() && I18n.exists(key);
    }

    /** Resolves one translation using Minecraft's standard I18n formatting behavior. */
    public static String get(String key, Object... args) {
        return I18n.get(key, args);
    }
}
