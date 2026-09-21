package dev.xyat.kineticcore.feature.cobweb.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class AxesEventHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticPlayerEvents.onHarvestCheck(KineticEventPriority.NORMAL, AxesEventHandler::onHarvestCheck),
                () -> KineticPlayerEvents.onBreakSpeed(KineticEventPriority.NORMAL, AxesEventHandler::onBreakSpeed)
        );
    }


    public static void onHarvestCheck(KineticPlayerEvents.HarvestCheckContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "fast_web", true)) return;

        if (context.state().getBlock() == Blocks.COBWEB) {
            ItemStack stack = context.player().getMainHandItem();
            if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
                context.canHarvest(true);
            }
        }
    }

    public static void onBreakSpeed(KineticPlayerEvents.BreakSpeedContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "fast_web", true)) return;

        if (context.state().getBlock() == Blocks.COBWEB) {
            ItemStack stack = context.player().getMainHandItem();
            if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
                context.speed(15.0f);
            }
        }
    }
}
