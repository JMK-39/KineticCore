package dev.xyat.kineticcore.api.client.advancement;

import dev.xyat.kineticcore.internal.client.advancement.KineticClientAdvancementRuntime;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/** Public Kinetic API facade for client advancements. */
public final class KineticClientAdvancements {
    private KineticClientAdvancements() {
    }

    /**
     * Performs the completed ids API operation.
     */
    public static Set<ResourceLocation> completedIds() {
        return KineticClientAdvancementRuntime.completedIds();
    }

    /**
     * Performs the all API operation.
     */
    public static List<Advancement> all() {
        return KineticClientAdvancementRuntime.all();
    }
}
