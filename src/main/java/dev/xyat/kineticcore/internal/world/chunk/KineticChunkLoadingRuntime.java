package dev.xyat.kineticcore.internal.world.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
//? if forge {
import net.minecraftforge.common.world.ForgeChunkManager;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.HashMap;
import java.util.Map;
*///?}

//? if neoforge && >=26.1 {
/*// 26.1 routes mod bus events to subscribers on its own.
@EventBusSubscriber(modid = "kineticcore")
*///?} else if neoforge {
/*@EventBusSubscriber(modid = "kineticcore", bus = EventBusSubscriber.Bus.MOD)*/
//?}
public final class KineticChunkLoadingRuntime {
    //? if neoforge {
    /*// NeoForge only accepts ticket controllers while mods load, but the API names the owning mod when a chunk is
    // forced, so every loaded mod gets its own controller up front.
    private static final Map<String, TicketController> CONTROLLERS = new HashMap<>();

    @SubscribeEvent
    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        for (IModInfo mod : ModList.get().getMods()) {
            TicketController controller = new TicketController(ResourceLocation.fromNamespaceAndPath("kineticcore", mod.getModId()));
            event.register(controller);
            CONTROLLERS.put(mod.getModId(), controller);
        }
    }
    *///?}

    private KineticChunkLoadingRuntime() {
    }

    public static void setForced(
            ServerLevel level,
            String ownerModId,
            BlockPos ownerPos,
            int chunkX,
            int chunkZ,
            boolean forced,
            boolean ticking
    ) {
        //? if forge {
        ForgeChunkManager.forceChunk(level, ownerModId, ownerPos, chunkX, chunkZ, forced, ticking);
        //?} else {
        /*TicketController controller = CONTROLLERS.get(ownerModId);
        if (controller == null) throw new IllegalArgumentException("Unknown mod id: " + ownerModId);
        controller.forceChunk(level, ownerPos, chunkX, chunkZ, forced, ticking);
        *///?}
    }
}
