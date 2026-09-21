package dev.xyat.kineticcore.feature.flight;

import dev.xyat.kineticcore.api.flight.KineticFlightSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * KineticCore flight feature state that is not part of the public addon API.
 */
public final class FlightState {
    private static final String NBT_LAST_FLYING = "last_known_flying";
    private static final String NBT_NOCLIP = "kt_noclip";

    private static final Set<UUID> DEBOUNCING_PLAYERS = new HashSet<>();
    private static volatile BiConsumer<ServerPlayer, Boolean> noclipSyncSender = (player, enabled) -> { };

    public static boolean isProcessingExplicitCancel;
    public static boolean isInternalUpdate;
    public static boolean isGamemodeSwitching;

    private FlightState() {
    }

    /** Installs noclip sync sender. */
    public static void installNoclipSyncSender(BiConsumer<ServerPlayer, Boolean> sender) {
        noclipSyncSender = sender == null ? (player, enabled) -> { } : sender;
    }

    /** Returns whether debouncing is true. */
    public static boolean isDebouncing(Player player) {
        return DEBOUNCING_PLAYERS.contains(player.getUUID());
    }

    /** Sets debouncing. */
    public static void setDebouncing(Player player, boolean value) {
        if (value) DEBOUNCING_PLAYERS.add(player.getUUID());
        else DEBOUNCING_PLAYERS.remove(player.getUUID());
    }

    /** Sets last known flying. */
    public static void setLastKnownFlying(Player player, boolean flying) {
        player.getPersistentData().putBoolean(NBT_LAST_FLYING, flying);
    }

    /** Provides the last known flying operation exposed by this API. */
    public static boolean lastKnownFlying(Player player) {
        return player.getPersistentData().getBoolean(NBT_LAST_FLYING);
    }

    /** Provides the noclip enabled operation exposed by this API. */
    public static boolean noclipEnabled(Player player) {
        return player.getPersistentData().getBoolean(NBT_NOCLIP);
    }

    /** Provides the copy persistent state operation exposed by this API. */
    public static void copyPersistentState(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        KineticFlightSources.copySources(oldPlayer, newPlayer);
        if (oldPlayer.getAbilities().flying) {
            newPlayer.getAbilities().flying = true;
        }
        boolean noclip = noclipEnabled(oldPlayer);
        newPlayer.getPersistentData().putBoolean(NBT_NOCLIP, noclip);
        newPlayer.noPhysics = noclip;
        newPlayer.refreshDimensions();
    }

    /** Provides the server noclip enabled operation exposed by this API. */
    public static boolean serverNoclipEnabled(Player player) {
        return noclipEnabled(player);
    }

    /** Applies server noclip. */
    public static void applyServerNoclip(ServerPlayer player, boolean requestedState) {
        boolean enabled = requestedState && player.isCreative();
        player.getPersistentData().putBoolean(NBT_NOCLIP, enabled);
        player.noPhysics = enabled;
        player.refreshDimensions();
        noclipSyncSender.accept(player, enabled);
    }

    /** Provides the sync server noclip operation exposed by this API. */
    public static void syncServerNoclip(ServerPlayer player) {
        applyServerNoclip(player, serverNoclipEnabled(player));
    }
}
