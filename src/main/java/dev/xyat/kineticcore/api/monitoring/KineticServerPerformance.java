package dev.xyat.kineticcore.api.monitoring;

import net.minecraft.server.MinecraftServer;

import java.util.Optional;

/** Public Kinetic API facade for server performance. */
public final class KineticServerPerformance {
    private KineticServerPerformance() {
    }

    /** Internal mixin access contract used to expose the tracker attached to a server instance. */
    public interface Access {
        /** Returns the tracker attached to this server instance. */
        ServerTickTracker kineticcore$getTickTracker();
    }

    /**
     * Performs the tracker API operation.
     */
    public static Optional<ServerTickTracker> tracker(MinecraftServer server) {
        if (server instanceof Access access) {
            return Optional.ofNullable(access.kineticcore$getTickTracker());
        }
        return Optional.empty();
    }

    /**
     * Performs the tps API operation.
     */
    public static double tps(double mspt) {
        return ServerTickTracker.tps(mspt);
    }
}
