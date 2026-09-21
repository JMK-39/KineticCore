package dev.xyat.kineticcore.feature.pvp.network;


import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import net.minecraft.server.level.ServerPlayer;

public final class PvpNetwork {
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            KineticResourceIds.of(KineticRuntime.MOD_ID, "pvp")
    , "1", NetworkVersionPolicy.EXACT);

    private static ClientboundSender<S2CPvpStatePacket> stateSender;

    private PvpNetwork() {
    }

    public static synchronized void register() {
        if (stateSender == null) {
            stateSender = CHANNEL.registerClientbound(0,
                            S2CPvpStatePacket.class,
                    NetworkCodec.of(
                            (buffer, message) -> buffer.writeBoolean(message.enabled()),
                            buffer -> new S2CPvpStatePacket(buffer.readBoolean())
                    ),
                    message -> PvpNetworkHandlerClient.handleState(message.enabled())
            );
        }
    }

    public static void sendState(ServerPlayer player, boolean enabled) {
        if (stateSender != null) {
            stateSender.send(player, new S2CPvpStatePacket(enabled));
        }
    }

    public record S2CPvpStatePacket(boolean enabled) {
    }
}
