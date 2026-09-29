package dev.xyat.kineticcore.internal.client.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KineticClientAdvancementRuntime {
    private KineticClientAdvancementRuntime() {
    }

    public static Set<ResourceLocation> completedIds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return Set.of();
        if (!(minecraft.getConnection().getAdvancements() instanceof ClientAdvancementsAccess access)) return Set.of();

        Set<ResourceLocation> completed = new LinkedHashSet<>();
        for (Map.Entry<Advancement, AdvancementProgress> entry : access.kineticcore$getProgress().entrySet()) {
            if (entry.getValue().isDone()) {
                completed.add(entry.getKey().getId());
            }
        }
        return Set.copyOf(completed);
    }

    public static List<Advancement> all() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return List.of();
        return new ArrayList<>(minecraft.getConnection().getAdvancements().getAdvancements().getAllAdvancements());
    }
}
