package dev.xyat.kineticcore.internal.flight;

import dev.xyat.kineticcore.internal.player.KineticCrawlingRuntime;
import dev.xyat.kineticcore.internal.player.KineticPlayerPoseRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Authoritative server state for Kinetic super flight. */
public final class KineticSuperFlightRuntime {
    private static final String NBT_ACTIVE = "kinetic_super_flight_active";
    private static final String NBT_FALL_FLYING_POSE = "kinetic_super_flight_fall_flying_pose";
    private static final UUID COMMAND_SPEED_UUID = UUID.fromString("6bb2f49e-4f79-4a4b-9bb0-2f3305a9e081");
    private static final String COMMAND_SPEED_NAME = "Kinetic command super flight";
    private static final double COMMAND_SPEED_VALUE = 20.0D;
    private static volatile BiConsumer<ServerPlayer, Boolean> stateSyncSender = (player, active) -> { };
    private static volatile BiConsumer<ServerPlayer, Float> rollSyncSender = (player, roll) -> { };
    private static volatile Consumer<ServerPlayer> transientResetSender = player -> { };

    private KineticSuperFlightRuntime() {
    }

    public static void installStateSyncSender(BiConsumer<ServerPlayer, Boolean> sender) {
        stateSyncSender = sender == null ? (player, active) -> { } : sender;
    }

    public static void installRollSyncSender(BiConsumer<ServerPlayer, Float> sender) {
        rollSyncSender = sender == null ? (player, roll) -> { } : sender;
    }

    public static void installTransientResetSender(Consumer<ServerPlayer> sender) {
        transientResetSender = sender == null ? player -> { } : sender;
    }

    public static boolean available(LivingEntity entity) {
        return KineticFlightAttributeRuntime.flightSpeed(entity) > 0.0D;
    }

    public static boolean active(Player player) {
        return player != null && player.getPersistentData().getBoolean(NBT_ACTIVE);
    }

    public static boolean fallFlyingPose(Player player) {
        return player != null
                && active(player)
                && player.getPersistentData().getBoolean(NBT_FALL_FLYING_POSE);
    }

    public static boolean setActive(ServerPlayer player, boolean requested) {
        if (player == null) return false;
        if (requested && available(player)) KineticCrawlingRuntime.clearCrawling(player);
        boolean actual = requested && available(player) && compatible(player);
        boolean ownedFallFlying = fallFlyingPose(player);
        player.getPersistentData().putBoolean(NBT_ACTIVE, actual);
        if (!actual) {
            player.getPersistentData().putBoolean(NBT_FALL_FLYING_POSE, false);
            if (ownedFallFlying) player.stopFallFlying();
            if (ownedFallFlying && player.getPose() == Pose.SWIMMING) player.setPose(Pose.STANDING);
            player.refreshDimensions();
            rollSyncSender.accept(player, 0.0F);
        } else {
            player.setShiftKeyDown(false);
            player.fallDistance = 0.0F;
        }
        stateSyncSender.accept(player, actual);
        return actual;
    }

    public static boolean toggle(ServerPlayer player) {
        return setActive(player, !active(player));
    }

    public static void resetTransientState(ServerPlayer player) {
        if (player == null) return;
        boolean ownedFallFlying = player.getPersistentData().getBoolean(NBT_FALL_FLYING_POSE);
        player.getPersistentData().putBoolean(NBT_FALL_FLYING_POSE, false);
        if (ownedFallFlying) player.stopFallFlying();
        if (ownedFallFlying && player.getPose() == Pose.SWIMMING) player.setPose(Pose.STANDING);
        player.refreshDimensions();
        player.fallDistance = 0.0F;
        rollSyncSender.accept(player, 0.0F);
        transientResetSender.accept(player);
    }

    public static void sync(ServerPlayer player) {
        if (player == null) return;
        boolean actual = active(player) && available(player) && compatible(player);
        if (actual != active(player)) {
            setActive(player, actual);
            return;
        }
        if (fallFlyingPose(player)) {
            player.startFallFlying();
            player.fallDistance = 0.0F;
        }
        stateSyncSender.accept(player, actual);
        if (!actual) rollSyncSender.accept(player, 0.0F);
    }

    public static void tick(ServerPlayer player) {
        if (player == null || !active(player)) return;
        if (!available(player) || !compatible(player)) {
            setActive(player, false);
            return;
        }
        if (player.isShiftKeyDown()) player.setShiftKeyDown(false);
        player.fallDistance = 0.0F;
        if (fallFlyingPose(player)) {
            player.startFallFlying();
        }
    }

    public static boolean setFallFlyingPose(ServerPlayer player, boolean requested) {
        if (player == null) return false;
        if (requested && active(player)) KineticCrawlingRuntime.clearCrawling(player);
        boolean actual = requested && active(player) && available(player) && baseCompatible(player);
        boolean previous = player.getPersistentData().getBoolean(NBT_FALL_FLYING_POSE);
        player.getPersistentData().putBoolean(NBT_FALL_FLYING_POSE, actual);
        if (actual) {
            player.startFallFlying();
            KineticPlayerPoseRuntime.apply(player, Pose.SWIMMING);
            player.fallDistance = 0.0F;
        } else if (previous) {
            player.stopFallFlying();
            if (player.getPose() == Pose.SWIMMING) player.setPose(Pose.STANDING);
            player.refreshDimensions();
            player.fallDistance = 0.0F;
        }
        return actual;
    }

    public static boolean commandEnabled(ServerPlayer player) {
        AttributeInstance instance = player == null ? null : player.getAttribute(KineticFlightAttributeRuntime.flightSpeed());
        return instance != null && instance.getModifier(COMMAND_SPEED_UUID) != null;
    }

    public static boolean setCommandEnabled(ServerPlayer player, boolean enabled) {
        if (player == null) return false;
        AttributeInstance instance = player.getAttribute(KineticFlightAttributeRuntime.flightSpeed());
        if (instance == null) return false;

        AttributeModifier existing = instance.getModifier(COMMAND_SPEED_UUID);
        if (enabled) {
            if (existing == null || Double.compare(existing.getAmount(), COMMAND_SPEED_VALUE) != 0) {
                if (existing != null) instance.removeModifier(COMMAND_SPEED_UUID);
                instance.addPermanentModifier(new AttributeModifier(
                        COMMAND_SPEED_UUID,
                        COMMAND_SPEED_NAME,
                        COMMAND_SPEED_VALUE,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        } else if (existing != null) {
            instance.removeModifier(COMMAND_SPEED_UUID);
            if (!available(player)) setActive(player, false);
        }
        return commandEnabled(player);
    }

    private static boolean compatible(ServerPlayer player) {
        return baseCompatible(player)
                && (!player.isFallFlying() || player.getPersistentData().getBoolean(NBT_FALL_FLYING_POSE));
    }

    private static boolean baseCompatible(ServerPlayer player) {
        return player.isAlive()
                && !player.isPassenger()
                && !player.isSleeping()
                && !player.isSwimming()
                && !player.isSpectator();
    }
}
