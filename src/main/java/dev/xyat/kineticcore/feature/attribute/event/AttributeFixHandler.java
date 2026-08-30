package dev.xyat.kineticcore.feature.attribute.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.attribute.config.AttributeConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/**
 * 属性修改处理器
 * 负责属性上限的应用以及客户端配置翻译的刷新
 */
public class AttributeFixHandler {

    // 标志位：确保翻译刷新在一次进程中只运行一次
    private static boolean translationsRefreshed = false;

    /**
     * MOD总线事件：处理生命周期
     */
    @Mod.EventBusSubscriber(modid = KineticCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {
            // 在加载完成后应用属性修改（服务端/客户端通用）
            event.enqueueWork(AttributeConfig::loadAndApply);
        }
    }

    /**
     * FORGE总线事件：处理游戏内UI逻辑（仅客户端）
     */
    @Mod.EventBusSubscriber(modid = KineticCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            // 检查当前打开的是否是主界面（TitleScreen）
            if (event.getScreen() instanceof TitleScreen && !translationsRefreshed) {
                try {
                    // 执行翻译刷新并保存配置文件注释
                    AttributeConfig.refreshTranslationsAndSave();
                    translationsRefreshed = true;
                } catch (Exception e) {
                    KineticCore.LOGGER.error("AttributeConfig: 刷新翻译时发生错误", e);
                }
            }
        }
    }
}