package dev.xyat.kineticcore.internal.resource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Internal resource-key construction bridge for the public Kinetic resource API. */
public final class KineticResourceKeyRuntime {
    private KineticResourceKeyRuntime() {
    }

    /** Creates a dimension resource key. */
    public static ResourceKey<Level> dimension(ResourceLocation id) {
        return ResourceKey.create(Registries.DIMENSION, id);
    }

    /** Creates a damage-type resource key. */
    public static ResourceKey<DamageType> damageType(ResourceLocation id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, id);
    }
}
