package dev.xyat.kineticcore.feature.food.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticCore.MODID)
public class FoodAndToolTweaks {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!GeneralMechanicsConfig.enableAlwaysEdible) return;
        ItemStack stack = event.getItemStack();
        if (!stack.isEdible()) return;

        Player player = event.getEntity();
        if (!player.canEat(false)) {
            player.startUsingItem(event.getHand());
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }
}