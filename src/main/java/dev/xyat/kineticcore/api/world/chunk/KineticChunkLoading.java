package dev.xyat.kineticcore.api.world.chunk;

import dev.xyat.kineticcore.internal.world.chunk.KineticChunkLoadingRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

public final class KineticChunkLoading {
    private KineticChunkLoading() {
    }

    public static void setForced(
            ServerLevel level,
            String ownerModId,
            BlockPos ownerPos,
            int chunkX,
            int chunkZ,
            boolean forced,
            boolean ticking
    ) {
        KineticChunkLoadingRuntime.setForced(
                Objects.requireNonNull(level, "level"),
                Objects.requireNonNull(ownerModId, "ownerModId"),
                Objects.requireNonNull(ownerPos, "ownerPos"),
                chunkX,
                chunkZ,
                forced,
                ticking
        );
    }
}
