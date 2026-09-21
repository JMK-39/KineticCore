package dev.xyat.kineticcore.api.flight;

import dev.xyat.kineticcore.internal.flight.KineticSuperFlightRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

/** Public server/common API for Kinetic's attribute-driven super-flight mode. */
public final class KineticSuperFlight {
    private KineticSuperFlight() {
    }

    /** Returns whether the entity's final flight-speed attribute grants super-flight capability. */
    public static boolean available(LivingEntity entity) {
        return KineticSuperFlightRuntime.available(entity);
    }

    /** Returns whether this player currently has authoritative super-flight mode enabled. */
    public static boolean active(Player player) {
        return KineticSuperFlightRuntime.active(player);
    }

    /** Returns the raw flight-capability attribute value. Kept for source compatibility with older addons. */
    public static double maxSpeed(LivingEntity entity) {
        return KineticFlightAttributes.flightSpeed(entity);
    }

    /** Returns the entity's configured turn damping in the public 0..1 range. */
    public static double turnDamping(LivingEntity entity) {
        return KineticFlightAttributes.turnDamping(entity);
    }

    /** Enables or disables authoritative super flight after validating the current player state. */
    public static boolean setActive(ServerPlayer player, boolean active) {
        return KineticSuperFlightRuntime.setActive(player, active);
    }

    /** Toggles authoritative super flight and returns the resulting state. */
    public static boolean toggle(ServerPlayer player) {
        return KineticSuperFlightRuntime.toggle(player);
    }

    /** Revalidates and synchronizes the authoritative state to the owning client. */
    public static void sync(ServerPlayer player) {
        KineticSuperFlightRuntime.sync(player);
    }

    /** Performs one server-side validation/fall-distance tick for active super flight. */
    public static void tick(ServerPlayer player) {
        KineticSuperFlightRuntime.tick(player);
    }

    /** Returns whether Kinetic currently owns the player's real fall-flying pose. */
    public static boolean fallFlyingPose(Player player) {
        return KineticSuperFlightRuntime.fallFlyingPose(player);
    }

    /** Enables or disables Kinetic's real fall-flying pose for the active super-flight player. */
    public static boolean setFallFlyingPose(ServerPlayer player, boolean active) {
        return KineticSuperFlightRuntime.setFallFlyingPose(player, active);
    }

    /** Returns whether the dedicated `/kt flight` command modifier is installed. */
    public static boolean commandEnabled(ServerPlayer player) {
        return KineticSuperFlightRuntime.commandEnabled(player);
    }

    /** Adds or removes only the dedicated command-owned flight-capability modifier. */
    public static boolean setCommandEnabled(ServerPlayer player, boolean enabled) {
        return KineticSuperFlightRuntime.setCommandEnabled(player, enabled);
    }

    /** Installs the core network sender used to synchronize authoritative active state. */
    public static void installStateSyncSender(BiConsumer<ServerPlayer, Boolean> sender) {
        KineticSuperFlightRuntime.installStateSyncSender(sender);
    }
}
