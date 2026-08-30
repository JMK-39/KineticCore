package dev.xyat.kineticcore.feature.flight.network;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
import dev.xyat.kineticcore.feature.flight.client.FlightClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

@KTNetwork
public final class FlightNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final String NOCLIP_KEY = "kt_noclip";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KineticCore.MODID, "flight_channel"),
            () -> PROTOCOL_VERSION,
            KTNetworkProtocol::acceptsAnyVersion,
            KTNetworkProtocol::acceptsAnyVersion
    );

    private FlightNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                PacketNoclip.class,
                PacketNoclip::encode,
                PacketNoclip::decode,
                PacketNoclip::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                1,
                PacketNoclipState.class,
                PacketNoclipState::encode,
                PacketNoclipState::decode,
                PacketNoclipState::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void applyServerNoclip(ServerPlayer player, boolean requestedState) {
        boolean enabled = requestedState && player.isCreative();
        player.getPersistentData().putBoolean(NOCLIP_KEY, enabled);
        player.noPhysics = enabled;
        player.refreshDimensions();
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketNoclipState(enabled)
        );
    }

    public static void syncNoclipState(ServerPlayer player) {
        applyServerNoclip(player, player.getPersistentData().getBoolean(NOCLIP_KEY));
    }

    public record PacketNoclip(boolean enabled) {
        public static void encode(PacketNoclip packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.enabled);
        }

        public static PacketNoclip decode(FriendlyByteBuf buffer) {
            return new PacketNoclip(buffer.readBoolean());
        }

        public static void handle(PacketNoclip packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) applyServerNoclip(player, packet.enabled);
            });
            context.setPacketHandled(true);
        }
    }

    public record PacketNoclipState(boolean enabled) {
        public static void encode(PacketNoclipState packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.enabled);
        }

        public static PacketNoclipState decode(FriendlyByteBuf buffer) {
            return new PacketNoclipState(buffer.readBoolean());
        }

        public static void handle(PacketNoclipState packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> FlightClient.applyServerNoclip(packet.enabled)
            ));
            context.setPacketHandled(true);
        }
    }
}
