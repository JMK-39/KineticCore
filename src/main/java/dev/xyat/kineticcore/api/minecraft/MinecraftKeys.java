package dev.xyat.kineticcore.api.minecraft;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.internal.client.input.MinecraftKeysImpl;
import net.minecraft.client.KeyMapping;

public final class MinecraftKeys {
    private MinecraftKeys() {
    }

    public static void setDefault(KeyMapping mapping, InputConstants.Key key, KineticKeyBindings.Modifier modifier) {
        MinecraftKeysImpl.setDefault(mapping, key, modifier);
    }

    public static KineticKeyBindings.Modifier defaultModifier(KeyMapping mapping) {
        return MinecraftKeysImpl.defaultModifier(mapping);
    }

    public static KineticKeyBindings.Modifier modifier(KeyMapping mapping) {
        return MinecraftKeysImpl.modifier(mapping);
    }
}
