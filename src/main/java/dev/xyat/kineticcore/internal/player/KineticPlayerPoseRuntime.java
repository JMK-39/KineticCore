package dev.xyat.kineticcore.internal.player;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

import java.util.Objects;

/** Internal implementation for applying a vanilla pose through the normal dimension refresh path. */
public final class KineticPlayerPoseRuntime {
    private KineticPlayerPoseRuntime() {
    }

    public static void apply(LivingEntity entity, Pose pose) {
        if (entity == null) return;
        Pose target = Objects.requireNonNull(pose, "pose");
        if (entity.getPose() != target) entity.setPose(target);
        entity.refreshDimensions();
    }
}
