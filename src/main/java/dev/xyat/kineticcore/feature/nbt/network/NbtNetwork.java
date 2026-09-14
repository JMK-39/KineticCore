package dev.xyat.kineticcore.feature.nbt.network;

import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NbtNetwork {
    public static final byte TARGET_HAND = 0;
    public static final byte TARGET_ENTITY = 1;
    public static final byte TARGET_BLOCK_ENTITY = 2;
    public static final byte COMMAND_OPEN_HAND = 0;
    public static final byte COMMAND_OPEN_CROSSHAIR = 1;

    private static final int MAX_NBT_LENGTH = 32767;
    private static final int MAX_TARGET_ID_LENGTH = 128;
    private static final double MAX_TARGET_DISTANCE_SQUARED = 64.0D * 64.0D;

    private static final String PERMISSION_ERROR_KEY = "gui.kineticcore.nbt.error.permission";
    private static final String NO_ITEM_ERROR_KEY = "gui.kineticcore.nbt.error.no_item";
    private static final String TARGET_UNAVAILABLE_ERROR_KEY = "gui.kineticcore.nbt.error.target_unavailable";
    private static final String INVALID_NBT_ERROR_KEY = "gui.kineticcore.nbt.editor.invalid";
    private static final String HAND_SUCCESS_KEY = "gui.kineticcore.nbt.success.hand";
    private static final String ENTITY_SUCCESS_KEY = "gui.kineticcore.nbt.success.entity";
    private static final String BLOCK_SUCCESS_KEY = "gui.kineticcore.nbt.success.block";

    private static final Map<UUID, EditorSession> EDITOR_SESSIONS = new ConcurrentHashMap<>();
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            new ResourceLocation(KineticRuntime.MOD_ID, "nbt")
    );

    private static ServerboundSender<OpenNbtEditorRequestPacket> openRequestSender;
    private static ClientboundSender<OpenNbtEditorPacket> openEditorSender;
    private static ClientboundSender<OpenNbtFromCommandPacket> commandOpenSender;
    private static ServerboundSender<SaveNbtPacket> saveSender;
    private static ClientboundSender<S2CNotifyPacket> notifySender;
    private static boolean eventsRegistered;

    private NbtNetwork() {
    }

    public static void register() {
        openRequestSender = CHANNEL.registerServerbound(
                OpenNbtEditorRequestPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> {
                            buffer.writeByte(packet.targetType());
                            buffer.writeUtf(packet.targetId(), MAX_TARGET_ID_LENGTH);
                            buffer.writeResourceLocation(packet.dimension());
                        },
                        buffer -> new OpenNbtEditorRequestPacket(
                                buffer.readByte(),
                                buffer.readUtf(MAX_TARGET_ID_LENGTH),
                                buffer.readResourceLocation()
                        )
                ),
                (packet, context) -> handleOpenRequest(context.sender(), packet)
        );

        openEditorSender = CHANNEL.registerClientbound(
                OpenNbtEditorPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> buffer.writeUtf(packet.nbt(), MAX_NBT_LENGTH),
                        buffer -> new OpenNbtEditorPacket(buffer.readUtf(MAX_NBT_LENGTH))
                ),
                packet -> NbtNetworkHandlerClient.handleOpenEditor(packet.nbt())
        );

        commandOpenSender = CHANNEL.registerClientbound(
                OpenNbtFromCommandPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> buffer.writeByte(packet.commandMode()),
                        buffer -> new OpenNbtFromCommandPacket(buffer.readByte())
                ),
                packet -> NbtNetworkHandlerClient.handleCommandOpen(packet.commandMode())
        );

        saveSender = CHANNEL.registerServerbound(
                SaveNbtPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> buffer.writeUtf(packet.nbt(), MAX_NBT_LENGTH),
                        buffer -> new SaveNbtPacket(buffer.readUtf(MAX_NBT_LENGTH))
                ),
                (packet, context) -> handleSave(context.sender(), packet)
        );

        notifySender = CHANNEL.registerClientbound(
                S2CNotifyPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> buffer.writeUtf(packet.translationKey()),
                        buffer -> new S2CNotifyPacket(buffer.readUtf())
                ),
                packet -> NbtNetworkHandlerClient.handleNotify(packet.translationKey())
        );

        if (!eventsRegistered) {
            eventsRegistered = true;
            KineticServerEvents.onPlayerLogout(NbtNetwork::onPlayerLogout);
        }
    }

    public static void openFromCommand(ServerPlayer player, byte commandMode) {
        if (player != null && commandOpenSender != null) {
            commandOpenSender.send(player, new OpenNbtFromCommandPacket(commandMode));
        }
    }

    public static void sendToServer(OpenNbtEditorRequestPacket message) {
        if (openRequestSender != null) {
            openRequestSender.send(message);
        }
    }

    public static void sendToServer(SaveNbtPacket message) {
        if (saveSender != null) {
            saveSender.send(message);
        }
    }

    private static void handleOpenRequest(ServerPlayer player, OpenNbtEditorRequestPacket request) {
        EDITOR_SESSIONS.remove(player.getUUID());
        if (!player.hasPermissions(2)) {
            sendNotify(player, PERMISSION_ERROR_KEY);
            return;
        }

        ServerLevel level = player.serverLevel();
        if (!level.dimension().location().equals(request.dimension())) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        try {
            switch (request.targetType()) {
                case TARGET_HAND -> openHandEditor(player, level);
                case TARGET_ENTITY -> openEntityEditor(player, level, request.targetId());
                case TARGET_BLOCK_ENTITY -> openBlockEditor(player, level, request.targetId());
                default -> sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            }
        } catch (IllegalArgumentException exception) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
        }
    }

    private static void openHandEditor(ServerPlayer player, ServerLevel level) {
        ItemStack item = player.getMainHandItem();
        if (item.isEmpty()) {
            sendNotify(player, NO_ITEM_ERROR_KEY);
            return;
        }

        CompoundTag tag = item.getTag();
        openEditor(
                player,
                new EditorSession(TARGET_HAND, level.dimension(), item),
                tag == null ? "{}" : tag.toString()
        );
    }

    private static void openEntityEditor(ServerPlayer player, ServerLevel level, String targetId) {
        Entity target = level.getEntity(UUID.fromString(targetId));
        if (!isAvailableEntity(player, level, target)) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        CompoundTag tag = new CompoundTag();
        target.saveWithoutId(tag);
        openEditor(
                player,
                new EditorSession(TARGET_ENTITY, level.dimension(), target),
                tag.toString()
        );
    }

    private static void openBlockEditor(ServerPlayer player, ServerLevel level, String targetId) {
        BlockPos pos = BlockPos.of(Long.parseLong(targetId));
        BlockEntity target = findAvailableBlockEntity(player, level, pos);
        if (target == null) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        openEditor(
                player,
                new EditorSession(TARGET_BLOCK_ENTITY, level.dimension(), target),
                target.saveWithId().toString()
        );
    }

    private static void openEditor(ServerPlayer player, EditorSession session, String nbt) {
        EDITOR_SESSIONS.put(player.getUUID(), session);
        if (openEditorSender != null) {
            openEditorSender.send(player, new OpenNbtEditorPacket(nbt));
        }
    }

    private static void handleSave(ServerPlayer player, SaveNbtPacket packet) {
        EditorSession session = EDITOR_SESSIONS.remove(player.getUUID());
        if (!player.hasPermissions(2)) {
            sendNotify(player, PERMISSION_ERROR_KEY);
            return;
        }
        if (session == null || !player.serverLevel().dimension().equals(session.dimension())) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        CompoundTag tag;
        try {
            tag = packet.nbt().isBlank() ? new CompoundTag() : TagParser.parseTag(packet.nbt());
        } catch (Exception exception) {
            sendNotify(player, INVALID_NBT_ERROR_KEY);
            return;
        }

        try {
            switch (session.targetType()) {
                case TARGET_HAND -> saveHand(player, session, tag);
                case TARGET_ENTITY -> saveEntity(player, session, tag);
                case TARGET_BLOCK_ENTITY -> saveBlockEntity(player, session, tag);
                default -> sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            }
        } catch (Exception exception) {
            sendNotify(player, INVALID_NBT_ERROR_KEY);
        }
    }

    private static void saveHand(ServerPlayer player, EditorSession session, CompoundTag tag) {
        ItemStack item = player.getMainHandItem();
        if (item.isEmpty() || item != session.target()) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        item.setTag(tag.isEmpty() ? null : tag);
        sendNotify(player, HAND_SUCCESS_KEY);
    }

    private static void saveEntity(ServerPlayer player, EditorSession session, CompoundTag tag) {
        ServerLevel level = player.serverLevel();
        Entity target = session.target() instanceof Entity entity ? entity : null;
        if (!isAvailableEntity(player, level, target)
                || level.getEntity(target.getUUID()) != target) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        UUID oldUuid = target.getUUID();
        target.load(tag);
        target.setUUID(oldUuid);
        sendNotify(player, ENTITY_SUCCESS_KEY);
    }

    private static void saveBlockEntity(ServerPlayer player, EditorSession session, CompoundTag tag) {
        ServerLevel level = player.serverLevel();
        BlockEntity target = session.target() instanceof BlockEntity blockEntity ? blockEntity : null;
        if (target == null) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        BlockPos pos = target.getBlockPos();
        BlockEntity current = findAvailableBlockEntity(player, level, pos);
        if (current != target) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        target.load(tag);
        target.setChanged();
        level.sendBlockUpdated(pos, target.getBlockState(), target.getBlockState(), 3);
        sendNotify(player, BLOCK_SUCCESS_KEY);
    }

    private static boolean isAvailableEntity(ServerPlayer player, ServerLevel level, Entity target) {
        return target != null
                && !target.isRemoved()
                && target.level() == level
                && player.distanceToSqr(target) <= MAX_TARGET_DISTANCE_SQUARED;
    }

    private static BlockEntity findAvailableBlockEntity(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) > MAX_TARGET_DISTANCE_SQUARED || !level.hasChunkAt(pos)) {
            return null;
        }

        BlockEntity target = level.getBlockEntity(pos);
        return target == null || target.isRemoved() ? null : target;
    }

    private static void onPlayerLogout(ServerPlayer player) {
        EDITOR_SESSIONS.remove(player.getUUID());
    }

    private static void sendNotify(ServerPlayer player, String key) {
        if (notifySender != null) {
            notifySender.send(player, new S2CNotifyPacket(key));
        }
    }

    public record OpenNbtEditorRequestPacket(byte targetType, String targetId, ResourceLocation dimension) {
    }

    public record OpenNbtEditorPacket(String nbt) {
    }

    public record OpenNbtFromCommandPacket(byte commandMode) {
    }

    public record SaveNbtPacket(String nbt) {
    }

    public record S2CNotifyPacket(String translationKey) {
    }

    private record EditorSession(byte targetType, ResourceKey<Level> dimension, Object target) {
    }
}
