package dev.xyat.kineticcore.feature.setspawn.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;

public class StructureUtils {
    public static List<String> getStructuresAt(ServerLevel level, BlockPos pos) {
        List<String> structureIds = new ArrayList<>();
        if (level == null || pos == null) {
            return structureIds;
        }

        List<StructureStart> structureStarts = level.structureManager().startsForStructure(new ChunkPos(pos), structure -> true);

        for (StructureStart start : structureStarts) {
            if (!start.isValid() || !start.getBoundingBox().isInside(pos)) {
                continue;
            }

            boolean isInsidePiece = false;
            for (StructurePiece piece : start.getPieces()) {
                if (piece.getBoundingBox().isInside(pos)) {
                    isInsidePiece = true;
                    break;
                }
            }

            if (!isInsidePiece) {
                continue;
            }

            Structure structure = start.getStructure();
            ResourceLocation key = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
            if (key != null) {
                structureIds.add(key.toString());
            }
        }

        return structureIds;
    }

    public static boolean isInStructure(Entity entity, String structureId) {
        if (entity == null || structureId == null || structureId.isBlank()) {
            return false;
        }
        if (entity.level().isClientSide) {
            return false;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        List<String> currentStructures = getStructuresAt(serverLevel, entity.blockPosition());
        for (String currentStructure : currentStructures) {
            if (currentStructure.equals(structureId)) {
                return true;
            }
        }
        return false;
    }

    public static String getDimension(Level level) {
        return level == null ? "unknown" : level.dimension().location().toString();
    }

    public static String getDimension(Entity entity) {
        return entity == null ? "unknown" : getDimension(entity.level());
    }

    public static boolean isDimension(Level level, String dimensionId) {
        return getDimension(level).equals(dimensionId);
    }

    public static boolean isDimension(Entity entity, String dimensionId) {
        return getDimension(entity).equals(dimensionId);
    }
}
