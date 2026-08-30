package dev.xyat.kineticcore.bootstrap.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.command.KTCommandApi;

public class ReloadCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    KTLegacyCommandBridge.reloadCore();
                    KTCommandApi.reload(ctx.getSource());

                    ctx.getSource().sendSuccess(
                            () -> Component.translatable("cmd.kineticcore.reload.success").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                            true
                    );
                    return 1;
                })
        );
    }
}
