package dev.xyat.kineticcore.feature.firstjoin.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.client.gui.editor.KineticCommandListEditor;
import dev.xyat.kineticcore.feature.firstjoin.client.FirstJoinRewardItemsPage;


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
                        KTConfigApi.pageAction(FirstJoinRewardItemsPage::new),
                        KineticI18n.translatable("cfg.kineticcore.join.items.tooltip"))
                .action("commands", KineticI18n.translatable("cfg.kineticcore.join.commands"),
                        KineticCommandListEditor.action(
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
                        ),
                        KineticI18n.translatable("cfg.kineticcore.join.commands.tooltip"))
                .build());
    }

}
