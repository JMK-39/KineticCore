package dev.xyat.kineticcore.config.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.api.NetworkCompressUtil;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
import dev.xyat.kineticcore.config.client.KTServerConfigClient;
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

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@KTNetwork
public final class KTServerConfigNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final int MAX_COMPRESSED_BYTES = 2 * 1024 * 1024;
    private static final int MAX_DECOMPRESSED_BYTES = 8 * 1024 * 1024;
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>() { }.getType();

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KineticCore.MODID, "server_config"),
            () -> PROTOCOL_VERSION,
            KTNetworkProtocol::acceptsAnyVersion,
            KTNetworkProtocol::acceptsAnyVersion
    );

    private KTServerConfigNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                RequestPacket.class,
                RequestPacket::encode,
                RequestPacket::decode,
                RequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                1,
                SavePacket.class,
                SavePacket::encode,
                SavePacket::decode,
                SavePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                2,
                SyncPacket.class,
                SyncPacket::encode,
                SyncPacket::decode,
                SyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static byte[] encodeValues(Map<String, Object> values) {
        String json = GSON.toJson(values == null ? Map.of() : values);
        byte[] compressed = NetworkCompressUtil.compress(json);
        if (compressed.length > MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException("Compressed server config payload exceeds limit");
        }
        return compressed;
    }

    public static Map<String, Object> decodeValues(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return new LinkedHashMap<>();
        if (compressed.length > MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException("Compressed server config payload exceeds limit");
        }
        String json = new String(
                NetworkCompressUtil.decompressBytes(compressed, MAX_DECOMPRESSED_BYTES),
                StandardCharsets.UTF_8
        );
        Map<String, Object> values = GSON.fromJson(json, MAP_TYPE);
        return values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }

    public record RequestPacket(String pageId) {
        static void encode(RequestPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.pageId, 256);
        }

        static RequestPacket decode(FriendlyByteBuf buffer) {
            return new RequestPacket(buffer.readUtf(256));
        }

        static void handle(RequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                sendSnapshot(player, packet.pageId, false, true, "");
            });
            context.setPacketHandled(true);
        }
    }

    public record SavePacket(String pageId, byte[] payload) {
        static void encode(SavePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.pageId, 256);
            buffer.writeByteArray(packet.payload);
        }

        static SavePacket decode(FriendlyByteBuf buffer) {
            return new SavePacket(buffer.readUtf(256), buffer.readByteArray(MAX_COMPRESSED_BYTES));
        }

        static void handle(SavePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!player.hasPermissions(2)) {
                    sendSnapshot(player, packet.pageId, true, false, "gui.kineticcore.config.server.op_required");
                    return;
                }

                KTServerConfigSpec spec = KTServerConfigApi.find(packet.pageId).orElse(null);
                if (spec == null) {
                    sendSnapshot(player, packet.pageId, true, false, "gui.kineticcore.config.server.unmanaged");
                    return;
                }

                try {
                    Map<String, Object> values = decodeValues(packet.payload);
                    spec.applyAndSave(player.server, values);
                    sendSnapshot(player, packet.pageId, true, true, "gui.kineticcore.config.server.saved");
                } catch (Throwable throwable) {
                    KineticCore.LOGGER.error("Failed to save server config page {}", packet.pageId, throwable);
                    sendSnapshot(player, packet.pageId, true, false, "gui.kineticcore.config.server.save_failed");
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record SyncPacket(
            String pageId,
            boolean editable,
            boolean saveResponse,
            boolean success,
            String messageKey,
            byte[] payload
    ) {
        static void encode(SyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.pageId, 256);
            buffer.writeBoolean(packet.editable);
            buffer.writeBoolean(packet.saveResponse);
            buffer.writeBoolean(packet.success);
            buffer.writeUtf(packet.messageKey == null ? "" : packet.messageKey, 256);
            buffer.writeByteArray(packet.payload);
        }

        static SyncPacket decode(FriendlyByteBuf buffer) {
            return new SyncPacket(
                    buffer.readUtf(256),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readUtf(256),
                    buffer.readByteArray(MAX_COMPRESSED_BYTES)
            );
        }

        static void handle(SyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> KTServerConfigClient.handleSync(
                            packet.pageId,
                            packet.editable,
                            packet.saveResponse,
                            packet.success,
                            packet.messageKey,
                            packet.payload
                    )
            ));
            context.setPacketHandled(true);
        }
    }

    private static void sendSnapshot(
            ServerPlayer player,
            String pageId,
            boolean saveResponse,
            boolean success,
            String messageKey
    ) {
        KTServerConfigSpec spec = KTServerConfigApi.find(pageId).orElse(null);
        boolean editable = spec != null && player.hasPermissions(2);
        byte[] payload = new byte[0];
        boolean actualSuccess = success;
        String actualMessageKey = messageKey == null ? "" : messageKey;

        if (spec == null) {
            actualSuccess = false;
            if (actualMessageKey.isEmpty()) actualMessageKey = "gui.kineticcore.config.server.unmanaged";
        } else {
            try {
                payload = encodeValues(spec.snapshot());
            } catch (Throwable throwable) {
                KineticCore.LOGGER.error("Failed to create server config snapshot {}", pageId, throwable);
                actualSuccess = false;
                actualMessageKey = "gui.kineticcore.config.server.load_failed";
                payload = new byte[0];
            }
        }

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPacket(pageId, editable, saveResponse, actualSuccess, actualMessageKey, payload)
        );
    }
}
