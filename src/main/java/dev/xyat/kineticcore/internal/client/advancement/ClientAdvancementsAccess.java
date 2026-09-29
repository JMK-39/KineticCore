package dev.xyat.kineticcore.internal.client.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;

import java.util.Map;

public interface ClientAdvancementsAccess {
    Map<Advancement, AdvancementProgress> kineticcore$getProgress();
}
