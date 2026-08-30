package dev.xyat.kineticcore.feature.tps.logic;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.tps.network.TpsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = KineticCore.MODID)
public final class TpsHudManager {
    private static final Set<UUID> SUBSCRIBERS = new HashSet<>();
    private static int tickCounter;

    private TpsHudManager() {
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        updateSubscription(player, enabled);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SUBSCRIBERS.isEmpty()) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        if (!(server instanceof ITpsServer tpsServer)) return;

        TpsTracker tracker = tpsServer.kineticcore$getTpsTracker();
        double mspt = tracker.getStats(2, 0);
        TpsNetwork.TpsData packet = new TpsNetwork.TpsData(TpsTracker.tps(mspt), mspt);

        for (UUID uuid : Set.copyOf(SUBSCRIBERS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                TpsNetwork.sendToPlayer(packet, player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SUBSCRIBERS.remove(event.getEntity().getUUID());
    }

    private static void updateSubscription(ServerPlayer player, boolean enabled) {
        if (enabled) {
            SUBSCRIBERS.add(player.getUUID());
        } else {
            SUBSCRIBERS.remove(player.getUUID());
        }
    }

}
