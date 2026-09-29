package dev.xyat.kineticcore.api.flight;

import dev.xyat.kineticcore.internal.flight.KineticFlightClientRuntime;
import dev.xyat.kineticcore.internal.flight.KineticSuperFlightClientRuntime;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Client-side flight state: flight speed, inertia, noclip and the super-flight controller.
 *
 * <p>The {@code install...} methods connect key bindings and network senders; KineticCore's flight feature calls
 * them during client setup. The remaining methods expose state for rendering and for add-ons. Call everything on
 * the client thread.
 */
public final class KineticFlightClient {
    private static volatile boolean noclipEnabled;
    private static volatile float flightSpeedMultiplier = 1.0F;
    private static volatile boolean inertiaEnabled;
    private static volatile BooleanSupplier speedModifierDown = () -> false;
    private static volatile BooleanSupplier superFlightFreeLookDown = () -> false;
    private static volatile Consumer<Boolean> noclipRequestHandler = KineticFlightClient::applyLocalNoclip;

    private KineticFlightClient() {
    }

    /**
     * Installs the source that reports whether the flight speed-modifier key is held; {@code null} means never
     * held.
     */
    public static void installSpeedModifierState(BooleanSupplier supplier) {
        speedModifierDown = supplier == null ? () -> false : supplier;
    }

    /** Installs the held-state source for the configurable super-flight free-look key. */
    public static void installSuperFlightFreeLookState(BooleanSupplier supplier) {
        superFlightFreeLookDown = supplier == null ? () -> false : supplier;
        KineticSuperFlightClientRuntime.installFreeLookState(superFlightFreeLookDown);
    }

    /** Installs the client-to-server sender used to synchronize the real fall-flying pose. */
    public static void installSuperFlightFallFlyingRequestHandler(Consumer<Boolean> handler) {
        KineticSuperFlightClientRuntime.installFallFlyingRequestHandler(handler);
    }

    /** Installs the client-to-server sender used to synchronize the physical super-flight roll. */
    public static void installSuperFlightRollRequestHandler(Consumer<Float> handler) {
        KineticSuperFlightClientRuntime.installRollSyncRequestHandler(handler);
    }

    /** Applies one server-broadcast roll sample for another rendered player. */
    public static void applySuperFlightRemoteRoll(UUID playerId, float roll) {
        KineticSuperFlightClientRuntime.applyRemoteRoll(playerId, roll);
    }

    /**
     * Installs the sender used by {@link #requestNoclip(boolean)} to ask the server for a noclip change.
     *
     * @param handler sender; {@code null} restores local-only behavior that applies the state immediately
     */
    public static void installNoclipRequestHandler(Consumer<Boolean> handler) {
        noclipRequestHandler = handler == null ? KineticFlightClient::applyLocalNoclip : handler;
    }

    /** Returns whether the flight speed-modifier key is currently held. */
    public static boolean isSpeedModifierDown() {
        return speedModifierDown.getAsBoolean();
    }

    /** Returns the local player's noclip state as last applied on this client. */
    public static boolean noclipEnabled() {
        return noclipEnabled;
    }

    /**
     * Asks for a noclip change through the installed request handler; the server confirms it with
     * {@link #applyServerNoclip(boolean)}.
     */
    public static void requestNoclip(boolean enabled) {
        noclipRequestHandler.accept(enabled);
    }

    /** Applies the noclip state confirmed by the server to the local player. */
    public static void applyServerNoclip(boolean enabled) {
        applyLocalNoclip(enabled);
    }

    /** Applies a noclip state to the local player immediately, without asking the server. */
    public static void applyLocalNoclip(boolean enabled) {
        noclipEnabled = enabled;
        KineticFlightClientRuntime.applyLocalNoclip(enabled);
    }

    /** Returns the creative flight speed multiplier; {@code 1.0} is vanilla speed. */
    public static float flightSpeedMultiplier() {
        return flightSpeedMultiplier;
    }

    /** Sets the creative flight speed multiplier; {@code 1.0} is vanilla speed. Takes effect on the next tick. */
    public static void setFlightSpeedMultiplier(float multiplier) {
        flightSpeedMultiplier = multiplier;
    }

    /** Applies the authoritative server state for attribute-driven super flight. */
    public static void applySuperFlightState(boolean active) {
        KineticSuperFlightClientRuntime.applyServerState(active);
    }

    /** Clears only local transient maneuver state after a lifecycle boundary while preserving the active flag. */
    public static void resetSuperFlightTransientState() {
        KineticSuperFlightClientRuntime.resetTransientStateFromServer();
    }

    /** Returns whether attribute-driven super flight is currently active on the local player. */
    public static boolean superFlightActive() {
        return KineticSuperFlightClientRuntime.active();
    }

