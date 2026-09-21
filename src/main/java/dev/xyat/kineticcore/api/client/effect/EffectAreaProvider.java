package dev.xyat.kineticcore.api.client.effect;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/** Provider contract for effect area data. */
public interface EffectAreaProvider {
    List<Rect2i> kineticcore$effectAreas();

    boolean kineticcore$effectsExpanded();
}
