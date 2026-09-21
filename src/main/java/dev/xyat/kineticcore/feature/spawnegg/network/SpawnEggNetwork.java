package dev.xyat.kineticcore.feature.spawnegg.network;


import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.PacketRegistrations;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import net.minecraft.server.level.ServerPlayer;

public final class SpawnEggNetwork {
    private static final String MODE_KEY = "DisableEggThrow";
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            KineticResourceIds.of(KineticRuntime.MOD_ID, "spawn_egg")
    , "1", NetworkVersionPolicy.EXACT);

    private static ServerboundSender<SetMode> setModeSender;
    private static ClientboundSender<SyncMode> syncModeSender;
    private static boolean eventRegistered;

    private SpawnEggNetwork() {
    }

    public static synchronized void register() {
        PacketRegistrations.runIndependent(
        () -> {
            if (setModeSender == null) {
                setModeSender = CHANNEL.registerServerbound(0,
                                SetMode.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.disabled()),
                                buffer -> new SetMode(buffer.readBoolean())
                        ),
                        (message, context) -> {
                            ServerPlayer player = context.sender();
                            player.getPersistentData().putBoolean(MODE_KEY, message.disabled());
                            sendModeToPlayer(player, message.disabled());
                        }
                );

            }
        },
        () -> {
            if (syncModeSender == null) {
                syncModeSender = CHANNEL.registerClientbound(1,
                                SyncMode.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.disabled()),
                                buffer -> new SyncMode(buffer.readBoolean())
                        ),
                        message -> SpawnEggNetworkClient.handleModeSync(message.disabled())
                );

            }
        },
        () -> {
            if (!eventRegistered) {
                KineticServerEvents.onPlayerLogin(KineticEventPriority.NORMAL, SpawnEggNetwork::onPlayerLoggedIn);
                eventRegistered = true;
            }
        }
        );
    }

    public static void sendModeToServer(boolean disabled) {
        if (setModeSender != null) {
            setModeSender.send(new SetMode(disabled));
        }
    }

    private static void sendModeToPlayer(ServerPlayer player, boolean disabled) {
        if (syncModeSender != null) {
            syncModeSender.send(player, new SyncMode(disabled));
        }
    }

    private static void onPlayerLoggedIn(ServerPlayer player) {
        sendModeToPlayer(player, player.getPersistentData().getBoolean(MODE_KEY));
    }

    public record SetMode(boolean disabled) {
    }

    public record SyncMode(boolean disabled) {
    }
}
