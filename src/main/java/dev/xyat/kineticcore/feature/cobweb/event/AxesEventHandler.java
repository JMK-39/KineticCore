package dev.xyat.kineticcore.feature.cobweb.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticCore.MODID)
public class AxesEventHandler {

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!GeneralMechanicsConfig.fastCobWebBreaking) return;

        if (event.getTargetBlock().getBlock() == Blocks.COBWEB) {
            ItemStack stack = event.getEntity().getMainHandItem();
            if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
                event.setCanHarvest(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!GeneralMechanicsConfig.fastCobWebBreaking) return;

        if (event.getState().getBlock() == Blocks.COBWEB) {
            ItemStack stack = event.getEntity().getMainHandItem();
            if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
                event.setNewSpeed(15.0f);
            }
        }
    }
}