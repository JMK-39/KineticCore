package dev.xyat.kineticcore.feature.worldinit.config;

import dev.xyat.kineticcore.ConfigGui;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import dev.xyat.kineticcore.feature.firstjoin.client.FirstJoinCommandListScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@KTClientModule
public class WorldInitConfigGui {
    public static final String PAGE_ID = "kineticcore:worldinit";

    public static void load() {
        ConfigGui.register(KTConfigPage.builder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.worldinit.title")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.NEXT_WORLD_LOAD)
                .applyNotice(Component.translatable("cfg.kineticcore.worldinit.apply_notice"))
                .section(Component.translatable("cfg.kineticcore.worldinit.title"))
                .booleanValue(
                        "enable_world_init",
                        Component.translatable("cfg.kineticcore.worldinit.enable"),
                        () -> WorldInitConfig.enableWorldInit,
                        value -> WorldInitConfig.enableWorldInit = value,
                        true,
                        Component.translatable("cfg.kineticcore.worldinit.enable.tooltip")
                )
                .action(
                        "commands",
                        Component.translatable("cfg.kineticcore.worldinit.commands"),
                        KTConfigApi.screenAction(FirstJoinCommandListScreen::forWorldInit),
                        Component.translatable("cfg.kineticcore.worldinit.commands.tooltip")
                )
                .build());
    }

    public static Screen create(Screen parent) {
        return ConfigGui.create(parent, PAGE_ID);
    }
}
