package dev.xyat.kineticcore.api.player;

import dev.xyat.kineticcore.internal.player.KineticPlayerPoseRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/** Public common API for applying a vanilla entity pose and refreshing its real dimensions. */
public final class KineticPlayerPose {
    private KineticPlayerPose() {
    }

    /** Applies the requested player pose through the shared pose API. */
    public static void apply(LivingEntity entity, Pose pose) {
        KineticPlayerPoseRuntime.apply(entity, pose);
    }
}
