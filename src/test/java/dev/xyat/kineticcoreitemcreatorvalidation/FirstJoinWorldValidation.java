package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.inventory.KineticItemText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** A fresh first-join reward must save in two different worlds opened by the same client process. */
public final class FirstJoinWorldValidation {
    private static final String FIRST_WORLD = "first-join-alpha";
    private static final String SECOND_WORLD = "first-join-beta";

    private static int stage;
    private static CompletableFuture<Boolean> saving;
    private static MinecraftServer firstServer;

    private FirstJoinWorldValidation() {
    }

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, FirstJoinWorldValidation::tick);
    }

    private static void tick() {
        if (stage == 3) return;
        Minecraft client = Minecraft.getInstance();
        try {
            if (stage == 1) {
                if (client.getSingleplayerServer() != null || client.level != null || client.getOverlay() != null) return;
                stage = 2;
                //? if >=1.21 {
                /*client.createWorldOpenFlows().openWorld(SECOND_WORLD, () -> {});
                *///?} else {
                client.createWorldOpenFlows().loadLevel(new TitleScreen(), SECOND_WORLD);
                //?}
                return;
            }
            var server = client.getSingleplayerServer();
            if (server == null || client.player == null) return;
            if (saving == null) {
                if (stage == 0) firstServer = server;
                else if (server == firstServer) throw new AssertionError("Second world reused the original server");
                String world = stage == 0 ? FIRST_WORLD : SECOND_WORLD;
                var profile = client.player.getUUID();
                saving = server.submit(() -> saveReward(server, profile, world));
                return;
            }
            if (!saving.isDone()) return;
            boolean granted = saving.join();
            saving = null;
            if (!granted) return;

            if (stage == 0) {
                stage = 1;
                //? if >=26.1 {
                /*client.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
                *///?} else if >=1.21 {
                /*client.level.disconnect();
                client.disconnect(new TitleScreen());
                *///?} else {
                client.level.disconnect();
                client.clearLevel(new TitleScreen());
                //?}
            } else {
                stage = 3;
                System.out.println("FIRST_JOIN_WORLD_VALIDATION_PASS saved starting enchantments in both worlds");
                client.stop();
            }
        } catch (Throwable error) {
            stage = 3;
            System.err.println("FIRST_JOIN_WORLD_VALIDATION_FAIL");
            error.printStackTrace();
            client.stop();
        }
    }

    private static boolean saveReward(MinecraftServer server, UUID profile, String world) {
        if (!world.equals(server.getWorldData().getLevelName())) {
            throw new AssertionError("Unexpected world: " + server.getWorldData().getLevelName());
        }
        var player = server.getPlayerList().getPlayer(profile);
        if (player == null) return false;
        var chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(Items.LEATHER_CHESTPLATE)) return false;
        server.getPlayerList().saveAll();
        String encoded = KineticItemText.format(chestplate);
        if (!encoded.contains("minecraft:unbreaking") || !encoded.contains("minecraft:protection")) {
            throw new AssertionError("Starting enchantments did not serialize in " + world);
        }
        System.out.println("FIRST_JOIN_WORLD_VALIDATION_SAVED " + world);
        return true;
    }
}
