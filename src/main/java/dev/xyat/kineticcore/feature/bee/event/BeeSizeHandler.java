package dev.xyat.kineticcore.feature.bee.event;

import net.minecraftforge.common.MinecraftForge;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.EntityEvent;

public class BeeSizeHandler {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(BeeSizeHandler::onEntitySize);
    }


    public static void onEntitySize(EntityEvent.Size event) {
        if (!KineticFeatureSwitches.isEnabled("entity.bee_logic")) {
            return;
        }

        if (event.getEntity() instanceof Bee) {
            // 获取原本的尺寸，并缩放 0.25 倍
            EntityDimensions newSize = event.getNewSize().scale(0.25F);
            event.setNewSize(newSize);

            // 眼睛高度（视线）也必须同步缩小，否则蜜蜂的寻路和视线判定会出错
            event.setNewEyeHeight(event.getNewEyeHeight() * 0.25F);
        }
    }
}
