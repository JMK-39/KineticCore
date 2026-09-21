package dev.xyat.kineticcore.feature.tps.logic;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.monitoring.KineticServerPerformance;
import dev.xyat.kineticcore.api.monitoring.ServerTickTracker;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.feature.tps.network.TpsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TpsHudManager {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticServerEvents.onTick(KineticEventPriority.NORMAL, KineticServerEvents.TickPhase.END, TpsHudManager::onServerTick),
                () -> KineticServerEvents.onPlayerLogout(KineticEventPriority.NORMAL, TpsHudManager::onPlayerLogout)
        );
    }

    private static final Set<UUID> SUBSCRIBERS = new HashSet<>();
    private static int tickCounter;

    private TpsHudManager() {
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        updateSubscription(player, enabled);
    }

    public static void onServerTick(MinecraftServer server) {
        if (SUBSCRIBERS.isEmpty()) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        ServerTickTracker tracker = KineticServerPerformance.tracker(server).orElse(null);
        if (tracker == null) return;
        double mspt = tracker.getStats(2, 0);
        TpsNetwork.TpsData packet = new TpsNetwork.TpsData(KineticServerPerformance.tps(mspt), mspt);

        for (UUID uuid : Set.copyOf(SUBSCRIBERS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                TpsNetwork.sendToPlayer(packet, player);
            }
        }
    }

    public static void onPlayerLogout(ServerPlayer player) {
        SUBSCRIBERS.remove(player.getUUID());
    }

    private static void updateSubscription(ServerPlayer player, boolean enabled) {
        if (enabled) {
            SUBSCRIBERS.add(player.getUUID());
        } else {
            SUBSCRIBERS.remove(player.getUUID());
        }
    }

}
