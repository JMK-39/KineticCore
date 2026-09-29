package dev.xyat.kineticcore.api.event;

/**
 * Order of Kinetic event handlers; maps to the Forge priority of the same name. Handlers with equal priority run in
 * registration order.
 */
public enum KineticEventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST
}
