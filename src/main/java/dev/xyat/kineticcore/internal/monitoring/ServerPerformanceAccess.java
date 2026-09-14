package dev.xyat.kineticcore.internal.monitoring;

import dev.xyat.kineticcore.api.monitoring.ServerTickTracker;

public interface ServerPerformanceAccess {
    ServerTickTracker kineticcore$getTickTracker();
}
