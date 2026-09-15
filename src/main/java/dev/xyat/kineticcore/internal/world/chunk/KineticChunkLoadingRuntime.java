package dev.xyat.kineticcore.internal.world.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

public final class KineticChunkLoadingRuntime {
    private KineticChunkLoadingRuntime() {
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
        ForgeChunkManager.forceChunk(level, ownerModId, ownerPos, chunkX, chunkZ, forced, ticking);
    }
}
