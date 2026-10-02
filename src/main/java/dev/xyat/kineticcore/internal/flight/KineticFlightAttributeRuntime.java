package dev.xyat.kineticcore.internal.flight;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.internal.registry.KineticAttributeHolders;
import dev.xyat.kineticcore.internal.registry.KineticDeferredRegistryRuntime;
import dev.xyat.kineticcore.internal.registry.KineticEntityAttributeRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Registration/runtime for the public Kinetic flight attributes. */
public final class KineticFlightAttributeRuntime {
    private static KineticRegistryHandle<Attribute> flightSpeedHandle;
    private static KineticRegistryHandle<Attribute> turnDampingHandle;

    private static boolean registered;

    private KineticFlightAttributeRuntime() {
    }

    public static synchronized void register() {
        if (registered) return;
        flightSpeedHandle = KineticDeferredRegistryRuntime.register(
                Registries.ATTRIBUTE,
                ResourceLocation.fromNamespaceAndPath("kineticcore", "flight_speed"),
                () -> new RangedAttribute("attribute.name.kineticcore.flight_speed", 0.0D, 0.0D, 20.0D)
                        .setSyncable(true)
        );
        turnDampingHandle = KineticDeferredRegistryRuntime.register(
                Registries.ATTRIBUTE,
                ResourceLocation.fromNamespaceAndPath("kineticcore", "flight_turn_damping"),
                () -> new RangedAttribute("attribute.name.kineticcore.flight_turn_damping", 0.3D, 0.0D, 1.0D)
                        .setSyncable(true)
        );
        KineticEntityAttributeRuntime.registerModification(contextView -> {
            Attribute speed = flightSpeedHandle.get();
            Attribute damping = turnDampingHandle.get();
            if (!contextView.has(EntityType.PLAYER, speed)) contextView.add(EntityType.PLAYER, speed);
            if (!contextView.has(EntityType.PLAYER, damping)) contextView.add(EntityType.PLAYER, damping);
        });
        registered = true;
    }

    public static Attribute flightSpeed() {
        return flightSpeedHandle.get();
    }

    public static Attribute turnDamping() {
        return turnDampingHandle.get();
    }

    public static double flightSpeed(LivingEntity entity) {
        if (entity == null || entity.getAttribute(KineticAttributeHolders.of(flightSpeed())) == null) return 0.0D;
        return Math.max(0.0D, entity.getAttributeValue(KineticAttributeHolders.of(flightSpeed())));
    }

    public static double turnDamping(LivingEntity entity) {
        if (entity == null || entity.getAttribute(KineticAttributeHolders.of(turnDamping())) == null) return 0.3D;
        return Math.max(0.0D, Math.min(1.0D, entity.getAttributeValue(KineticAttributeHolders.of(turnDamping()))));
    }
}
