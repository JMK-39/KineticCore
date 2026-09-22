package dev.xyat.kineticcore.feature.firstjoin.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.client.editor.KineticCommandListEditor;
import dev.xyat.kineticcore.feature.firstjoin.client.FirstJoinEquipmentScreen;
import dev.xyat.kineticcore.feature.firstjoin.client.FirstJoinRewardItemsScreen;
import net.minecraft.client.gui.screens.Screen;


public final class PlayerConfigGui {
    public static final String PAGE_ID = "kineticcore:first_join";

    private PlayerConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(PAGE_ID, KineticI18n.translatable("cfg.kineticcore.first_join"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .booleanValue("enabled", KineticI18n.translatable("cfg.kineticcore.join.enable"),
                        () -> PlayerConfig.enableFirstJoin, value -> PlayerConfig.enableFirstJoin = value, true,
                        KineticI18n.translatable("cfg.kineticcore.join.enable.tooltip"))
                .booleanValue("clear_inventory", KineticI18n.translatable("cfg.kineticcore.join.clear_inventory"),
                        () -> PlayerConfig.clearInvBeforeJoin, value -> PlayerConfig.clearInvBeforeJoin = value, true,
                        KineticI18n.translatable("cfg.kineticcore.join.clear_inventory.tooltip"))
                .tickSecondsValue("delay", KineticI18n.translatable("cfg.kineticcore.join.delay"),
                        () -> PlayerConfig.firstJoinDelay, value -> PlayerConfig.firstJoinDelay = value,
                        20, 0, Integer.MAX_VALUE, KineticI18n.translatable("cfg.kineticcore.join.delay.tooltip"))
                .action("items", KineticI18n.translatable("cfg.kineticcore.join.items"),
                        KTConfigApi.screenAction(FirstJoinRewardItemsScreen::new),
                        KineticI18n.translatable("cfg.kineticcore.join.items.tooltip"))
                .action("commands", KineticI18n.translatable("cfg.kineticcore.join.commands"),
                        KTConfigApi.screenAction(parent -> KineticCommandListEditor.create(
                                parent,
                                () -> PlayerConfig.firstJoinCommands,
                                value -> PlayerConfig.firstJoinCommands = value,
                                PAGE_ID,
                                "commands",
                                new KineticCommandListEditor.Text(
                                        KineticI18n.translatable("gui.kineticcore.firstjoin.command_list.title"),
                                        KineticI18n.translatable("gui.kineticcore.firstjoin.command_edit.add_title"),
                                        KineticI18n.translatable("gui.kineticcore.firstjoin.command_edit.edit_title"),
                                        KineticI18n.translatable("gui.kineticcore.firstjoin.command_list.empty"),
                                        KineticI18n.translatable("msg.kineticcore.firstjoin.command_edit.saved"),
                                        KineticI18n.translatable("msg.kineticcore.firstjoin.command_list.deleted"),
                                        KineticI18n.translatable("msg.kineticcore.firstjoin.command_list.save_failed"),
                                        KineticI18n.translatable("gui.kineticcore.firstjoin.command_edit.variables")
                                )
                        )),
                        KineticI18n.translatable("cfg.kineticcore.join.commands.tooltip"))
                .divider()
                .action("equipment_editor", KineticI18n.translatable("cfg.kineticcore.join.equipment.editor"),
                        KTConfigApi.screenAction(FirstJoinEquipmentScreen::new),
                        KineticI18n.translatable("cfg.kineticcore.join.equipment.editor.tooltip"))
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createRegisteredPageScreen(parent, PAGE_ID);
    }
}
