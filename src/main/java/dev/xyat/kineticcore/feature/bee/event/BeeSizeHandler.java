package dev.xyat.kineticcore.feature.bee.event;

import dev.xyat.kineticcore.MixinPlugin;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kineticcore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeeSizeHandler {

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        // 如果 feature.bee.BeeMixins 被关闭，则直接返回，不执行后续的体型缩小逻辑
        if (!MixinPlugin.isFeatureEnabled("feature.bee.BeeMixins")) {
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