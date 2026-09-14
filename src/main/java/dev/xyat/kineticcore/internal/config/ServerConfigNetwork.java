package dev.xyat.kineticcore.internal.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticCompression;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.api.config.server.KTServerConfigSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServerConfigNetwork {
    private static final int MAX_COMPRESSED_BYTES = 2 * 1024 * 1024;
    private static final int MAX_DECOMPRESSED_BYTES = 8 * 1024 * 1024;
    private static final Gson GSON = new Gson();
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            new ResourceLocation(KineticRuntime.MOD_ID, "server_config")
    );

    private static ServerboundSender<RequestPacket> requestSender;
    private static ServerboundSender<SavePacket> saveSender;
    private static ClientboundSender<SyncPacket> syncSender;

    private ServerConfigNetwork() {
    }

    public static void register() {
        requestSender = CHANNEL.registerServerbound(
                RequestPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> buffer.writeUtf(packet.pageId(), 256),
                        buffer -> new RequestPacket(buffer.readUtf(256))
                ),
                (packet, context) -> sendSnapshot(context.sender(), packet.pageId(), false, true, "")
        );

        saveSender = CHANNEL.registerServerbound(
                SavePacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> {
                            buffer.writeUtf(packet.pageId(), 256);
                            buffer.writeByteArray(packet.payload());
                        },
                        buffer -> new SavePacket(buffer.readUtf(256), buffer.readByteArray(MAX_COMPRESSED_BYTES))
                ),
                (packet, context) -> handleSave(context.sender(), packet)
        );

        syncSender = CHANNEL.registerClientbound(
                SyncPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> {
                            buffer.writeUtf(packet.pageId(), 256);
                            buffer.writeBoolean(packet.editable());
                            buffer.writeBoolean(packet.saveResponse());
                            buffer.writeBoolean(packet.success());
                            buffer.writeUtf(packet.messageKey() == null ? "" : packet.messageKey(), 256);
                            buffer.writeByteArray(packet.payload());
                        },
                        buffer -> new SyncPacket(
                                buffer.readUtf(256),
                                buffer.readBoolean(),
                                buffer.readBoolean(),
                                buffer.readBoolean(),
                                buffer.readUtf(256),
                                buffer.readByteArray(MAX_COMPRESSED_BYTES)
                        )
                ),
                packet -> ServerConfigClientDispatch.handleSync(
                        packet.pageId(),
                        packet.editable(),
                        packet.saveResponse(),
                        packet.success(),
                        packet.messageKey(),
                        packet.payload()
                )
        );
    }

    public static void requestPage(String pageId) {
        if (requestSender != null) {
            requestSender.send(new RequestPacket(pageId));
        }
    }

    public static void savePage(String pageId, byte[] payload) {
        if (saveSender != null) {
            saveSender.send(new SavePacket(pageId, payload));
        }
    }

    public static byte[] encodeValues(Map<String, Object> values) {
        String json = GSON.toJson(values == null ? Map.of() : values);
        byte[] compressed = KineticCompression.compressUtf8(json);
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
        String json = KineticCompression.decompressUtf8(compressed, MAX_DECOMPRESSED_BYTES);
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Server config payload root must be an object");
        }
        return decodeObject(root.getAsJsonObject());
    }

    private static Map<String, Object> decodeObject(JsonObject object) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            result.put(entry.getKey(), decodeElement(entry.getValue()));
        }
        return result;
    }

    private static List<Object> decodeArray(JsonArray array) {
        List<Object> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            result.add(decodeElement(element));
        }
        return result;
    }

    private static Object decodeElement(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) return decodeObject(element.getAsJsonObject());
        if (element.isJsonArray()) return decodeArray(element.getAsJsonArray());

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isString()) return primitive.getAsString();
        if (!primitive.isNumber()) {
            throw new IllegalArgumentException("Unsupported server config JSON value");
        }

        String number = primitive.getAsString();
        if (number.indexOf('.') >= 0 || number.indexOf('e') >= 0 || number.indexOf('E') >= 0) {
            double value = Double.parseDouble(number);
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Non-finite server config number");
            }
            return value;
        }
        return Long.parseLong(number);
    }

    private static void handleSave(ServerPlayer player, SavePacket packet) {
        if (!player.hasPermissions(2)) {
            sendSnapshot(player, packet.pageId(), true, false, "gui.kineticcore.config.server.op_required");
            return;
        }

        KTServerConfigSpec spec = KTServerConfigApi.find(packet.pageId()).orElse(null);
        if (spec == null) {
            sendSnapshot(player, packet.pageId(), true, false, "gui.kineticcore.config.server.unmanaged");
            return;
        }

        try {
            Map<String, Object> values = decodeValues(packet.payload());
            spec.applyAndSave(player.server, values);
            sendSnapshot(player, packet.pageId(), true, true, "gui.kineticcore.config.server.saved");
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("Failed to save server config page {}", packet.pageId(), throwable);
            sendSnapshot(player, packet.pageId(), true, false, "gui.kineticcore.config.server.save_failed");
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
                KineticRuntime.logger().error("Failed to create server config snapshot {}", pageId, throwable);
                actualSuccess = false;
                actualMessageKey = "gui.kineticcore.config.server.load_failed";
                payload = new byte[0];
            }
        }

        if (syncSender != null) {
            syncSender.send(
                    player,
                    new SyncPacket(pageId, editable, saveResponse, actualSuccess, actualMessageKey, payload)
            );
        }
    }

    private record RequestPacket(String pageId) {
    }

    private record SavePacket(String pageId, byte[] payload) {
    }

    private record SyncPacket(
            String pageId,
            boolean editable,
            boolean saveResponse,
            boolean success,
            String messageKey,
            byte[] payload
    ) {
    }
}
