package dev.xyat.kineticcore.feature.cobweb.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Axes, pickaxes, shovels and hoes break cobwebs like a sword: same speed, and the web drops string. */
public class AxesEventHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    // A sword's mining speed on cobwebs.
    private static final float SWORD_SPEED = 15.0f;

    public static void register() {
        REGISTRATION.run(
                () -> KineticPlayerEvents.onHarvestCheck(KineticEventPriority.NORMAL, AxesEventHandler::onHarvestCheck),
                () -> KineticPlayerEvents.onBreakSpeed(KineticEventPriority.NORMAL, AxesEventHandler::onBreakSpeed)
        );
    }


    public static void onHarvestCheck(KineticPlayerEvents.HarvestCheckContext context) {
        if (!context.canHarvest() && appliesTo(context.state(), context.player().getMainHandItem())) {
            context.canHarvest(true);
        }
    }

    public static void onBreakSpeed(KineticPlayerEvents.BreakSpeedContext context) {
        ItemStack stack = context.player().getMainHandItem();
        // Only tools that are not already fast on cobwebs; the incoming speed then carries the tool's base speed of 1
        // times haste, mining fatigue, underwater and airborne factors, which keep applying as they do for a sword.
        if (appliesTo(context.state(), stack) && stack.getDestroySpeed(context.state()) <= 1.0f) {
            context.speed(context.speed() * SWORD_SPEED);
        }
    }

    private static boolean appliesTo(BlockState state, ItemStack stack) {
        if (state.getBlock() != Blocks.COBWEB || stack.isEmpty()) return false;
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "fast_web", true)) return false;
        return stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
    }
}
