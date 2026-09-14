package dev.xyat.kineticcore.feature.spawnegg;

import dev.xyat.kineticcore.api.registry.KineticEntityTypes;
import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.feature.spawnegg.entity.ThrowSpawnEgg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class SpawnEggInit {
    public static final KineticRegistryHandle<EntityType<ThrowSpawnEgg>> THROWABLE_SPAWN_EGG =
            KineticEntityTypes.register(
                    KineticRuntime.id("throwable_spawn_egg"),
                    () -> EntityType.Builder.<ThrowSpawnEgg>of(ThrowSpawnEgg::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("throwable_spawn_egg")
            );

    private SpawnEggInit() {
    }

    public static void register() {
    }
}
