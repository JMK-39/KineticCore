package dev.xyat.kineticcore.feature.bee.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.entity.event.KineticLivingEvents;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.Bee;

public class BeeSizeHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(() -> KineticLivingEvents.onSize(KineticEventPriority.NORMAL, BeeSizeHandler::onEntitySize));
    }


    public static void onEntitySize(KineticLivingEvents.SizeContext context) {
        if (!KineticFeatureSwitches.isEnabled("entity.bee_logic")) {
            return;
        }

        if (context.entity() instanceof Bee) {
            // 获取原本的尺寸，并缩放 0.25 倍
            EntityDimensions newSize = context.newSize().scale(0.25F);
            context.newSize(newSize);

            // 眼睛高度（视线）也必须同步缩小，否则蜜蜂的寻路和视线判定会出错
            context.newEyeHeight(context.newEyeHeight() * 0.25F);
        }
    }
}
