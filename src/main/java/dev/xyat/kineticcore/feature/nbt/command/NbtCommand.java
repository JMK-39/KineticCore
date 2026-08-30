package dev.xyat.kineticcore.feature.nbt.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.xyat.kineticcore.command.CommandUtils;
import dev.xyat.kineticcore.feature.nbt.network.NbtNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class NbtCommand {
    private NbtCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> nbt = Commands.literal("nbt")
                .executes(context -> sendHelp(context.getSource()));

        nbt.then(Commands.literal("hand")
                .requires(source -> source.hasPermission(2))
                .executes(NbtCommand::openHand));

        nbt.then(Commands.literal("entity")
                .requires(source -> source.hasPermission(2))
                .executes(NbtCommand::openCrosshair));

        root.then(nbt);
    }

    private static int sendHelp(CommandSourceStack source) {
        MutableComponent message = CommandUtils.createHeader("cmd.kineticcore.nbt.desc").append("\n")
                .append(CommandUtils.createExecutableCommand("/kt nbt hand", "cmd.kineticcore.nbt.hand.desc"))
                .append("\n")
                .append(CommandUtils.createExecutableCommand("/kt nbt entity", "cmd.kineticcore.nbt.entity.desc"));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int openHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NbtNetwork.openFromCommand(player, NbtNetwork.COMMAND_OPEN_HAND);
        return 1;
    }

    private static int openCrosshair(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NbtNetwork.openFromCommand(player, NbtNetwork.COMMAND_OPEN_CROSSHAIR);
        return 1;
    }
}
