package dev.xyat.kineticcore.feature.setspawn.command;

import dev.xyat.kineticcore.api.text.KineticI18n;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.api.command.CommandText;
import dev.xyat.kineticcore.feature.setspawn.util.StructureUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class SetSpawnCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> world = Commands.literal("world");

        world.then(Commands.literal("structure").executes(ctx -> checkCurrentPosStructures(ctx.getSource())));
        world.then(Commands.literal("list-structures").requires(source -> source.hasPermission(2)).executes(ctx -> listAllStructures(ctx.getSource())));
        world.then(Commands.literal("help").executes(ctx -> sendHelp(ctx.getSource())));
        world.executes(ctx -> sendHelp(ctx.getSource()));

        root.then(world);
    }

    private static int sendHelp(CommandSourceStack source) {
        MutableComponent msg = CommandText.header("cmd.kineticcore.world.desc").append("\n");
        msg.append(CommandText.executable("/kt world structure", "cmd.kineticcore.world.structure.desc"));
        if (source.hasPermission(2)) {
            msg.append("\n").append(CommandText.executable("/kt world list-structures", "cmd.kineticcore.world.list_structures.desc"));
        }
        source.sendSuccess(() -> msg, false);
        return 1;
    }

    private static int checkCurrentPosStructures(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            List<String> structures = StructureUtils.getStructuresAt(player.serverLevel(), player.blockPosition());

            if (structures.isEmpty()) {
                source.sendSuccess(() -> KineticI18n.translatable("msg.kineticcore.structure.not_found"), false);
                return 1;
            }

            MutableComponent msg = KineticI18n.translatable("msg.kineticcore.structure.found_simple");
            for (String id : structures) {
                msg.append(KineticI18n.translatable("msg.kineticcore.structure.entry", Component.literal(id))
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, KineticI18n.translatable("msg.kineticcore.copy")))
                        ));
            }
            source.sendSuccess(() -> msg, false);
        } catch (Exception e) {
            source.sendFailure(KineticI18n.translatable("msg.kineticcore.structure.check_failed", Component.literal(String.valueOf(e.getMessage()))));
        }
        return 1;
    }

    private static int listAllStructures(CommandSourceStack source) {
        try {
            List<String> ids = source.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .collect(Collectors.toList());

            String allIdsStr = String.join("\n", ids);
            MutableComponent msg = KineticI18n.translatable("msg.kineticcore.list.structures", Component.literal(String.valueOf(ids.size())));
            if (!ids.isEmpty()) {
                msg.append(Component.literal("  "));
                msg.append(KineticI18n.translatable("msg.kineticcore.copy_all")
                        .withStyle(style -> style
                                .withBold(true)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, allIdsStr))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, KineticI18n.translatable("cmd.kineticcore.copy.too_long")))
                        ));
            }
            source.sendSuccess(() -> msg, false);
        } catch (Exception e) {
            source.sendFailure(KineticI18n.translatable("msg.kineticcore.structure.list_failed"));
        }
        return 1;
    }
}
