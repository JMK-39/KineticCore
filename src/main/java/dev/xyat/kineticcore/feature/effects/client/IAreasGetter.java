package dev.xyat.kineticcore.feature.effects.client;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

public interface IAreasGetter {
    List<Rect2i> kineticRefined$getAreas();
    boolean kineticRefined$isExpanded();
}