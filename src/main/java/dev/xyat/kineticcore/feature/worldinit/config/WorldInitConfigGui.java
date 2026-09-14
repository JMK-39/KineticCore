package dev.xyat.kineticcore.feature.worldinit.config;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.client.editor.KineticCommandListEditor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WorldInitConfigGui {
    public static final String PAGE_ID = "kineticcore:worldinit";

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
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
                        KTConfigApi.screenAction(parent -> KineticCommandListEditor.create(
                                parent,
                                () -> WorldInitConfig.worldInitCommands,
                                value -> WorldInitConfig.worldInitCommands = value,
                                PAGE_ID,
                                "commands",
                                new KineticCommandListEditor.Text(
                                        Component.translatable("gui.kineticcore.worldinit.command_list.title"),
                                        Component.translatable("gui.kineticcore.worldinit.command_edit.add_title"),
                                        Component.translatable("gui.kineticcore.worldinit.command_edit.edit_title"),
                                        Component.translatable("gui.kineticcore.worldinit.command_list.empty"),
                                        Component.translatable("msg.kineticcore.worldinit.command_edit.saved"),
                                        Component.translatable("msg.kineticcore.worldinit.command_list.deleted"),
                                        Component.translatable("msg.kineticcore.worldinit.command_list.save_failed"),
                                        null
                                )
                        )),
                        Component.translatable("cfg.kineticcore.worldinit.commands.tooltip")
                )
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, PAGE_ID);
    }
}
