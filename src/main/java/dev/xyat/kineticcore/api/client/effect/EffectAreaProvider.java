package dev.xyat.kineticcore.api.client.effect;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/**
 * Implemented on inventory screens by KineticCore's effect display so other code can avoid drawing over the
 * potion-effect panel.
 */
public interface EffectAreaProvider {
    /** Returns the screen rectangles covered by effect entries in the last frame; empty when none are drawn. */
    List<Rect2i> kineticcore$effectAreas();

    /** Returns whether the effect panel is currently shown in its expanded layout. */
    boolean kineticcore$effectsExpanded();
}
