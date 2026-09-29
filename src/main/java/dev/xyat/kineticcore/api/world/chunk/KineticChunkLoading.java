package dev.xyat.kineticcore.api.world.chunk;

import dev.xyat.kineticcore.internal.world.chunk.KineticChunkLoadingRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

/** Forced chunk loading through Forge's ticket system. Call on the server thread. */
public final class KineticChunkLoading {
    private KineticChunkLoading() {
    }

    /**
     * Adds or removes a forced-chunk ticket owned by a block, for example a chunk loader. Tickets are saved with
     * the level and restored after a restart.
     *
     * @param level level containing the chunk
     * @param ownerModId mod id that owns the ticket
     * @param ownerPos position of the owning block; used to find and release the ticket later
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param forced {@code true} to add the ticket, {@code false} to remove it
     * @param ticking whether entities and blocks in the chunk keep ticking, not just stay loaded
     * @throws NullPointerException if {@code level}, {@code ownerModId} or {@code ownerPos} is {@code null}
     */
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
