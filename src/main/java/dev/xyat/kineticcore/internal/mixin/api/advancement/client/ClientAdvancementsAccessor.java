package dev.xyat.kineticcore.internal.mixin.api.advancement.client;

import dev.xyat.kineticcore.internal.client.advancement.ClientAdvancementsAccess;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccessor extends ClientAdvancementsAccess {
    @Override
    @Accessor("progress")
    Map<Advancement, AdvancementProgress> kineticcore$getProgress();
}
