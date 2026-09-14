package dev.xyat.kineticcore.api.monitoring;

import dev.xyat.kineticcore.internal.monitoring.ServerPerformanceAccess;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

public final class KineticServerPerformance {
    private KineticServerPerformance() {
    }

    public static Optional<ServerTickTracker> tracker(MinecraftServer server) {
        if (server instanceof ServerPerformanceAccess access) {
            return Optional.of(access.kineticcore$getTickTracker());
        }
        return Optional.empty();
    }

    public static double tps(double mspt) {
        return ServerTickTracker.tps(mspt);
    }
}
