package dev.xyat.kineticcore.api.flight;

import dev.xyat.kineticcore.internal.flight.KineticFlightSourcesRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * Public flight-source ownership API shared by KineticCore features and addons.
 */
public final class KineticFlightSources {
    private KineticFlightSources() {
    }

    /** Returns a detached snapshot of the persistent flight-source ids owned by the player. */
    public static Set<String> sources(Player player) {
        return KineticFlightSourcesRuntime.sources(player);
    }

    /** Adds one persistent flight source and refreshes server-side flight abilities when the source is new. */
    public static void addSource(LivingEntity entity, String sourceId) {
        KineticFlightSourcesRuntime.addSource(entity, sourceId);
    }

    /** Removes one persistent flight source and refreshes server-side flight abilities when the source existed. */
    public static void removeSource(LivingEntity entity, String sourceId) {
        KineticFlightSourcesRuntime.removeSource(entity, sourceId);
    }

    /** Returns whether the player is currently allowed to fly through creative/spectator state or any registered source. */
    public static boolean allowsFlight(Player player) {
        return KineticFlightSourcesRuntime.allowsFlight(player);
    }

    /** Reapplies the authoritative server-side flight ability state for the player. */
    public static void refresh(Player player) {
        KineticFlightSourcesRuntime.refresh(player);
    }

    /** Copies only the persistent flight-source payload between player instances. */
    public static void copySources(Player oldPlayer, Player newPlayer) {
        KineticFlightSourcesRuntime.copySources(oldPlayer, newPlayer);
    }

    /** Returns whether Kinetic is currently synchronizing an ability update caused by the flight-source API. */
    public static boolean abilityRefreshInProgress() {
        return KineticFlightSourcesRuntime.abilityRefreshInProgress();
    }
}
