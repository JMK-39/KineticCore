package dev.xyat.kineticcore.feature.firstjoin.config;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.client.editor.KineticCommandListEditor;
import dev.xyat.kineticcore.feature.firstjoin.client.FirstJoinEquipmentScreen;
import dev.xyat.kineticcore.feature.firstjoin.client.FirstJoinRewardItemsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public final class PlayerConfigGui {
    public static final String PAGE_ID = "kineticcore:first_join";

    private PlayerConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(PAGE_ID, Component.translatable("cfg.kineticcore.first_join"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .booleanValue("enabled", Component.translatable("cfg.kineticcore.join.enable"),
                        () -> PlayerConfig.enableFirstJoin, value -> PlayerConfig.enableFirstJoin = value, true,
                        Component.translatable("cfg.kineticcore.join.enable.tooltip"))
                .booleanValue("clear_inventory", Component.translatable("cfg.kineticcore.join.clear_inventory"),
                        () -> PlayerConfig.clearInvBeforeJoin, value -> PlayerConfig.clearInvBeforeJoin = value, true,
                        Component.translatable("cfg.kineticcore.join.clear_inventory.tooltip"))
                .tickSecondsValue("delay", Component.translatable("cfg.kineticcore.join.delay"),
                        () -> PlayerConfig.firstJoinDelay, value -> PlayerConfig.firstJoinDelay = value,
                        20, 0, Integer.MAX_VALUE, Component.translatable("cfg.kineticcore.join.delay.tooltip"))
                .action("items", Component.translatable("cfg.kineticcore.join.items"),
                        KTConfigApi.screenAction(FirstJoinRewardItemsScreen::new),
                        Component.translatable("cfg.kineticcore.join.items.tooltip"))
                .action("commands", Component.translatable("cfg.kineticcore.join.commands"),
                        KTConfigApi.screenAction(parent -> KineticCommandListEditor.create(
                                parent,
                                () -> PlayerConfig.firstJoinCommands,
                                value -> PlayerConfig.firstJoinCommands = value,
                                PAGE_ID,
                                "commands",
                                new KineticCommandListEditor.Text(
                                        Component.translatable("gui.kineticcore.firstjoin.command_list.title"),
                                        Component.translatable("gui.kineticcore.firstjoin.command_edit.add_title"),
                                        Component.translatable("gui.kineticcore.firstjoin.command_edit.edit_title"),
                                        Component.translatable("gui.kineticcore.firstjoin.command_list.empty"),
                                        Component.translatable("msg.kineticcore.firstjoin.command_edit.saved"),
                                        Component.translatable("msg.kineticcore.firstjoin.command_list.deleted"),
                                        Component.translatable("msg.kineticcore.firstjoin.command_list.save_failed"),
                                        Component.translatable("gui.kineticcore.firstjoin.command_edit.variables")
                                )
                        )),
                        Component.translatable("cfg.kineticcore.join.commands.tooltip"))
                .section(Component.translatable("cfg.kineticcore.join.equipment"))
                .action("equipment_editor", Component.translatable("cfg.kineticcore.join.equipment.editor"),
                        KTConfigApi.screenAction(FirstJoinEquipmentScreen::new),
                        Component.translatable("cfg.kineticcore.join.equipment.editor.tooltip"))
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, PAGE_ID);
    }
}
