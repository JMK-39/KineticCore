package dev.xyat.kineticcoreflightvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in check in a disposable installed-runtime save, using actual chat and commands. */
public final class FlightTutorialValidation {
    private static final Logger LOG = LoggerFactory.getLogger(FlightTutorialValidation.class);
    private static int tutorials, phase, waitingTicks;
    private static volatile long due;
    private static String closeCommand;
    private static volatile Throwable failure;
    private static volatile boolean commandComplete;
    private static volatile boolean modeChangeComplete;

    public static void install() {
        MinecraftForge.EVENT_BUS.addListener(FlightTutorialValidation::chat);
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, FlightTutorialValidation::tick);
    }

    private static void chat(ClientChatReceivedEvent event) {
        Component message = event.getMessage();
        if (message.getContents() instanceof TranslatableContents translated
                && (translated.getKey().equals("msg.kineticcore.flying.fine.tune")
                || translated.getKey().equals("msg.kineticcore.flying.fast.tune")
                || translated.getKey().equals("msg.kineticcore.flying.noclip.status"))) tutorials++;
        findClose(message);
    }

    private static void findClose(Component component) {
        var click = component.getStyle().getClickEvent();
        if (click != null) {
            try {
                String value;
                //? if >=26.1 {
                /*value = click instanceof net.minecraft.network.chat.ClickEvent.RunCommand command ? command.command() : "";
                *///?} else {
                value = click.getAction() == net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND ? click.getValue() : "";
                //?}
                if ("/kt flight tips off".equals(value)) closeCommand = value;
            } catch (Throwable exception) { failure = exception; }
        }
        component.getSiblings().forEach(FlightTutorialValidation::findClose);
    }

    private static void tick() {
        var client = Minecraft.getInstance();
        if (phase == 99) return;
        if (client.player == null || client.getSingleplayerServer() == null) {
            if (++waitingTicks % 200 == 0) LOG.info("FLIGHT_TUTORIAL_WAIT screen={}", client.screen == null ? "none" : client.screen.getClass().getSimpleName());
            return;
        }
        try {
            if (failure != null) throw new AssertionError("Server validation failed", failure);
            if (phase == 0) {
                phase = 1;
                due = System.currentTimeMillis() + 2000;
                runServer(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());
                    if (!Boolean.getBoolean("kineticcore.flightValidation.reload")) {
                        var persistentData = player.getPersistentData();
                        var forgeData = persistentData.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
                        forgeData.remove("kt_flight_tutorial_disabled");
                    }
                    player.setGameMode(GameType.SURVIVAL);
                    player.setGameMode(GameType.CREATIVE);
                    due = System.currentTimeMillis() + 2000;
                    modeChangeComplete = true;
                });
            } else if (phase == 1 && modeChangeComplete && System.currentTimeMillis() >= due) {
                if (Boolean.getBoolean("kineticcore.flightValidation.reload")) {
                    require(tutorials == 0, "tutorial must stay disabled after save/rejoin: " + tutorials);
                    finish(client, "reload");
                    return;
                }
                require(tutorials == 3, "all three original tutorial lines must arrive: " + tutorials);
                require(closeCommand != null, "creative flight tutorial has no clickable close action");
                phase = 2;
                due = System.currentTimeMillis() + 2000;
                runServer(() -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayer(client.player.getUUID());
                    //? if >=26.1 {
                    /*var source = player.createCommandSourceStack().withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.ALL);
                    *///?} else {
                    var source = player.createCommandSourceStack().withPermission(0);
                    //?}
                    require(server.getCommands().getDispatcher().execute(closeCommand.substring(1), source) == 1, "non-operator can dismiss their tutorial");
                    var otherProfile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "flight_other");
                    //? if >=1.21 {
                    /*var other = new net.minecraft.server.level.ServerPlayer(server, server.overworld(), otherProfile, net.minecraft.server.level.ClientInformation.createDefault());
                    var fresh = new net.minecraft.server.level.ServerPlayer(server, server.overworld(), player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
                    *///?} else {
                    var other = new net.minecraft.server.level.ServerPlayer(server, server.overworld(), otherProfile);
                    var fresh = new net.minecraft.server.level.ServerPlayer(server, server.overworld(), player.getGameProfile());
                    //?}
                    require(!dev.xyat.kineticcore.feature.flight.FlightState.tutorialDisabled(other), "another player must keep tutorials");
                    require(!dev.xyat.kineticcore.feature.flight.FlightState.tutorialDisabled(fresh), "same UUID without this save's player data must keep tutorials");
                    fresh.restoreFrom(player, false);
                    require(dev.xyat.kineticcore.feature.flight.FlightState.tutorialDisabled(fresh), "tutorial preference must survive the real respawn copy");
                    try {
                        server.getCommands().getDispatcher().execute("kt flight on", source);
                        throw new AssertionError("non-operator gained access to flight on");
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
                    player.setGameMode(GameType.SURVIVAL);
                    player.setGameMode(GameType.CREATIVE);
                    server.getPlayerList().saveAll();
                    due = System.currentTimeMillis() + 2000;
                    commandComplete = true;
                });
            } else if (phase == 2 && commandComplete && System.currentTimeMillis() >= due) {
                require(tutorials == 3, "dismissed tutorial must not be sent again: " + tutorials);
                finish(client, "dismiss");
            }
        } catch (Throwable exception) {
            phase = 99;
            LOG.error("FLIGHT_TUTORIAL_FAIL", exception);
            client.stop();
        }
    }

    private static void runServer(CheckedAction action) {
        Minecraft.getInstance().getSingleplayerServer().execute(() -> {
            try { action.run(); } catch (Throwable exception) { failure = exception; }
        });
    }

    private static void finish(Minecraft client, String caseName) {
        phase = 99;
        LOG.info("FLIGHT_TUTORIAL_PASS case={} tutorials={}", caseName, tutorials);
        client.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface CheckedAction { void run() throws Exception; }
}
