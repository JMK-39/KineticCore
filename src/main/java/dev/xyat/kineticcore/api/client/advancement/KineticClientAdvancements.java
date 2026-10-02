package dev.xyat.kineticcore.api.client.advancement;

import dev.xyat.kineticcore.internal.client.advancement.KineticClientAdvancementRuntime;
//? if >=1.20.2 {
/*import net.minecraft.advancements.AdvancementHolder;
*///?} else {
import net.minecraft.advancements.Advancement;
//?}
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/** Read-only view of the advancements the server has sent to this client. Call on the client thread. */
public final class KineticClientAdvancements {
    private KineticClientAdvancements() {
    }

    /**
     * Returns the ids of advancements the local player has completed.
     *
     * @return an immutable set; empty while not connected or when progress cannot be read
     */
    public static Set<ResourceLocation> completedIds() {
        return KineticClientAdvancementRuntime.completedIds();
    }

    //? if >=1.20.2 {
    /*/^*
     * Returns every advancement the server has sent, with its id.
     *
     * @return a new list; empty while not connected
     ^/
    public static List<AdvancementHolder> all() {
        return KineticClientAdvancementRuntime.all();
    }
    *///?} else {
    /**
     * Returns every advancement the server has sent.
     *
     * @return a new list; empty while not connected
     */
    public static List<Advancement> all() {
        return KineticClientAdvancementRuntime.all();
    }
    //?}
}
