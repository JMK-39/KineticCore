package dev.xyat.kineticcore.feature.flight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

/**
 * KineticCore flight feature state that is not part of the public addon API.
 */
public final class FlightState {
    private static final String NBT_LAST_FLYING = "last_known_flying";
    private static final String NBT_TUTORIAL_DISABLED = "kt_flight_tutorial_disabled";

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

    public static boolean tutorialDisabled(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag forgeData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        return forgeData.getBoolean(NBT_TUTORIAL_DISABLED);
    }

    public static void disableTutorial(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag forgeData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        forgeData.putBoolean(NBT_TUTORIAL_DISABLED, true);
        // The loader saves this with the player and copies it to the replacement player after death.
        persistentData.put(Player.PERSISTED_NBT_TAG, forgeData);
    }
}
