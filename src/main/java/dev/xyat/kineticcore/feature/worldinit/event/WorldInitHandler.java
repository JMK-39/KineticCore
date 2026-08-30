package dev.xyat.kineticcore.feature.worldinit.event;

import net.minecraft.ChatFormatting;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.worldinit.config.WorldInitConfig;
import dev.xyat.kineticcore.feature.worldinit.data.WorldInitData;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = KineticCore.MODID)
public class WorldInitHandler {
    private static final List<Component> PENDING_ADMIN_MESSAGES = new ArrayList<>();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        PENDING_ADMIN_MESSAGES.clear();

        if (!WorldInitConfig.enableWorldInit) {
            return;
        }

        MinecraftServer server = event.getServer();
        WorldInitData data = WorldInitData.get(server.overworld());

        if (data.isCommandsExecuted()) {
            KineticCore.LOGGER.debug("World init commands already executed, skipping.");
            return;
        }

        CommandSourceStack source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        List<String> commands = normalizeCommands(WorldInitConfig.worldInitCommands);

        int successCount = 0;
        int failedCount = 0;

        KineticCore.LOGGER.info("kineticcore world init command count={}", commands.size());

        for (String command : commands) {
            try {
                executeCommand(server, source, command);
                successCount++;
                KineticCore.LOGGER.info("World init command executed: {}", command);
            } catch (Exception e) {
                failedCount++;
                String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                broadcastAdminFailure(server, command, reason);
                KineticCore.LOGGER.error("Failed to execute world init command: {}", command, e);
            }
        }

        data.setCommandsExecuted(true);
        KineticCore.LOGGER.info("kineticcore world init completed. success={}, failed={}", successCount, failedCount);

        if (failedCount > 0) {
            broadcastOrQueueAdmins(server, Component.translatable("msg.kineticcore.worldinit.completed_with_failures", Component.literal(String.valueOf(successCount)).withStyle(ChatFormatting.GREEN), Component.literal(String.valueOf(failedCount)).withStyle(ChatFormatting.RED)));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasPermissions(2)) {
            return;
        }
        if (PENDING_ADMIN_MESSAGES.isEmpty()) {
            return;
        }

        for (Component message : PENDING_ADMIN_MESSAGES) {
            player.sendSystemMessage(message);
        }
    }

    private static void executeCommand(MinecraftServer server, CommandSourceStack source, String command) throws Exception {
        ParseResults<CommandSourceStack> parseResults = server.getCommands().getDispatcher().parse(command, source);
        server.getCommands().getDispatcher().execute(parseResults);
    }

    private static List<String> normalizeCommands(List<String> rawCommands) {
        List<String> commands = new ArrayList<>();
        if (rawCommands == null) {
            return commands;
        }

        for (String raw : rawCommands) {
            if (raw == null) {
                continue;
            }

            String[] split = raw.replace("\r", "\n").split("\n");
            for (String part : split) {
                String command = part.trim();
                while (command.startsWith("/")) {
                    command = command.substring(1).trim();
                }
                if (command.isEmpty() || command.startsWith("#")) {
                    continue;
                }
                commands.add(command);
            }
        }

        return commands;
    }

    private static void broadcastAdminFailure(MinecraftServer server, String command, String reason) {
        broadcastOrQueueAdmins(server, Component.translatable("msg.kineticcore.worldinit.command_failed.admin", Component.literal(command).withStyle(ChatFormatting.GOLD), Component.literal(reason).withStyle(ChatFormatting.RED)));
    }

    private static void broadcastOrQueueAdmins(MinecraftServer server, Component message) {
        boolean sent = false;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(message);
                sent = true;
            }
        }

        if (!sent) {
            PENDING_ADMIN_MESSAGES.add(message);
        }
    }
}
