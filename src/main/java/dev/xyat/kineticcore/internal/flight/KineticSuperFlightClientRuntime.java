package dev.xyat.kineticcore.internal.flight;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Client-only steering, speed, maneuver and camera state for attribute-driven super flight. */
public final class KineticSuperFlightClientRuntime {
    private static final double CREATIVE_SPRINT_SPEED = 0.6D;
    private static final double MIN_SELECTED_SPEED_MULTIPLIER = 1.0D;
    private static final double MAX_SELECTED_SPEED_MULTIPLIER = 100.0D;
    private static final double ACCELERATION_SECONDS = 3.0D;
    private static final double ACCELERATION_TICKS = ACCELERATION_SECONDS * 20.0D;
    private static final float ROLL_DEGREES_PER_TICK = 6.0F;
    private static final float FREE_LOOK_RETURN = 0.14F;
    private static final float FOV_RESPONSE = 0.16F;
    private static final float DEFAULT_DAMPING_REFERENCE = 0.7F;
    private static final float DEFAULT_MAX_YAW_STEP = 12.0F;
    private static final float DEFAULT_MAX_PITCH_STEP = 10.0F;

    private static boolean active;
    private static boolean directionInitialized;
    private static boolean moving;
    private static boolean maneuvering;
    private static boolean spaceStopLatch;
    private static boolean rollLeftDown;
    private static boolean rollRightDown;
    private static boolean requestedFallFlyingPose;
    private static double currentSpeed = CREATIVE_SPRINT_SPEED;
    private static double selectedSpeedMultiplier = 20.0D;
    private static double accelerationStartSpeed = CREATIVE_SPRINT_SPEED;
    private static double accelerationTargetSpeed = CREATIVE_SPRINT_SPEED;
    private static double accelerationTicks;
    private static boolean accelerating;
    private static float flightYaw;
    private static float flightPitch;
    private static float visualYaw;
    private static float previousCameraYawOffset;
    private static float previousCameraPitchOffset;
    private static float cameraYawOffset;
    private static float cameraPitchOffset;
    private static float previousRoll;
    private static float roll;
    private static float fovBoost;
    private static BooleanSupplier freeLookDown = () -> false;
    private static Consumer<Boolean> fallFlyingRequestHandler = ignored -> { };
    private static Consumer<Float> rollSyncRequestHandler = ignored -> { };
    private static final Map<UUID, RemoteRollState> REMOTE_ROLLS = new ConcurrentHashMap<>();
    private static int rollSyncTicks;
    private static float lastSyncedRoll = Float.NaN;

    private KineticSuperFlightClientRuntime() {
    }

    public static void installFreeLookState(BooleanSupplier supplier) {
        freeLookDown = supplier == null ? () -> false : supplier;
    }

    public static void installFallFlyingRequestHandler(Consumer<Boolean> handler) {
        fallFlyingRequestHandler = handler == null ? ignored -> { } : handler;
    }

    public static void installRollSyncRequestHandler(Consumer<Float> handler) {
        rollSyncRequestHandler = handler == null ? ignored -> { } : handler;
    }

