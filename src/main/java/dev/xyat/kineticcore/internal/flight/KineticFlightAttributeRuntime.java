package dev.xyat.kineticcore.internal.flight;

import dev.xyat.kineticcore.internal.registry.KineticEntityAttributeRuntime;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Forge-backed registration/runtime for the public Kinetic flight attributes. */
public final class KineticFlightAttributeRuntime {
    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "kineticcore");

    private static final RegistryObject<Attribute> FLIGHT_SPEED = ATTRIBUTES.register(
            "flight_speed",
            () -> new RangedAttribute("attribute.name.kineticcore.flight_speed", 0.0D, 0.0D, 20.0D)
                    .setSyncable(true)
    );

    private static final RegistryObject<Attribute> TURN_DAMPING = ATTRIBUTES.register(
            "flight_turn_damping",
            () -> new RangedAttribute("attribute.name.kineticcore.flight_turn_damping", 0.3D, 0.0D, 1.0D)
                    .setSyncable(true)
    );

    private static boolean registered;

    private KineticFlightAttributeRuntime() {
    }

    public static synchronized void register() {
        if (registered) return;
        var context = FMLJavaModLoadingContext.get();
        if (context == null) {
            throw new IllegalStateException("Kinetic flight attributes must be registered during mod construction");
        }
        ATTRIBUTES.register(context.getModEventBus());
        KineticEntityAttributeRuntime.registerModification(contextView -> {
            Attribute speed = FLIGHT_SPEED.get();
            Attribute damping = TURN_DAMPING.get();
            if (!contextView.has(EntityType.PLAYER, speed)) contextView.add(EntityType.PLAYER, speed);
            if (!contextView.has(EntityType.PLAYER, damping)) contextView.add(EntityType.PLAYER, damping);
        });
        registered = true;
    }

    public static Attribute flightSpeed() {
        return FLIGHT_SPEED.get();
    }

    public static Attribute turnDamping() {
        return TURN_DAMPING.get();
    }

    public static double flightSpeed(LivingEntity entity) {
        if (entity == null || entity.getAttribute(flightSpeed()) == null) return 0.0D;
        return Math.max(0.0D, entity.getAttributeValue(flightSpeed()));
    }

    public static double turnDamping(LivingEntity entity) {
        if (entity == null || entity.getAttribute(turnDamping()) == null) return 0.3D;
        return Math.max(0.0D, Math.min(1.0D, entity.getAttributeValue(turnDamping())));
    }
}
