package dev.xyat.kineticcore.feature.datapack.config;

import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import dev.xyat.kineticcore.feature.datapack.PackModule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

@KTClientModule
public final class PackConfigGui {
    public static final String PAGE_ID = "kineticcore:datapack_pack_order";
    public static final String RESOURCE_PAGE_ID = "kineticcore:resourcepack_pack_order";

    private PackConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticcore.datapack.pack_order.title")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.RELOAD_REQUIRED)
                .applyNotice(Component.translatable("cfg.kineticcore.datapack.pack_order.apply_notice"))
                .stringList(
                        "datapacks",
                        Component.translatable("cfg.kineticcore.datapack.pack_order.datapacks"),
                        PackModule::datapackOrderSnapshot,
                        PackModule::replaceDatapackOrder,
                        List.of(),
                        Component.translatable("cfg.kineticcore.datapack.pack_order.datapacks.tooltip")
                )
                .build());

        KTConfigApi.register(KTConfigPage.builder(
                        RESOURCE_PAGE_ID,
                        Component.translatable("cfg.kineticcore.resourcepack.pack_order.title")
                )
                .scope(KTConfigScope.CLIENT_LOCAL)
                .applyTiming(KTConfigPage.ApplyTiming.RELOAD_REQUIRED)
                .applyNotice(Component.translatable("cfg.kineticcore.datapack.pack_order.apply_notice"))
                .stringList(
                        "resourcepacks",
                        Component.translatable("cfg.kineticcore.datapack.pack_order.resourcepacks"),
                        PackModule::resourcePackOrderSnapshot,
                        PackModule::replaceResourcePackOrder,
                        List.of(),
                        Component.translatable("cfg.kineticcore.datapack.pack_order.resourcepacks.tooltip")
                )
                .onSave(PackModule::saveResourcePackOrder)
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, PAGE_ID);
    }
}
