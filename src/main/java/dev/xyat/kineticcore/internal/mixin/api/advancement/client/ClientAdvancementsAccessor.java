package dev.xyat.kineticcore.internal.mixin.api.advancement.client;

import dev.xyat.kineticcore.internal.client.advancement.ClientAdvancementsAccess;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
//? if >=1.20.2 {
/*import net.minecraft.advancements.AdvancementHolder;
*///?} else {
import net.minecraft.advancements.Advancement;
//?}

import java.util.Map;

@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccessor extends ClientAdvancementsAccess {
    @Override
    @Accessor("progress")
    //? if >=1.20.2 {
    /*Map<AdvancementHolder, AdvancementProgress> kineticcore$getProgress();
    *///?} else {
    Map<Advancement, AdvancementProgress> kineticcore$getProgress();
    //?}
}
