package dev.xyat.kineticcore.feature.worldinit.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.client.editor.KineticCommandListEditor;
import net.minecraft.client.gui.screens.Screen;

public class WorldInitConfigGui {
    public static final String PAGE_ID = "kineticcore:worldinit";

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticcore.worldinit.title")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.NEXT_WORLD_LOAD)
                .applyNotice(KineticI18n.translatable("cfg.kineticcore.worldinit.apply_notice"))
                .section(KineticI18n.translatable("cfg.kineticcore.worldinit.title"))
                .booleanValue(
                        "enable_world_init",
                        KineticI18n.translatable("cfg.kineticcore.worldinit.enable"),
                        () -> WorldInitConfig.enableWorldInit,
                        value -> WorldInitConfig.enableWorldInit = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.worldinit.enable.tooltip")
                )
                .action(
                        "commands",
                        KineticI18n.translatable("cfg.kineticcore.worldinit.commands"),
                        KTConfigApi.screenAction(parent -> KineticCommandListEditor.create(
                                parent,
                                () -> WorldInitConfig.worldInitCommands,
                                value -> WorldInitConfig.worldInitCommands = value,
                                PAGE_ID,
                                "commands",
                                new KineticCommandListEditor.Text(
                                        KineticI18n.translatable("gui.kineticcore.worldinit.command_list.title"),
                                        KineticI18n.translatable("gui.kineticcore.worldinit.command_edit.add_title"),
                                        KineticI18n.translatable("gui.kineticcore.worldinit.command_edit.edit_title"),
                                        KineticI18n.translatable("gui.kineticcore.worldinit.command_list.empty"),
                                        KineticI18n.translatable("msg.kineticcore.worldinit.command_edit.saved"),
                                        KineticI18n.translatable("msg.kineticcore.worldinit.command_list.deleted"),
                                        KineticI18n.translatable("msg.kineticcore.worldinit.command_list.save_failed"),
                                        null
                                )
                        )),
                        KineticI18n.translatable("cfg.kineticcore.worldinit.commands.tooltip")
                )
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createRegisteredPageScreen(parent, PAGE_ID);
    }
}
