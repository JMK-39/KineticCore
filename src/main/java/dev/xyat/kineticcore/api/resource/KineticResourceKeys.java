package dev.xyat.kineticcore.api.resource;

import dev.xyat.kineticcore.internal.resource.KineticResourceKeyRuntime;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Creates common Minecraft resource keys without exposing registry-key construction details to addons.
 */
public final class KineticResourceKeys {
    private KineticResourceKeys() {
    }

    /** Creates a dimension key for the supplied resource identifier. */
    public static ResourceKey<Level> dimension(ResourceLocation id) {
        return KineticResourceKeyRuntime.dimension(Objects.requireNonNull(id, "id"));
    }

    /** Creates a dimension key from validated namespace and path components. */
    public static ResourceKey<Level> dimension(String namespace, String path) {
        return dimension(KineticResourceIds.of(namespace, path));
    }

    /** Creates a damage-type key for the supplied resource identifier. */
    public static ResourceKey<DamageType> damageType(ResourceLocation id) {
        return KineticResourceKeyRuntime.damageType(Objects.requireNonNull(id, "id"));
    }

    /** Creates a damage-type key from validated namespace and path components. */
    public static ResourceKey<DamageType> damageType(String namespace, String path) {
        return damageType(KineticResourceIds.of(namespace, path));
    }
}
