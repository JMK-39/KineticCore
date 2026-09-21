package dev.xyat.kineticcore.api.entity;

import dev.xyat.kineticcore.internal.entity.KineticDamageSourceRuntime;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Creates damage sources from registered damage-type keys without exposing registry lookup details to addons.
 */
public final class KineticDamageSources {
    private KineticDamageSources() {
    }

    /**
     * Creates a damage source backed by the supplied registered damage type and causing entity.
     *
     * @param level level whose registry access owns the damage type
     * @param damageType registered damage-type key
     * @param causingEntity entity responsible for the damage
     * @return damage source bound to the requested type and entity
     */
    public static DamageSource fromKey(
            Level level,
            ResourceKey<DamageType> damageType,
            Entity causingEntity
    ) {
        return KineticDamageSourceRuntime.fromKey(
                Objects.requireNonNull(level, "level"),
                Objects.requireNonNull(damageType, "damageType"),
                Objects.requireNonNull(causingEntity, "causingEntity")
        );
    }
}
