package dev.xyat.kineticcore.feature.food.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FoodAndToolTweaks {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(() -> KineticPlayerEvents.onRightClickItem(KineticEventPriority.NORMAL, FoodAndToolTweaks::onRightClickItem));
    }


    public static void onRightClickItem(KineticPlayerEvents.RightClickItemContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "always_edible", true)) return;
        ItemStack stack = context.stack();
        if (!stack.isEdible()) return;

        Player player = context.player();
        if (!player.canEat(false)) {
            player.startUsingItem(context.hand());
            context.cancel();
            context.cancellationResult(InteractionResult.CONSUME);
        }
    }
}
