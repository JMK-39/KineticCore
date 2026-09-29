package dev.xyat.kineticcore.api.monitoring;

import net.minecraft.server.MinecraftServer;

import java.util.Optional;

/** Access to the tick-time tracker that KineticCore attaches to every running server. */
public final class KineticServerPerformance {
    private KineticServerPerformance() {
    }

    /**
     * Implemented on {@code MinecraftServer} by a KineticCore mixin. Add-ons use {@link #tracker(MinecraftServer)}
     * instead.
     */
    public interface Access {
        /** Returns the tracker attached to this server instance. */
        ServerTickTracker kineticcore$getTickTracker();
    }

    /**
     * Returns the tick tracker of a server.
     *
     * @param server running server
     * @return the tracker, or empty when the tracking mixin is not applied (for example disabled by a feature
     *   switch)
     */
    public static Optional<ServerTickTracker> tracker(MinecraftServer server) {
        if (server instanceof Access access) {
            return Optional.ofNullable(access.kineticcore$getTickTracker());
        }
        return Optional.empty();
    }

    /**
     * Converts milliseconds per tick to ticks per second, capped at 20. See {@link ServerTickTracker#tps(double)}.
     */
    public static double tps(double mspt) {
        return ServerTickTracker.tps(mspt);
    }
}
