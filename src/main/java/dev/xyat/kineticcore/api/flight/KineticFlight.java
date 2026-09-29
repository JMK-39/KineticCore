package dev.xyat.kineticcore.api.flight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

/**
 * Server-side noclip state stored in the player's persistent data, and copying of flight state when a new player
 * object replaces the old one. Flight permission itself is managed by {@link KineticFlightSources}. Call everything
 * on the server thread.
 */
public final class KineticFlight {
    private static final String NBT_NOCLIP = "kt_noclip";

    private static volatile BiConsumer<ServerPlayer, Boolean> noclipSyncSender = (player, enabled) -> { };

    private KineticFlight() {
    }

    /**
     * Installs the callback that tells a client its noclip state changed. KineticCore's flight feature installs it
     * during setup; add-ons normally do not call this.
     *
     * @param sender receives the player and the new state; {@code null} installs a no-op
     */
    public static void installNoclipSyncSender(BiConsumer<ServerPlayer, Boolean> sender) {
        noclipSyncSender = sender == null ? (player, enabled) -> { } : sender;
    }

    /** Returns the noclip flag stored in the player's persistent data. */
    public static boolean noclipEnabled(Player player) {
        return player.getPersistentData().getBoolean(NBT_NOCLIP);
    }

    /**
     * Copies flight sources, current flight and noclip from the old player instance to the new one when the player
     * respawns or returns from the End.
     */
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

    /**
     * Sets noclip for the player and notifies their client. Noclip is only granted in creative mode; any other
     * request turns it off.
     *
     * @param player target player
     * @param requestedState desired state
     */
    public static void applyServerNoclip(ServerPlayer player, boolean requestedState) {
        boolean enabled = requestedState && player.isCreative();
        player.getPersistentData().putBoolean(NBT_NOCLIP, enabled);
        player.noPhysics = enabled;
        player.refreshDimensions();
        noclipSyncSender.accept(player, enabled);
    }

    /** Re-applies the stored noclip state, for example after a game-mode change, and resends it to the client. */
    public static void syncServerNoclip(ServerPlayer player) {
        applyServerNoclip(player, noclipEnabled(player));
    }
}
