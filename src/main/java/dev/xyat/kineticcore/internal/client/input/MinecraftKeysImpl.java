package dev.xyat.kineticcore.internal.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class MinecraftKeysImpl {
    private MinecraftKeysImpl() {
    }

    private static void setDefault(KeyMapping mapping, InputConstants.Key key, KineticKeyBindings.Modifier modifier) {
        KeyMappingAccess access = (KeyMappingAccess) mapping;
        access.kineticcore$setDefaultKey(key);
        access.kineticcore$setDefaultModifier(toForge(modifier));
    }

    public static boolean setDefaultSerialized(KeyMapping mapping, String serializedKey, KineticKeyBindings.Modifier modifier) {
        if (mapping == null || serializedKey == null || serializedKey.isBlank()) return false;
        try {
            setDefault(mapping, InputConstants.getKey(serializedKey), modifier);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static KineticKeyBindings.Modifier defaultModifier(KeyMapping mapping) {
        return fromForge(((KeyMappingAccess) mapping).kineticcore$getDefaultModifier());
    }

    public static KineticKeyBindings.Modifier modifier(KeyMapping mapping) {
        return fromForge(((KeyMappingAccess) mapping).kineticcore$getModifier());
    }


    public static boolean inventoryMatches(int keyCode, int scanCode) {
        return Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode);
    }

    public static List<KeyMapping> snapshotMappings(KeyMapping[] mappings) {
        Objects.requireNonNull(mappings, "mappings");
        return List.copyOf(Arrays.asList(mappings));
    }

    public static List<KeyMapping> currentMappings() {
        var options = Minecraft.getInstance().options;
        return options == null ? List.of() : snapshotMappings(options.keyMappings);
    }

    public static boolean inventoryUses(KineticKeyBindings.Key key) {
        return KineticKeyBindings.matchesKeyCode(key, Minecraft.getInstance().options.keyInventory.getKey().getValue());
    }

    public static void resetMappings() {
        KeyMapping.resetMapping();
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
