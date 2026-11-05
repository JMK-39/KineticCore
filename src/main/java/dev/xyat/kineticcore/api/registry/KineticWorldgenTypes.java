package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticDeferredRegistryRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Public Kinetic API facade for the world generation types a mod adds: structure types, structure placement types
 * and structure piece types, registered with the loader's deferred registers on the registering mod's event bus.
 * Call during mod construction.
 */
public final class KineticWorldgenTypes {
    private KineticWorldgenTypes() {
    }

    /** Registers a structure type, the {@code type} of {@code worldgen/structure} files. */
    public static <T extends StructureType<?>> KineticRegistryHandle<T> structureType(ResourceLocation id, Supplier<? extends T> factory) {
        return KineticDeferredRegistryRuntime.register(Registries.STRUCTURE_TYPE, Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }

    /** Registers a structure placement type, the {@code placement.type} of {@code worldgen/structure_set} files. */
    public static <T extends StructurePlacementType<?>> KineticRegistryHandle<T> placementType(ResourceLocation id, Supplier<? extends T> factory) {
        return KineticDeferredRegistryRuntime.register(Registries.STRUCTURE_PLACEMENT, Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }

    /** Registers a structure piece type, which saved structure starts name their pieces by. */
    public static <T extends StructurePieceType> KineticRegistryHandle<T> pieceType(ResourceLocation id, Supplier<? extends T> factory) {
        return KineticDeferredRegistryRuntime.register(Registries.STRUCTURE_PIECE, Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }
}
