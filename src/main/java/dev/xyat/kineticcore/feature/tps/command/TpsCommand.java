package dev.xyat.kineticcore.feature.tps.command;

import net.minecraft.ChatFormatting;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.xyat.kineticcore.feature.tps.logic.ITpsServer;
import dev.xyat.kineticcore.feature.tps.logic.TpsTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

import java.util.Locale;

public final class TpsCommand {
    private TpsCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("tps")
                .executes(TpsCommand::checkTps));
    }

    private static int checkTps(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        if (!(server instanceof ITpsServer tpsServer)) {
            source.sendFailure(Component.translatable("cmd.kineticcore.tps.unavailable"));
            return 0;
        }

        TpsTracker tracker = tpsServer.kineticcore$getTpsTracker();
        double currentMspt = tracker.getLatestMspt();
        double average10Mspt = tracker.getStats(10, 0);
        double maximum10Mspt = tracker.getStats(10, 1);
        double average60Mspt = tracker.getStats(60, 0);
        double maximum60Mspt = tracker.getStats(60, 1);

        MutableComponent message = Component.empty()
                .append(Component.translatable("cmd.kineticcore.tps.report_header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append("\n")
                .append(Component.translatable("cmd.kineticcore.tps.report.prefix.tps").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD))
                .append(gray("cmd.kineticcore.tps.current"))
                .append(formatTps(TpsTracker.tps(currentMspt)))
                .append(gray("cmd.kineticcore.tps.report.separator"))
                .append(gray("cmd.kineticcore.tps.10s_avg"))
                .append(formatTps(TpsTracker.tps(average10Mspt)))
                .append(gray("cmd.kineticcore.tps.report.separator"))
                .append(gray("cmd.kineticcore.tps.60s_avg"))
                .append(formatTps(TpsTracker.tps(average60Mspt))).append("\n")
                .append(Component.translatable("cmd.kineticcore.tps.report.prefix.minimum").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(gray("cmd.kineticcore.tps.10s_min"))
                .append(formatTps(TpsTracker.tps(maximum10Mspt)))
                .append(gray("cmd.kineticcore.tps.report.separator"))
                .append(gray("cmd.kineticcore.tps.60s_min"))
                .append(formatTps(TpsTracker.tps(maximum60Mspt))).append("\n")
                .append(Component.translatable("cmd.kineticcore.tps.report.prefix.mspt").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                .append(gray("cmd.kineticcore.tps.current"))
                .append(formatMspt(currentMspt))
                .append(gray("cmd.kineticcore.tps.report.separator"))
                .append(gray("cmd.kineticcore.tps.10s_avg"))
                .append(formatMspt(average10Mspt))
                .append(gray("cmd.kineticcore.tps.report.separator"))
                .append(gray("cmd.kineticcore.tps.60s_avg"))
                .append(formatMspt(average60Mspt)).append("\n")
                .append(Component.translatable("cmd.kineticcore.tps.report.prefix.maximum").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(gray("cmd.kineticcore.tps.10s_max"))
                .append(formatMspt(maximum10Mspt))
                .append(gray("cmd.kineticcore.tps.report.separator"))
                .append(gray("cmd.kineticcore.tps.60s_max"))
                .append(formatMspt(maximum60Mspt));

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static Component gray(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    private static Component formatTps(double tps) {
        ChatFormatting color = tps >= 18.0D
                ? ChatFormatting.GREEN
                : tps >= 14.0D ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return Component.literal(String.format(Locale.ROOT, "%.2f", tps)).withStyle(color);
    }

    private static Component formatMspt(double mspt) {
        ChatFormatting color = mspt <= 40.0D
                ? ChatFormatting.GREEN
                : mspt <= 50.0D ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return Component.literal(String.format(Locale.ROOT, "%.2fms", mspt)).withStyle(color);
    }
}
