package dev.xyat.kineticcore.api;

import net.minecraft.resources.ResourceLocation;

public interface IKineticSpawnerAccessor {
    int kineticcore$getSpawnCount();
    long kineticcore$getCooldownEnd();
    ResourceLocation kineticcore$getEntityId();
}