package dev.xyat.kineticcore.feature.firstjoin.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class FirstJoinCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("setfirstjoin")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    List<String> newItems = new ArrayList<>();

                    // 遍历玩家的主背包 (0-35 涵盖快捷栏和主要背包，不含防具槽)
                    for (int i = 0; i < player.getInventory().items.size(); i++) {
                        ItemStack stack = player.getInventory().items.get(i);
                        if (!stack.isEmpty()) {
                            // 使用[slot] 格式严格保留物品放置顺序
                            newItems.add("[" + i + "] " + PlayerConfig.serializeItemStack(stack));
                        }
                    }

                    // 写入配置内存
                    PlayerConfig.firstJoinItemsRaw = newItems;
                    PlayerConfig.helmetId = PlayerConfig.serializeItemStack(player.getItemBySlot(EquipmentSlot.HEAD));
                    PlayerConfig.chestplateId = PlayerConfig.serializeItemStack(player.getItemBySlot(EquipmentSlot.CHEST));
                    PlayerConfig.leggingsId = PlayerConfig.serializeItemStack(player.getItemBySlot(EquipmentSlot.LEGS));
                    PlayerConfig.bootsId = PlayerConfig.serializeItemStack(player.getItemBySlot(EquipmentSlot.FEET));
                    PlayerConfig.offhandId = PlayerConfig.serializeItemStack(player.getItemBySlot(EquipmentSlot.OFFHAND));

                    // 保存至磁盘
                    PlayerConfig.save();

                    ctx.getSource().sendSuccess(() -> Component.translatable("cmd.kineticcore.setfirstjoin.success").withStyle(ChatFormatting.GREEN), true);
                    return 1;
                })
        );
    }
}