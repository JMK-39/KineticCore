package dev.xyat.kineticcore.feature.flight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class FlightCommand {
    private FlightCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("flight")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on").executes(context -> set(context.getSource(), true)))
                .then(Commands.literal("off").executes(context -> set(context.getSource(), false)))
                .then(Commands.literal("toggle").executes(context -> toggle(context.getSource()))));
    }

    private static int set(CommandSourceStack source, boolean enabled) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(KineticI18n.translatable("cmd.kineticcore.flight.player_only"));
            return 0;
        }
        boolean commandEnabled = KineticSuperFlight.setCommandEnabled(player, enabled);
        if (enabled && commandEnabled) {
            KineticSuperFlight.setActive(player, true);
        }
        source.sendSuccess(() -> KineticI18n.translatable(
                commandEnabled ? "cmd.kineticcore.flight.enabled" : "cmd.kineticcore.flight.disabled"
        ), false);
        return 1;
    }

    private static int toggle(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(KineticI18n.translatable("cmd.kineticcore.flight.player_only"));
            return 0;
        }
        return set(source, !KineticSuperFlight.commandEnabled(player));
    }
}
