package dev.xyat.kineticcore.api.minecraft;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.internal.client.input.MinecraftKeysImpl;
import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.Objects;

/**
 * Public Kinetic API for minecraft keys.
 */
public final class MinecraftKeys {
    private MinecraftKeys() {
    }

    /** Applies a serialized Minecraft key name as the default binding and reports whether it was valid. */
    public static boolean setDefaultSerialized(KeyMapping mapping, String serializedKey, KineticKeyBindings.Modifier modifier) {
        return MinecraftKeysImpl.setDefaultSerialized(mapping, serializedKey, modifier);
    }

    /** Returns the default modifier associated with the supplied key mapping. */
    public static KineticKeyBindings.Modifier defaultModifier(KeyMapping mapping) {
        return MinecraftKeysImpl.defaultModifier(mapping);
    }

    /** Returns the currently configured modifier for the supplied key mapping. */
    public static KineticKeyBindings.Modifier modifier(KeyMapping mapping) {
        return MinecraftKeysImpl.modifier(mapping);
    }

    /** Returns whether the current vanilla inventory key matches the supplied key event. */
    public static boolean inventoryMatches(int keyCode, int scanCode) {
        return MinecraftKeysImpl.inventoryMatches(keyCode, scanCode);
    }

    /** Returns an immutable snapshot of the supplied key-mapping array. */
    public static List<KeyMapping> snapshotMappings(KeyMapping[] mappings) {
        return MinecraftKeysImpl.snapshotMappings(Objects.requireNonNull(mappings, "mappings"));
    }

    /** Returns an immutable snapshot of the currently active client key mappings. */
    public static List<KeyMapping> currentMappings() {
        return MinecraftKeysImpl.currentMappings();
    }

    /** Returns whether the current vanilla inventory action is bound to the supplied named keyboard key. */
    public static boolean inventoryUses(KineticKeyBindings.Key key) {
        return MinecraftKeysImpl.inventoryUses(Objects.requireNonNull(key, "key"));
    }

    /** Rebuilds Minecraft's key-mapping lookup table after default bindings change. */
    public static void resetMappings() {
        MinecraftKeysImpl.resetMappings();
    }

}
