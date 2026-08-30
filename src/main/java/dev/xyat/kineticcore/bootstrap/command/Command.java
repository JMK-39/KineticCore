package dev.xyat.kineticcore.bootstrap.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import dev.xyat.kineticcore.command.CommandUtils;
import dev.xyat.kineticcore.command.KTCommandApi;
import dev.xyat.kineticcore.feature.nbt.command.NbtCommand;
import dev.xyat.kineticcore.feature.tps.command.TpsCommand;

public class Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kt");

        KTLegacyCommandBridge.registerCommands(root);
        TpsCommand.register(root);
        NbtCommand.register(root);
        KTCommandApi.registerCommands(root);

        root.executes(ctx -> sendGlobalHelp(ctx.getSource()));
        dispatcher.register(root);
    }

    private static int sendGlobalHelp(CommandSourceStack source) {
        MutableComponent msg = CommandUtils.createHeader("mod.kineticcore.full_desc").append("\n");
        List<MutableComponent> items = new ArrayList<>();

        KTLegacyCommandBridge.appendHelpItems(source, items);
        items.add(CommandUtils.createExecutableCommand("/kt tps", "cmd.kineticcore.tps.desc"));
        items.add(CommandUtils.createExecutableCommand("/kt nbt", "cmd.kineticcore.nbt.desc"));
        KTCommandApi.appendHelpItems(source, items);

        for (int i = 0; i < items.size(); i++) {
            if (i > 0 && i % 3 == 0) {
                msg.append("\n");
            } else if (i % 3 != 0) {
                msg.append("  ");
            }
            msg.append(items.get(i));
        }

        source.sendSuccess(() -> msg, false);
        return 1;
    }
}
