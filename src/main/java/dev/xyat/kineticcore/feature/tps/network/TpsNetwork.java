package dev.xyat.kineticcore.feature.tps.network;


import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.PacketRegistrations;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.feature.tps.client.TpsRenderer;
import dev.xyat.kineticcore.feature.tps.logic.TpsHudManager;
import net.minecraft.server.level.ServerPlayer;

public final class TpsNetwork {
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            KineticResourceIds.of(KineticRuntime.MOD_ID, "tps")
    , "1", NetworkVersionPolicy.EXACT);

    private static ClientboundSender<TpsData> tpsDataSender;
    private static ServerboundSender<SubscriptionData> subscriptionSender;

    private TpsNetwork() {
    }

    public static synchronized void register() {
        PacketRegistrations.runIndependent(
        () -> {
            if (tpsDataSender == null) {
                tpsDataSender = CHANNEL.registerClientbound(0,
                                TpsData.class,
                        NetworkCodec.of(
                                (buffer, message) -> {
                                    buffer.writeDouble(message.tps());
                                    buffer.writeDouble(message.mspt());
                                },
                                buffer -> new TpsData(buffer.readDouble(), buffer.readDouble())
                        ),
                        message -> TpsRenderer.updateData(message.tps(), message.mspt())
                );

            }
        },
        () -> {
            if (subscriptionSender == null) {
                subscriptionSender = CHANNEL.registerServerbound(1,
                                SubscriptionData.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.enabled()),
                                buffer -> new SubscriptionData(buffer.readBoolean())
                        ),
                        (message, context) -> TpsHudManager.setEnabled(context.sender(), message.enabled())
                );

            }
        }
        );
    }

    public static void sendToPlayer(TpsData message, ServerPlayer player) {
        if (tpsDataSender != null) {
            tpsDataSender.send(player, message);
        }
    }

    public static void sendSubscription(boolean enabled) {
        if (subscriptionSender != null) {
            subscriptionSender.send(new SubscriptionData(enabled));
        }
    }

    public record TpsData(double tps, double mspt) {
    }

    public record SubscriptionData(boolean enabled) {
    }
}
