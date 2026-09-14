package dev.xyat.kineticcore.feature.cobweb.event;

import net.minecraftforge.common.MinecraftForge;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class AxesEventHandler {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(AxesEventHandler::onHarvestCheck);
        MinecraftForge.EVENT_BUS.addListener(AxesEventHandler::onBreakSpeed);
    }


    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "fast_web", true)) return;

        if (event.getTargetBlock().getBlock() == Blocks.COBWEB) {
            ItemStack stack = event.getEntity().getMainHandItem();
            if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
                event.setCanHarvest(true);
            }
        }
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "fast_web", true)) return;

        if (event.getState().getBlock() == Blocks.COBWEB) {
            ItemStack stack = event.getEntity().getMainHandItem();
            if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
                event.setNewSpeed(15.0f);
            }
        }
    }
}
