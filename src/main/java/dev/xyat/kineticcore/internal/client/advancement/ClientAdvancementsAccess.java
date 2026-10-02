package dev.xyat.kineticcore.internal.client.advancement;

import net.minecraft.advancements.AdvancementProgress;
//? if >=1.20.2 {
/*import net.minecraft.advancements.AdvancementHolder;
*///?} else {
import net.minecraft.advancements.Advancement;
//?}

import java.util.Map;

public interface ClientAdvancementsAccess {
    //? if >=1.20.2 {
    /*Map<AdvancementHolder, AdvancementProgress> kineticcore$getProgress();
    *///?} else {
    Map<Advancement, AdvancementProgress> kineticcore$getProgress();
    //?}
}
