package dev.xyat.kineticcore.feature.flight;

import net.minecraft.world.entity.player.Player;

/**
 * KineticCore flight feature state that is not part of the public addon API.
 */
public final class FlightState {
    private static final String NBT_LAST_FLYING = "last_known_flying";

    public static boolean isProcessingExplicitCancel;
    public static boolean isInternalUpdate;

    private FlightState() {
    }

    /** Sets last known flying. */
    public static void setLastKnownFlying(Player player, boolean flying) {
        player.getPersistentData().putBoolean(NBT_LAST_FLYING, flying);
    }

    /** Provides the last known flying operation exposed by this API. */
    public static boolean lastKnownFlying(Player player) {
        return player.getPersistentData().getBoolean(NBT_LAST_FLYING);
    }
}
