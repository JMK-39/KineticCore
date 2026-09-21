package dev.xyat.kineticcore.api.world;

import dev.xyat.kineticcore.internal.world.KineticParticleRuntime;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

/**
 * Provides reusable server-side particle delivery with explicit distance limits.
 */
public final class KineticParticles {
    private KineticParticles() {
    }

    /**
     * Sends particles only to players within {@code maxDistance} blocks of the particle origin.
     */
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
        KineticParticleRuntime.sendWithinDistance(
                Objects.requireNonNull(level, "level"),
                Objects.requireNonNull(particle, "particle"),
                x, y, z, count, offsetX, offsetY, offsetZ, speed, maxDistance
        );
    }

    /**
     * Sends one directional particle packet only to players within {@code maxDistance} blocks of the origin.
     */
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
        KineticParticleRuntime.sendDirectionalWithinDistance(
                Objects.requireNonNull(level, "level"),
                Objects.requireNonNull(particle, "particle"),
                x, y, z, velocityX, velocityY, velocityZ, maxDistance
        );
    }
}
