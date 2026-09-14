package dev.xyat.kineticcore.internal.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;

public final class MinecraftKeysImpl {
    private MinecraftKeysImpl() {
    }

    public static void setDefault(KeyMapping mapping, InputConstants.Key key, KineticKeyBindings.Modifier modifier) {
        KeyMappingAccess access = (KeyMappingAccess) mapping;
        access.kineticcore$setDefaultKey(key);
        access.kineticcore$setDefaultModifier(toForge(modifier));
    }

    public static KineticKeyBindings.Modifier defaultModifier(KeyMapping mapping) {
        return fromForge(((KeyMappingAccess) mapping).kineticcore$getDefaultModifier());
    }

    public static KineticKeyBindings.Modifier modifier(KeyMapping mapping) {
        return fromForge(((KeyMappingAccess) mapping).kineticcore$getModifier());
    }

    private static KeyModifier toForge(KineticKeyBindings.Modifier modifier) {
        return switch (modifier == null ? KineticKeyBindings.Modifier.NONE : modifier) {
            case NONE -> KeyModifier.NONE;
            case SHIFT -> KeyModifier.SHIFT;
            case CONTROL -> KeyModifier.CONTROL;
            case ALT -> KeyModifier.ALT;
        };
    }

    private static KineticKeyBindings.Modifier fromForge(KeyModifier modifier) {
        if (modifier == null) return KineticKeyBindings.Modifier.NONE;
        return switch (modifier) {
            case SHIFT -> KineticKeyBindings.Modifier.SHIFT;
            case CONTROL -> KineticKeyBindings.Modifier.CONTROL;
            case ALT -> KineticKeyBindings.Modifier.ALT;
            default -> KineticKeyBindings.Modifier.NONE;
        };
    }
}
