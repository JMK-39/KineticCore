package dev.xyat.kineticcore.feature.flight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.feature.flight.FlightState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class FlightCommand {
    private FlightCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("flight")
                .then(Commands.literal("on").requires(source -> source.hasPermission(2)).executes(context -> set(context.getSource(), true)))
                .then(Commands.literal("off").requires(source -> source.hasPermission(2)).executes(context -> set(context.getSource(), false)))
                .then(Commands.literal("toggle").requires(source -> source.hasPermission(2)).executes(context -> toggle(context.getSource())))
                .then(Commands.literal("tips").requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.literal("off").executes(context -> {
                            FlightState.disableTutorial(context.getSource().getPlayerOrException());
                            context.getSource().sendSuccess(() -> KineticI18n.translatable("msg.kineticcore.flying.tips.disabled"), false);
                            return 1;
                        }))));
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
