package dev.xyat.kineticcore.api.client.advancement;

import dev.xyat.kineticcore.internal.client.advancement.KineticClientAdvancementRuntime;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public final class KineticClientAdvancements {
    private KineticClientAdvancements() {
    }

    public static Set<ResourceLocation> completedIds() {
        return KineticClientAdvancementRuntime.completedIds();
    }

    public static List<Advancement> all() {
        return KineticClientAdvancementRuntime.all();
    }
}
