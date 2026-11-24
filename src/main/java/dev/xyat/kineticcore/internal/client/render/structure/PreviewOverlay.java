package dev.xyat.kineticcore.internal.client.render.structure;

import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * What a structure view draws over its snapshot: entities as their models and outlined boxes, all in cells. It does
 * not change once made; a view makes a new one when anything in it changes, so 26.1 can tell from its identity whether
 * the preview must be drawn again.
 */
public final class PreviewOverlay {
    /** Most entities drawn; the rest of a crowded snapshot are left out of the picture. */
    public static final int MAX_FIGURES = 512;

    /**
     * An outlined box; a see-through one also shows, faintly, where blocks are in front of it, and a fine one is drawn
     * a pixel wide (grid lines) instead of as a bold outline.
     */
    public record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb, boolean seeThrough, boolean fine) {
    }

    /** An entity drawn at a position in cells, made from {@code entry}. */
    public record Figure(KineticBlockSnapshot.EntityEntry entry, Entity entity, double x, double y, double z) {
    }

    public static final PreviewOverlay EMPTY = new PreviewOverlay(List.of(), List.of());
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    // Entity types already reported as not drawable, so a crowd of them is reported once.
    private static final java.util.Set<String> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final List<Box> boxes;
    private final List<Figure> figures;

    public PreviewOverlay(List<Box> boxes, List<Figure> figures) {
        this.boxes = List.copyOf(boxes);
        this.figures = List.copyOf(figures);
    }

    public List<Box> boxes() {
        return boxes;
    }

    public List<Figure> figures() {
        return figures;
    }

    public boolean isEmpty() {
        return boxes.isEmpty() && figures.isEmpty();
    }

    /**
     * Entities made from saved data, to be drawn and never added to a level: loaded into the client's level without
     * joining it. One that cannot be made here (a modded entity that needs the server) is left out. The figures of
     * {@code previous} made from the very same entries are kept, so changing one entity does not make all again.
     */
    public static List<Figure> figures(List<KineticBlockSnapshot.EntityEntry> entries, List<Figure> previous) {
        var level = Minecraft.getInstance().level;
        List<Figure> figures = new ArrayList<>();
        if (level == null) return figures;
        java.util.Map<KineticBlockSnapshot.EntityEntry, Figure> made = new java.util.IdentityHashMap<>();
        for (Figure figure : previous) made.put(figure.entry(), figure);
        for (KineticBlockSnapshot.EntityEntry entry : entries) {
            if (figures.size() >= MAX_FIGURES) break;
            Figure old = made.get(entry);
            if (old != null) {
                figures.add(old);
                continue;
            }
            Entity entity = load(entry.tag(), level);
            if (entity != null) figures.add(new Figure(entry, entity, entry.x(), entry.y(), entry.z()));
        }
        return figures;
    }

    @Nullable
    private static Entity load(CompoundTag tag, net.minecraft.world.level.Level level) {
        try {
            //? if >=26.1 {
            /*Entity entity = EntityType.loadEntityRecursive(tag.copy(), level, net.minecraft.world.entity.EntitySpawnReason.LOAD,
                    net.minecraft.world.entity.EntityProcessor.NOP);
            *///?} else {
            Entity entity = EntityType.loadEntityRecursive(tag.copy(), level, loaded -> loaded);
            //?}
            // A name shown over the head would face the game's camera, not the preview's.
            if (entity != null) entity.setCustomNameVisible(false);
            return entity;
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add(tag.getString("id"))) LOGGER.warn("A structure preview cannot draw entity {}", tag.getString("id"), e);
            return null;
        }
    }
}
