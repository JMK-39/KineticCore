package dev.xyat.kineticcore.feature.attribute.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.runtime.KineticModLifecycle;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.feature.attribute.config.AttributeConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * 属性修改处理器
 * 负责属性上限的应用以及客户端配置翻译的刷新
 */
public class AttributeFixHandler {
    private static final KineticRegistrationBatch COMMON_REGISTRATION = new KineticRegistrationBatch();
    private static final KineticRegistrationBatch CLIENT_REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        COMMON_REGISTRATION.run(() -> KineticModLifecycle.onLoadComplete(AttributeConfig::loadAndApply));
    }

    public static void registerClient() {
        CLIENT_REGISTRATION.run(() -> KineticClientEvents.onScreenInitAfter(context -> onScreenInit(context.screen())));
    }


    // 标志位：确保翻译刷新在一次进程中只运行一次
    private static boolean translationsRefreshed = false;

    private static void onScreenInit(Screen screen) {
        if (screen instanceof TitleScreen && !translationsRefreshed) {
            try {
                AttributeConfig.refreshTranslationsAndSave();
                translationsRefreshed = true;
            } catch (Exception e) {
                KineticRuntime.logger().error("AttributeConfig: 刷新翻译时发生错误", e);
            }
        }
    }
}
