package dev.xyat.kineticcore.api.flight;

import dev.xyat.kineticcore.internal.flight.KineticFlightClientRuntime;
import dev.xyat.kineticcore.internal.flight.KineticSuperFlightClientRuntime;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Public Kinetic API facade for flight client. */
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
     * Performs the install speed modifier state API operation.
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
     * Performs the install noclip request handler API operation.
     */
    public static void installNoclipRequestHandler(Consumer<Boolean> handler) {
        noclipRequestHandler = handler == null ? KineticFlightClient::applyLocalNoclip : handler;
    }

    /**
     * Returns whether speed modifier down.
     */
    public static boolean isSpeedModifierDown() {
        return speedModifierDown.getAsBoolean();
    }

    /**
     * Performs the noclip enabled API operation.
     */
    public static boolean noclipEnabled() {
        return noclipEnabled;
    }

    /**
     * Requests noclip.
     */
    public static void requestNoclip(boolean enabled) {
        noclipRequestHandler.accept(enabled);
    }

    /**
     * Applies server noclip.
     */
    public static void applyServerNoclip(boolean enabled) {
        applyLocalNoclip(enabled);
    }

    /**
     * Applies local noclip.
     */
    public static void applyLocalNoclip(boolean enabled) {
        noclipEnabled = enabled;
        KineticFlightClientRuntime.applyLocalNoclip(enabled);
    }

    /**
     * Performs the flight speed multiplier API operation.
     */
    public static float flightSpeedMultiplier() {
        return flightSpeedMultiplier;
    }

    /**
     * Updates flight speed multiplier.
     */
    public static void setFlightSpeedMultiplier(float multiplier) {
        flightSpeedMultiplier = multiplier;
    }

    /** Applies the authoritative server state for attribute-driven super flight. */
    public static void applySuperFlightState(boolean active) {
        KineticSuperFlightClientRuntime.applyServerState(active);
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

    /** Returns the steering pitch retained for source compatibility with older addons. */
    public static float superFlightRenderPitch() {
        return KineticSuperFlightClientRuntime.renderPitch();
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
     * Performs the inertia enabled API operation.
     */
    public static boolean inertiaEnabled() {
        return inertiaEnabled;
    }

    /**
     * Updates inertia enabled.
     */
    public static void setInertiaEnabled(boolean enabled) {
        inertiaEnabled = enabled;
    }
}