    public static void applyRemoteRoll(UUID playerId, float value) {
        if (playerId == null || !Float.isFinite(value)) return;
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && playerId.equals(localPlayer.getUUID())) return;
        float wrapped = Mth.wrapDegrees(value);
        if (Math.abs(wrapped) < 0.001F) {
            REMOTE_ROLLS.remove(playerId);
            return;
        }
        REMOTE_ROLLS.compute(playerId, (ignored, state) -> {
            if (state == null) return new RemoteRollState(wrapped);
            state.update(wrapped);
            return state;
        });
    }

    public static float playerRoll(Player player, float partialTick) {
        if (player == null) return 0.0F;
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == player) return roll(partialTick);
        RemoteRollState state = REMOTE_ROLLS.get(player.getUUID());
        return state == null ? 0.0F : state.value(partialTick);
    }

    public static void setSelectedSpeedMultiplier(double multiplier) {
        selectedSpeedMultiplier = Mth.clamp(multiplier, MIN_SELECTED_SPEED_MULTIPLIER, MAX_SELECTED_SPEED_MULTIPLIER);
        currentSpeed = Math.min(currentSpeed, targetSpeed());
        resetAccelerationPhase();
    }

    public static double selectedSpeedMultiplier() {
        return selectedSpeedMultiplier;
    }

    public static void setRollInput(boolean leftDown, boolean rightDown) {
        rollLeftDown = leftDown;
        rollRightDown = rightDown;
    }

    public static void applyServerState(boolean enabled) {
        Player player = Minecraft.getInstance().player;
        active = enabled;
        if (!enabled) {
            if (player != null && maneuvering) {
                player.stopFallFlying();
                player.setDeltaMovement(Vec3.ZERO);
            }
            requestFallFlyingPose(false);
            resetAllState();
        }
    }

    public static boolean active() {
        return active;
    }

    public static boolean freeLookDown() {
        return active && freeLookDown.getAsBoolean();
    }

    public static boolean moving() {
        return active && maneuvering && moving;
    }

    public static boolean maneuvering() {
        return active && maneuvering;
    }

    public static boolean fastPose() {
        return maneuvering();
    }

    public static float renderPitch() {
        return freeLookDown() ? 0.0F : flightPitch;
    }

    public static boolean appliesTo(Player player) {
        return active
                && player != null
                && Minecraft.getInstance().player == player
                && compatible(player);
    }

    public static void tick() {
        Player player = Minecraft.getInstance().player;
        snapshotFrameState();

        if (player == null) {
            active = false;
            requestFallFlyingPose(false);
            resetAllState();
            fovBoost = approach(fovBoost, 0.0F, FOV_RESPONSE);
            return;
        }

        if (!active) {
            fovBoost = approach(fovBoost, 0.0F, FOV_RESPONSE);
            return;
        }

        if (!KineticClientRuntime.jumpKeyDown()) {
            spaceStopLatch = false;
        }

        if (!compatible(player)) {
            if (maneuvering) stopManeuver(player, false);
            fovBoost = approach(fovBoost, 0.0F, FOV_RESPONSE);
            return;
        }

        if (!directionInitialized) {
            initializeDirection(player);
        }

        if (!freeLookDown()) {
            double damping = KineticFlightAttributeRuntime.turnDamping(player);
            updateFlightDirection(player.getYRot(), player.getXRot(), damping);
            returnCameraOffsets();
        }

        moving = hasMovementInput();
        if (moving) {
            Vec3 direction = movementDirection(freeLookDown());
            if (direction.lengthSqr() > 1.0E-7D) {
                visualYaw = yawFromDirection(direction);
            }
        }
        applyVisualYaw(player, visualYaw);

        if (!maneuvering) {
            currentSpeed = CREATIVE_SPRINT_SPEED;
            fovBoost = approach(fovBoost, 0.0F, FOV_RESPONSE);
            return;
        }

        player.startFallFlying();
        player.fallDistance = 0.0F;
        updateRoll();
        syncRoll(false);
        updateManeuverSpeed();

        currentSpeed = Math.max(CREATIVE_SPRINT_SPEED, Math.min(currentSpeed, targetSpeed()));
        float targetFov = fovForSpeed(currentSpeed);
        fovBoost = approach(fovBoost, targetFov, FOV_RESPONSE);
    }

    public static void applyTravel(Player player) {
        if (!active || player == null || Minecraft.getInstance().player != player) return;

        boolean instantBoost = instantBoostRequested();
        if (!maneuvering) {
            if (instantBoost && !spaceStopLatch) {
                startManeuver(player);
                currentSpeed = targetSpeed();
            } else if (KineticClientRuntime.shiftKeyDown() && !spaceStopLatch) {
                startManeuver(player);
            } else {
                applyCruiseTravel(player);
                return;
            }
        }

        if (KineticClientRuntime.jumpKeyDown()) {
            stopManeuver(player, true);
            return;
        }

        if (!compatible(player)) {
            stopManeuver(player, false);
            return;
        }

        player.startFallFlying();
        player.fallDistance = 0.0F;
        moving = hasMovementInput();
        if (instantBoost) {
            currentSpeed = targetSpeed();
            accelerating = false;
            accelerationTicks = ACCELERATION_TICKS;
        }

        if (!moving) {
            player.setDeltaMovement(Vec3.ZERO);
            applyVisualYaw(player, visualYaw);
            return;
        }

        applyDirectionalTravel(player, Math.max(CREATIVE_SPRINT_SPEED, Math.min(currentSpeed, targetSpeed())));
    }


    private static void applyCruiseTravel(Player player) {
        moving = hasMovementInput();
        currentSpeed = CREATIVE_SPRINT_SPEED;
        player.fallDistance = 0.0F;
        if (!moving) {
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        applyDirectionalTravel(player, CREATIVE_SPRINT_SPEED);
    }

    private static void applyDirectionalTravel(Player player, double speed) {
        Vec3 direction = movementDirection(freeLookDown());
        if (direction.lengthSqr() < 1.0E-7D) {
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 motion = direction.scale(speed);
        player.setDeltaMovement(motion);
        player.move(MoverType.SELF, motion);
        player.setDeltaMovement(motion);
        player.fallDistance = 0.0F;

        visualYaw = yawFromDirection(direction);
        applyVisualYaw(player, visualYaw);
    }

    public static void captureFreeLookDelta(Player player, float yawDelta, float pitchDelta) {
        if (!active || player == null || Minecraft.getInstance().player != player || !freeLookDown()) return;
        cameraYawOffset = Mth.wrapDegrees(cameraYawOffset + yawDelta);
        float minOffset = -89.9F - player.getXRot();
        float maxOffset = 89.9F - player.getXRot();
        cameraPitchOffset = Mth.clamp(cameraPitchOffset + pitchDelta, minOffset, maxOffset);
        previousCameraYawOffset = cameraYawOffset;
        previousCameraPitchOffset = cameraPitchOffset;
    }

    public static float cameraYawOffset() {
        return active ? cameraYawOffset : 0.0F;
    }

    public static float cameraYawOffset(float partialTick) {
        if (!active) return 0.0F;
        if (freeLookDown()) return cameraYawOffset;
        float t = smoothPartial(partialTick);
        float delta = Mth.wrapDegrees(cameraYawOffset - previousCameraYawOffset);
        return Mth.wrapDegrees(previousCameraYawOffset + delta * t);
    }

    public static float cameraPitchOffset() {
        return active ? cameraPitchOffset : 0.0F;
    }

    public static float cameraPitchOffset(float partialTick) {
        if (!active) return 0.0F;
        if (freeLookDown()) return cameraPitchOffset;
        return Mth.lerp(smoothPartial(partialTick), previousCameraPitchOffset, cameraPitchOffset);
    }

    public static float roll(float partialTick) {
        if (!maneuvering()) return 0.0F;
        float delta = Mth.wrapDegrees(roll - previousRoll);
        return Mth.wrapDegrees(previousRoll + delta * smoothPartial(partialTick));
    }

    public static float fovBoost() {
        return fovBoost;
    }

    public static double currentSpeed() {
        return currentSpeed;
    }

    public static float flightYaw() {
        return flightYaw;
    }

    public static float flightPitch() {
        return flightPitch;
    }

    private static void startManeuver(Player player) {
        maneuvering = true;
        moving = false;
        currentSpeed = CREATIVE_SPRINT_SPEED;
        resetAccelerationPhase();
        roll = 0.0F;
        previousRoll = 0.0F;
        rollSyncTicks = 0;
        lastSyncedRoll = Float.NaN;
        syncRoll(true);
        initializeDirection(player);
        visualYaw = player.getYRot();
        player.setDeltaMovement(Vec3.ZERO);
        player.startFallFlying();
        player.fallDistance = 0.0F;
        requestFallFlyingPose(true);
    }

    private static void stopManeuver(Player player, boolean stoppedBySpace) {
        maneuvering = false;
        moving = false;
        currentSpeed = CREATIVE_SPRINT_SPEED;
        resetAccelerationPhase();
        directionInitialized = false;
        rollLeftDown = false;
        rollRightDown = false;
        roll = 0.0F;
        previousRoll = 0.0F;
        syncRoll(true);
        rollSyncTicks = 0;
        lastSyncedRoll = Float.NaN;
        cameraYawOffset = 0.0F;
        previousCameraYawOffset = 0.0F;
        cameraPitchOffset = 0.0F;
        previousCameraPitchOffset = 0.0F;
        fovBoost = 0.0F;
        if (stoppedBySpace) spaceStopLatch = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.stopFallFlying();
        player.fallDistance = 0.0F;
        player.setYBodyRot(player.getYRot());
        player.setYHeadRot(player.getYRot());
        requestFallFlyingPose(false);
    }

    private static void initializeDirection(Player player) {
        flightYaw = player.getYRot();
        flightPitch = player.getXRot();
        visualYaw = player.getYRot();
        directionInitialized = true;
    }

    private static boolean compatible(Player player) {
        return KineticFlightAttributeRuntime.flightSpeed(player) > 0.0D
                && !player.isPassenger()
                && !player.isSleeping()
                && !player.isSwimming()
                && !player.isSpectator();
    }

    private static void updateRoll() {
        boolean left = rollLeftDown || KineticClientRuntime.leftKeyDown();
        boolean right = rollRightDown || KineticClientRuntime.rightKeyDown();
        if (left == right) return;
        float step = left ? -ROLL_DEGREES_PER_TICK : ROLL_DEGREES_PER_TICK;
        roll = Mth.wrapDegrees(roll + step);
    }

    private static void syncRoll(boolean force) {
        if (!active) return;
        rollSyncTicks++;
        float wrapped = Mth.wrapDegrees(roll);
        boolean changed = !Float.isFinite(lastSyncedRoll)
                || Math.abs(Mth.wrapDegrees(wrapped - lastSyncedRoll)) >= 0.01F;
        if (!force && !changed && rollSyncTicks < 10) return;
        rollSyncRequestHandler.accept(wrapped);
        lastSyncedRoll = wrapped;
        rollSyncTicks = 0;
    }

    private static boolean hasMovementInput() {
        return KineticClientRuntime.forwardKeyDown() != KineticClientRuntime.backKeyDown();
    }

    private static boolean instantBoostRequested() {
        return KineticClientRuntime.controlModifierDown()
                && KineticClientRuntime.forwardKeyDown()
                && !KineticClientRuntime.backKeyDown();
    }

    private static Vec3 movementDirection(boolean freeLook) {
        double forwardInput = (KineticClientRuntime.forwardKeyDown() ? 1.0D : 0.0D)
                - (KineticClientRuntime.backKeyDown() ? 1.0D : 0.0D);
        if (Math.abs(forwardInput) < 1.0E-7D) return Vec3.ZERO;

        float pitch = freeLook ? 0.0F : flightPitch;
        Vec3 direction = Vec3.directionFromRotation(pitch, flightYaw).scale(forwardInput);
        return direction.lengthSqr() < 1.0E-7D ? Vec3.ZERO : direction.normalize();
    }

    private static void updateManeuverSpeed() {
        if (instantBoostRequested()) {
            currentSpeed = targetSpeed();
            accelerating = false;
            accelerationTicks = ACCELERATION_TICKS;
            return;
        }

        boolean accelerateNow = moving && KineticClientRuntime.shiftKeyDown();
        double target = targetSpeed();
        if (!accelerateNow || currentSpeed >= target) {
            accelerating = false;
            return;
        }

        if (!accelerating || Math.abs(accelerationTargetSpeed - target) > 1.0E-7D) {
            accelerating = true;
            accelerationStartSpeed = currentSpeed;
            accelerationTargetSpeed = target;
            accelerationTicks = 0.0D;
        }

        accelerationTicks = Math.min(ACCELERATION_TICKS, accelerationTicks + 1.0D);
        double progress = accelerationTicks / ACCELERATION_TICKS;
        double eased = progress * progress * (3.0D - 2.0D * progress);
        currentSpeed = accelerationStartSpeed + (accelerationTargetSpeed - accelerationStartSpeed) * eased;
        if (accelerationTicks >= ACCELERATION_TICKS) {
            currentSpeed = accelerationTargetSpeed;
            accelerating = false;
        }
    }

    private static void resetAccelerationPhase() {
        accelerating = false;
        accelerationTicks = 0.0D;
        accelerationStartSpeed = currentSpeed;
        accelerationTargetSpeed = targetSpeed();
    }

    private static void applyVisualYaw(Player player, float yaw) {
        player.setYBodyRot(yaw);
        player.setYHeadRot(yaw);
    }

    private static float yawFromDirection(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private static void snapshotFrameState() {
        previousCameraYawOffset = cameraYawOffset;
        previousCameraPitchOffset = cameraPitchOffset;
        previousRoll = roll;
    }

    private static void returnCameraOffsets() {
        float yawDelta = Mth.wrapDegrees(-cameraYawOffset);
        cameraYawOffset = Mth.wrapDegrees(cameraYawOffset + yawDelta * FREE_LOOK_RETURN);
        cameraPitchOffset = approach(cameraPitchOffset, 0.0F, FREE_LOOK_RETURN);
    }

    private static void resetAllState() {
        moving = false;
        maneuvering = false;
        spaceStopLatch = false;
        directionInitialized = false;
        rollLeftDown = false;
        rollRightDown = false;
        currentSpeed = CREATIVE_SPRINT_SPEED;
        resetAccelerationPhase();
        previousCameraYawOffset = 0.0F;
        previousCameraPitchOffset = 0.0F;
        cameraYawOffset = 0.0F;
        cameraPitchOffset = 0.0F;
        previousRoll = 0.0F;
        roll = 0.0F;
        rollSyncTicks = 0;
        lastSyncedRoll = Float.NaN;
        fovBoost = 0.0F;
    }

    private static void requestFallFlyingPose(boolean enabled) {
        if (requestedFallFlyingPose == enabled) return;
        requestedFallFlyingPose = enabled;
        fallFlyingRequestHandler.accept(enabled);
    }

    private static double targetSpeed() {
        return CREATIVE_SPRINT_SPEED * Mth.clamp(
                selectedSpeedMultiplier,
                MIN_SELECTED_SPEED_MULTIPLIER,
                MAX_SELECTED_SPEED_MULTIPLIER
        );
    }

    private static float fovForSpeed(double speed) {
        double multiplier = speed / CREATIVE_SPRINT_SPEED;
        return (float) (Mth.clamp((multiplier - 1.0D) / 24.0D, 0.0D, 1.0D) * 34.0D);
    }

    private static void updateFlightDirection(float targetYaw, float targetPitch, double damping) {
        if (damping <= 0.0D) {
            flightYaw = targetYaw;
            flightPitch = targetPitch;
            return;
        }
        if (damping >= 1.0D) return;

        float response = (float) (1.0D - damping);
        float scale = response / DEFAULT_DAMPING_REFERENCE;
        float maxYawStep = DEFAULT_MAX_YAW_STEP * scale;
        float maxPitchStep = DEFAULT_MAX_PITCH_STEP * scale;
        flightYaw = approachAngle(flightYaw, targetYaw, response, maxYawStep);
        flightPitch = approachAngle(flightPitch, targetPitch, response, maxPitchStep);
    }

    private static float approachAngle(float current, float target, float response, float maxStep) {
        float delta = Mth.wrapDegrees(target - current);
        float step = Mth.clamp(delta * response, -maxStep, maxStep);
        return current + step;
    }

    private static double approach(double current, double target, double step) {
        if (step <= 0.0D) return target;
        if (current < target) return Math.min(target, current + step);
        if (current > target) return Math.max(target, current - step);
        return current;
    }

    private static float approach(float current, float target, float response) {
        float next = current + (target - current) * response;
        return Math.abs(target - next) < 0.01F ? target : next;
    }

    private static float smoothPartial(float partialTick) {
        float t = Mth.clamp(partialTick, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
    private static final class RemoteRollState {
        private float previous;
        private float current;

        private RemoteRollState(float value) {
            this.previous = value;
            this.current = value;
        }

        private void update(float value) {
            this.previous = this.current;
            this.current = value;
        }

        private float value(float partialTick) {
            float delta = Mth.wrapDegrees(this.current - this.previous);
            return Mth.wrapDegrees(this.previous + delta * smoothPartial(partialTick));
        }
    }

}
