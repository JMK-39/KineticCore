package dev.xyat.kineticcore.internal.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Internal registry bridge for public Kinetic damage-source creation helpers. */
public final class KineticDamageSourceRuntime {
    private KineticDamageSourceRuntime() {
    }

    /** Creates one damage source from the level's registered damage-type holder. */
    public static DamageSource fromKey(Level level, ResourceKey<DamageType> damageType, Entity causingEntity) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageType),
                causingEntity
        );
    }
}
