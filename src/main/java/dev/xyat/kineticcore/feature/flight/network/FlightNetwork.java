package dev.xyat.kineticcore.feature.flight.network;

import dev.xyat.kineticcore.api.flight.KineticFlight;
import dev.xyat.kineticcore.api.flight.KineticFlightClient;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import net.minecraft.server.level.ServerPlayer;

public final class FlightNetwork {
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(KineticRuntime.id("flight_channel"));

    private static ServerboundSender<PacketNoclip> noclipRequestSender;
    private static ClientboundSender<PacketNoclipState> noclipStateSender;

    private FlightNetwork() {
    }

    public static void register() {
        noclipRequestSender = CHANNEL.registerServerbound(
                PacketNoclip.class,
                NetworkCodec.of(
                        (buffer, message) -> buffer.writeBoolean(message.enabled()),
                        buffer -> new PacketNoclip(buffer.readBoolean())
                ),
                (message, context) -> KineticFlight.applyServerNoclip(context.sender(), message.enabled())
        );

        noclipStateSender = CHANNEL.registerClientbound(
                PacketNoclipState.class,
                NetworkCodec.of(
                        (buffer, message) -> buffer.writeBoolean(message.enabled()),
                        buffer -> new PacketNoclipState(buffer.readBoolean())
                ),
                message -> KineticFlightClient.applyServerNoclip(message.enabled())
        );

        KineticFlight.installNoclipSyncSender((player, enabled) -> {
            if (noclipStateSender != null) {
                noclipStateSender.send(player, new PacketNoclipState(enabled));
            }
        });
    }

    public static void requestNoclip(boolean enabled) {
        if (noclipRequestSender != null) {
            noclipRequestSender.send(new PacketNoclip(enabled));
        }
    }

    public static void applyServerNoclip(ServerPlayer player, boolean requestedState) {
        KineticFlight.applyServerNoclip(player, requestedState);
    }

    public static void syncNoclipState(ServerPlayer player) {
        KineticFlight.syncServerNoclip(player);
    }

    public record PacketNoclip(boolean enabled) {
    }

    public record PacketNoclipState(boolean enabled) {
    }
}
