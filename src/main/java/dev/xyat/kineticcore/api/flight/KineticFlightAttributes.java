package dev.xyat.kineticcore.api.flight;

import dev.xyat.kineticcore.internal.flight.KineticFlightAttributeRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Public attribute contract for Kinetic super flight. */
public final class KineticFlightAttributes {
    private KineticFlightAttributes() {
    }

    /** Registers the built-in Kinetic flight attributes during core construction. */
    public static void register() {
        KineticFlightAttributeRuntime.register();
    }

    /** Super-flight capability attribute. A final value above zero grants the capability. */
    public static Attribute flightSpeed() {
        return KineticFlightAttributeRuntime.flightSpeed();
    }

    /** Turn damping attribute. 0 follows the mouse fully; 1 prevents flight-direction turning. */
    public static Attribute turnDamping() {
        return KineticFlightAttributeRuntime.turnDamping();
    }

    /** Returns the entity's final super-flight capability value, or zero when unavailable. */
    public static double flightSpeed(LivingEntity entity) {
        return KineticFlightAttributeRuntime.flightSpeed(entity);
    }

    /** Returns the entity's final turn damping clamped to the public 0..1 contract. */
    public static double turnDamping(LivingEntity entity) {
        return KineticFlightAttributeRuntime.turnDamping(entity);
    }
}
