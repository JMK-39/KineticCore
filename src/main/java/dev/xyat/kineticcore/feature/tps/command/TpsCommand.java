package dev.xyat.kineticcore.feature.tps.command;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.monitoring.KineticServerPerformance;
import dev.xyat.kineticcore.api.monitoring.ServerTickTracker;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
        ServerTickTracker tracker = KineticServerPerformance.tracker(server).orElse(null);
        if (tracker == null) {
            source.sendFailure(KineticI18n.translatable("cmd.kineticcore.tps.unavailable"));
            return 0;
        }

        double currentMspt = tracker.getLatestMspt();
        double average10Mspt = tracker.getStats(10, 0);
        double maximum10Mspt = tracker.getStats(10, 1);
        double average60Mspt = tracker.getStats(60, 0);
        double maximum60Mspt = tracker.getStats(60, 1);

        MutableComponent message = Component.empty()
                .append(KineticI18n.translatable("cmd.kineticcore.tps.report_header")).append("\n")
                .append(KineticI18n.translatable("cmd.kineticcore.tps.report.prefix.tps"))
                .append(label("cmd.kineticcore.tps.current"))
                .append(formatTps(KineticServerPerformance.tps(currentMspt)))
                .append(label("cmd.kineticcore.tps.report.separator"))
                .append(label("cmd.kineticcore.tps.10s_avg"))
                .append(formatTps(KineticServerPerformance.tps(average10Mspt)))
                .append(label("cmd.kineticcore.tps.report.separator"))
                .append(label("cmd.kineticcore.tps.60s_avg"))
                .append(formatTps(KineticServerPerformance.tps(average60Mspt))).append("\n")
                .append(KineticI18n.translatable("cmd.kineticcore.tps.report.prefix.minimum"))
                .append(label("cmd.kineticcore.tps.10s_min"))
                .append(formatTps(KineticServerPerformance.tps(maximum10Mspt)))
                .append(label("cmd.kineticcore.tps.report.separator"))
                .append(label("cmd.kineticcore.tps.60s_min"))
                .append(formatTps(KineticServerPerformance.tps(maximum60Mspt))).append("\n")
                .append(KineticI18n.translatable("cmd.kineticcore.tps.report.prefix.mspt"))
                .append(label("cmd.kineticcore.tps.current"))
                .append(formatMspt(currentMspt))
                .append(label("cmd.kineticcore.tps.report.separator"))
                .append(label("cmd.kineticcore.tps.10s_avg"))
                .append(formatMspt(average10Mspt))
                .append(label("cmd.kineticcore.tps.report.separator"))
                .append(label("cmd.kineticcore.tps.60s_avg"))
                .append(formatMspt(average60Mspt)).append("\n")
                .append(KineticI18n.translatable("cmd.kineticcore.tps.report.prefix.maximum"))
                .append(label("cmd.kineticcore.tps.10s_max"))
                .append(formatMspt(maximum10Mspt))
                .append(label("cmd.kineticcore.tps.report.separator"))
                .append(label("cmd.kineticcore.tps.60s_max"))
                .append(formatMspt(maximum60Mspt));

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static Component label(String key) {
        return KineticI18n.translatable(key);
    }

    private static Component formatTps(double tps) {
        String key = tps >= 18.0D
                ? "msg.kineticcore.metric.good"
                : tps >= 14.0D ? "msg.kineticcore.metric.warning" : "msg.kineticcore.metric.bad";
        return KineticI18n.translatable(key, Component.literal(String.format(Locale.ROOT, "%.2f", tps)));
    }

    private static Component formatMspt(double mspt) {
        String key = mspt <= 40.0D
                ? "msg.kineticcore.metric.good"
                : mspt <= 50.0D ? "msg.kineticcore.metric.warning" : "msg.kineticcore.metric.bad";
        return KineticI18n.translatable(key, Component.literal(String.format(Locale.ROOT, "%.2fms", mspt)));
    }
}
