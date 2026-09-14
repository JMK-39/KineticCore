package dev.xyat.kineticcore.feature.food.event;

import net.minecraftforge.common.MinecraftForge;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class FoodAndToolTweaks {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(FoodAndToolTweaks::onRightClickItem);
    }


    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "always_edible", true)) return;
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
