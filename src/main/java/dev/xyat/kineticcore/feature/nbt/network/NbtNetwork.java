package dev.xyat.kineticcore.feature.nbt.network;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@KTNetwork
public final class NbtNetwork {
    public static final byte TARGET_HAND = 0;
    public static final byte TARGET_ENTITY = 1;
    public static final byte TARGET_BLOCK_ENTITY = 2;
    public static final byte COMMAND_OPEN_HAND = 0;
    public static final byte COMMAND_OPEN_CROSSHAIR = 1;

    private static final String PROTOCOL_VERSION = "1";
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
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(KineticCore.MODID, "nbt"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .serverAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .simpleChannel();

    private NbtNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(OpenNbtEditorRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenNbtEditorRequestPacket::new)
                .encoder(OpenNbtEditorRequestPacket::toBytes)
                .consumerMainThread(OpenNbtEditorRequestPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenNbtEditorPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenNbtEditorPacket::new)
                .encoder(OpenNbtEditorPacket::toBytes)
                .consumerMainThread(OpenNbtEditorPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenNbtFromCommandPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenNbtFromCommandPacket::new)
                .encoder(OpenNbtFromCommandPacket::toBytes)
                .consumerMainThread(OpenNbtFromCommandPacket::handle)
                .add();

        CHANNEL.messageBuilder(SaveNbtPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SaveNbtPacket::new)
                .encoder(SaveNbtPacket::toBytes)
                .consumerMainThread(SaveNbtPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CNotifyPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CNotifyPacket::new)
                .encoder(S2CNotifyPacket::toBytes)
                .consumerMainThread(S2CNotifyPacket::handle)
                .add();

        MinecraftForge.EVENT_BUS.addListener(NbtNetwork::onPlayerLogout);
    }

    private static int id() {
        return packetId++;
    }

    public static void openFromCommand(ServerPlayer player, byte commandMode) {
        if (player == null) return;
        sendToPlayer(new OpenNbtFromCommandPacket(commandMode), player);
    }

    public static final class OpenNbtEditorRequestPacket {
        private final byte targetType;
        private final String targetId;
        private final ResourceLocation dimension;

        public OpenNbtEditorRequestPacket(byte targetType, String targetId, ResourceLocation dimension) {
            this.targetType = targetType;
            this.targetId = targetId;
            this.dimension = dimension;
        }

        private OpenNbtEditorRequestPacket(FriendlyByteBuf buf) {
            this.targetType = buf.readByte();
            this.targetId = buf.readUtf(MAX_TARGET_ID_LENGTH);
            this.dimension = buf.readResourceLocation();
        }

        private void toBytes(FriendlyByteBuf buf) {
            buf.writeByte(targetType);
            buf.writeUtf(targetId, MAX_TARGET_ID_LENGTH);
            buf.writeResourceLocation(dimension);
        }

        private boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> handleOpenRequest(context.getSender(), this));
            context.setPacketHandled(true);
            return true;
        }
    }

    public static final class OpenNbtEditorPacket {
        private final String nbt;

        private OpenNbtEditorPacket(String nbt) {
            this.nbt = nbt;
        }

        /** Compatibility constructor for callers compiled against the old command-backed editor. */
        @Deprecated
        public OpenNbtEditorPacket(String nbt, byte ignoredTargetType, String ignoredTargetId) {
            this(nbt);
        }

        private OpenNbtEditorPacket(FriendlyByteBuf buf) {
            this.nbt = buf.readUtf(MAX_NBT_LENGTH);
        }

        private void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(nbt, MAX_NBT_LENGTH);
        }

        private boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> NbtNetworkHandlerClient.handleOpenEditor(nbt)
            ));
            context.setPacketHandled(true);
            return true;
        }
    }

    public static final class OpenNbtFromCommandPacket {
        private final byte commandMode;

        private OpenNbtFromCommandPacket(byte commandMode) {
            this.commandMode = commandMode;
        }

        private OpenNbtFromCommandPacket(FriendlyByteBuf buf) {
            this.commandMode = buf.readByte();
        }

        private void toBytes(FriendlyByteBuf buf) {
            buf.writeByte(commandMode);
        }

        private boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> NbtNetworkHandlerClient.handleCommandOpen(commandMode)
            ));
            context.setPacketHandled(true);
            return true;
        }
    }

    public static final class SaveNbtPacket {
        private final String nbt;

        public SaveNbtPacket(String nbt) {
            this.nbt = nbt;
        }

        /** Compatibility constructor; the server-authoritative editor session owns the target. */
        @Deprecated
        public SaveNbtPacket(String nbt, byte ignoredTargetType, String ignoredTargetId) {
            this(nbt);
        }

        private SaveNbtPacket(FriendlyByteBuf buf) {
            this.nbt = buf.readUtf(MAX_NBT_LENGTH);
        }

        private void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(nbt, MAX_NBT_LENGTH);
        }

        private boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> handleSave(context.getSender(), this));
            context.setPacketHandled(true);
            return true;
        }
    }

    public static final class S2CNotifyPacket {
        private final String translationKey;

        private S2CNotifyPacket(String translationKey) {
            this.translationKey = translationKey;
        }

        private S2CNotifyPacket(FriendlyByteBuf buf) {
            this.translationKey = buf.readUtf();
        }

        private void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(translationKey);
        }

        private boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> NbtNetworkHandlerClient.handleNotify(translationKey)
            ));
            context.setPacketHandled(true);
            return true;
        }
    }

    private static void handleOpenRequest(ServerPlayer player, OpenNbtEditorRequestPacket request) {
        if (player == null) return;

        EDITOR_SESSIONS.remove(player.getUUID());
        if (!player.hasPermissions(2)) {
            sendNotify(player, PERMISSION_ERROR_KEY);
            return;
        }

        ServerLevel level = player.serverLevel();
        if (!level.dimension().location().equals(request.dimension)) {
            sendNotify(player, TARGET_UNAVAILABLE_ERROR_KEY);
            return;
        }

        try {
            switch (request.targetType) {
                case TARGET_HAND -> openHandEditor(player, level);
                case TARGET_ENTITY -> openEntityEditor(player, level, request.targetId);
                case TARGET_BLOCK_ENTITY -> openBlockEditor(player, level, request.targetId);
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
        sendToPlayer(new OpenNbtEditorPacket(nbt), player);
    }

    private static void handleSave(ServerPlayer player, SaveNbtPacket packet) {
        if (player == null) return;

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
            tag = packet.nbt.isBlank() ? new CompoundTag() : TagParser.parseTag(packet.nbt);
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

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EDITOR_SESSIONS.remove(event.getEntity().getUUID());
    }

    private static void sendNotify(ServerPlayer player, String key) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CNotifyPacket(key));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private record EditorSession(byte targetType, ResourceKey<Level> dimension, Object target) {
    }
}
