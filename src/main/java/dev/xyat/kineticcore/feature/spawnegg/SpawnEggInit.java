package dev.xyat.kineticcore.feature.spawnegg;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.feature.spawnegg.entity.ThrowSpawnEgg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@KTModule
public final class SpawnEggInit {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KineticCore.MODID);

    public static final RegistryObject<EntityType<ThrowSpawnEgg>> THROWABLE_SPAWN_EGG = ENTITY_TYPES.register(
            "throwable_spawn_egg",
            () -> EntityType.Builder.<ThrowSpawnEgg>of(ThrowSpawnEgg::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("throwable_spawn_egg")
    );

    private SpawnEggInit() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
