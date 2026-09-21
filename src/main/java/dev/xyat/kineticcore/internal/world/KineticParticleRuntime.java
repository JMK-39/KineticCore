package dev.xyat.kineticcore.internal.world;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class KineticParticleRuntime {
    private KineticParticleRuntime() {
    }

    public static <T extends ParticleOptions> void sendWithinDistance(
            ServerLevel level,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed,
            double maxDistance
    ) {
        double distance = Math.max(0.0D, maxDistance);
        double distanceSqr = distance * distance;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(x, y, z) <= distanceSqr) {
                level.sendParticles(viewer, particle, true, x, y, z, count, offsetX, offsetY, offsetZ, speed);
            }
        }
    }

    public static <T extends ParticleOptions> void sendDirectionalWithinDistance(
            ServerLevel level,
            T particle,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            double maxDistance
    ) {
        double distance = Math.max(0.0D, maxDistance);
        double distanceSqr = distance * distance;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(x, y, z) <= distanceSqr) {
                level.sendParticles(viewer, particle, true, x, y, z, 0, velocityX, velocityY, velocityZ, 1.0D);
            }
        }
    }
}