    /** Returns whether the configurable super-flight free-look key is currently held. */
    public static boolean superFlightFreeLookDown() {
        return KineticSuperFlightClientRuntime.freeLookDown();
    }

    /** Returns whether the local player is currently being propelled by super flight. */
    public static boolean superFlightMoving() {
        return KineticSuperFlightClientRuntime.moving();
    }

    /** Returns whether the local player is in the persistent super-flight maneuver state. */
    public static boolean superFlightManeuvering() {
        return KineticSuperFlightClientRuntime.maneuvering();
    }

    /** Updates the held left/right mouse roll controls used by super flight. */
    public static void setSuperFlightRollInput(boolean leftDown, boolean rightDown) {
        KineticSuperFlightClientRuntime.setRollInput(leftDown, rightDown);
    }

    /** Returns the remembered target super-flight speed multiplier. */
    public static double superFlightSelectedSpeedMultiplier() {
        return KineticSuperFlightClientRuntime.selectedSpeedMultiplier();
    }

    /** Updates the remembered target super-flight speed multiplier used for acceleration. */
    public static void setSuperFlightSelectedSpeedMultiplier(double multiplier) {
        KineticSuperFlightClientRuntime.setSelectedSpeedMultiplier(multiplier);
    }

    /** Returns whether the local player is in the persistent real fall-flying maneuver state. */
    public static boolean superFlightFastPose() {
        return KineticSuperFlightClientRuntime.fastPose();
    }

    /** Runs one client update for super-flight cruise, acceleration, steering, roll, free-look and FOV. */
    public static void tickSuperFlight() {
        KineticSuperFlightClientRuntime.tick();
    }

    /** Returns whether super-flight physics should replace travel for the supplied player. */
    public static boolean appliesSuperFlightTo(net.minecraft.world.entity.player.Player player) {
        return KineticSuperFlightClientRuntime.appliesTo(player);
    }

    /** Applies the client-side collision-aware movement step for super flight. */
    public static void applySuperFlightTravel(net.minecraft.world.entity.player.Player player) {
        KineticSuperFlightClientRuntime.applyTravel(player);
    }

    /** Captures already sensitivity-scaled mouse rotation into the free-look camera offset. */
    public static void captureSuperFlightFreeLookDelta(net.minecraft.world.entity.player.Player player, float yawDelta, float pitchDelta) {
        KineticSuperFlightClientRuntime.captureFreeLookDelta(player, yawDelta, pitchDelta);
    }

    /** Returns current free-look/return camera yaw offset. */
    public static float superFlightCameraYawOffset() {
        return KineticSuperFlightClientRuntime.cameraYawOffset();
    }

    /** Returns the frame-interpolated free-look/return camera yaw offset. */
    public static float superFlightCameraYawOffset(float partialTick) {
        return KineticSuperFlightClientRuntime.cameraYawOffset(partialTick);
    }

    /** Returns current free-look/return camera pitch offset. */
    public static float superFlightCameraPitchOffset() {
        return KineticSuperFlightClientRuntime.cameraPitchOffset();
    }

    /** Returns the frame-interpolated free-look/return camera pitch offset. */
    public static float superFlightCameraPitchOffset(float partialTick) {
        return KineticSuperFlightClientRuntime.cameraPitchOffset(partialTick);
    }

    /** Returns the frame-interpolated persistent camera roll angle. */
    public static float superFlightRoll(float partialTick) {
        return KineticSuperFlightClientRuntime.roll(partialTick);
    }

    /** Returns the interpolated physical roll used to render the supplied player. */
    public static float superFlightPlayerRoll(net.minecraft.world.entity.player.Player player, float partialTick) {
        return KineticSuperFlightClientRuntime.playerRoll(player, partialTick);
    }

    /** Returns the current actual-motion-dependent FOV boost. */
    public static float superFlightFovBoost() {
        return KineticSuperFlightClientRuntime.fovBoost();
    }

    /** Returns the frame-interpolated actual-motion-dependent FOV boost. */
    public static float superFlightFovBoost(float partialTick) {
        return KineticSuperFlightClientRuntime.fovBoost(partialTick);
    }

    /** Returns current eased super-flight speed. */
    public static double superFlightCurrentSpeed() {
        return KineticSuperFlightClientRuntime.currentSpeed();
    }

    /**
     * Returns whether creative flight keeps vanilla inertia; when {@code false} the player stops as soon as
     * movement keys are released.
     */
    public static boolean inertiaEnabled() {
        return inertiaEnabled;
    }

    /** Sets whether creative flight keeps vanilla inertia. */
    public static void setInertiaEnabled(boolean enabled) {
        inertiaEnabled = enabled;
    }
}
