package dev.xyat.kineticcore.feature.setspawn.network;

import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.NetworkProtocolLimits;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.feature.setspawn.config.SetSpawnConfig;
import dev.xyat.kineticcore.feature.setspawn.util.StructureUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class SetSpawnNetwork {
    private static final int MAX_LIST_ENTRIES = NetworkProtocolLimits.DEFAULT.maxCollectionEntries();
    private static final int MAX_STRING_LENGTH = NetworkProtocolLimits.DEFAULT.maxUtfChars();
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            new ResourceLocation(KineticRuntime.MOD_ID, "setspawn")
    );

    private static ClientboundSender<OpenSetSpawnGuiPacket> openEditorSender;
    private static ServerboundSender<SaveSetSpawnPacket> saveSender;
    private static ClientboundSender<SaveSetSpawnResultPacket> saveResultSender;
    private static ServerboundSender<RequestOpenSetSpawnGuiPacket> openRequestSender;

    private SetSpawnNetwork() {
    }

    public static void register() {
        openEditorSender = CHANNEL.registerClientbound(
                OpenSetSpawnGuiPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> {
                            buffer.writeBoolean(packet.globalEnable());
                            buffer.writeBoolean(packet.dimEnable());
                            buffer.writeStringList(packet.dims());
                            buffer.writeBoolean(packet.biomeEnable());
                            buffer.writeStringList(packet.biomes());
                            buffer.writeBoolean(packet.structEnable());
                            buffer.writeStringList(packet.structs());
                            buffer.writeUtf(packet.playerDim());
                            buffer.writeUtf(packet.playerBiome());
                            buffer.writeUtf(packet.playerStruct());
                            buffer.writeStringList(packet.allDims());
                            buffer.writeStringList(packet.allBiomes());
                            buffer.writeStringList(packet.allStructs());
                        },
                        buffer -> new OpenSetSpawnGuiPacket(
                                buffer.readBoolean(),
                                buffer.readBoolean(),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                buffer.readBoolean(),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                buffer.readBoolean(),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                buffer.readUtf(),
                                buffer.readUtf(),
                                buffer.readUtf(),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH)
                        )
                ),
                SetSpawnNetworkClient::handleOpenGui
        );

        saveSender = CHANNEL.registerServerbound(
                SaveSetSpawnPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> {
                            buffer.writeBoolean(packet.globalEnable());
                            buffer.writeBoolean(packet.dimEnable());
                            buffer.writeStringList(packet.dims());
                            buffer.writeBoolean(packet.biomeEnable());
                            buffer.writeStringList(packet.biomes());
                            buffer.writeBoolean(packet.structEnable());
                            buffer.writeStringList(packet.structs());
                        },
                        buffer -> new SaveSetSpawnPacket(
                                buffer.readBoolean(),
                                buffer.readBoolean(),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                buffer.readBoolean(),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                buffer.readBoolean(),
                                buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH)
                        )
                ),
                (packet, context) -> handleSave(context.sender(), packet)
        );

        saveResultSender = CHANNEL.registerClientbound(
                SaveSetSpawnResultPacket.class,
                NetworkCodec.of(
                        (buffer, packet) -> buffer.writeBoolean(packet.success()),
                        buffer -> new SaveSetSpawnResultPacket(buffer.readBoolean())
                ),
                packet -> SetSpawnNetworkClient.handleSaveResult(packet.success())
        );

        openRequestSender = CHANNEL.registerServerbound(
                RequestOpenSetSpawnGuiPacket.class,
                NetworkCodec.of((buffer, packet) -> { }, buffer -> new RequestOpenSetSpawnGuiPacket()),
                (packet, context) -> {
                    ServerPlayer player = context.sender();
                    if (player.hasPermissions(2)) {
                        openEditorForPlayer(player);
                    }
                }
        );
    }

    public static void requestOpenEditor() {
        if (openRequestSender != null) {
            openRequestSender.send(new RequestOpenSetSpawnGuiPacket());
        }
    }

    public static void saveToServer(SaveSetSpawnPacket packet) {
        if (saveSender != null) {
            saveSender.send(packet);
        }
    }

    public static void openEditorForPlayer(ServerPlayer player) {
        if (player == null || !player.hasPermissions(2)) return;
        SetSpawnConfig.load();
        MinecraftServer server = player.server;
        String dim = player.level().dimension().location().toString();
        String biome = player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
        List<String> structures = StructureUtils.getStructuresAt(player.serverLevel(), player.blockPosition());
        String structure = structures.isEmpty() ? "none" : structures.get(0);

        List<String> allDims = server.levelKeys().stream()
                .map(key -> key.location().toString())
                .sorted()
                .collect(Collectors.toList());
        List<String> allBiomes = server.registryAccess().registryOrThrow(Registries.BIOME).keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.toList());
        List<String> allStructs = server.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.toList());

        if (openEditorSender != null) {
            openEditorSender.send(
                    player,
                    new OpenSetSpawnGuiPacket(
                            SetSpawnConfig.enableCustomSpawn,
                            SetSpawnConfig.enableDimensions,
                            SetSpawnConfig.setspawnDimensions,
                            SetSpawnConfig.enableBiomes,
                            SetSpawnConfig.setspawnBiomes,
                            SetSpawnConfig.enableStructures,
                            SetSpawnConfig.setspawnStructures,
                            dim,
                            biome,
                            structure,
                            allDims,
                            allBiomes,
                            allStructs
                    )
            );
        }
    }

    private static void handleSave(ServerPlayer player, SaveSetSpawnPacket packet) {
        if (!player.hasPermissions(2)) {
            sendSaveResult(player, false);
            return;
        }

        Set<String> allowedDimensions = player.server.levelKeys().stream()
                .map(key -> key.location().toString())
                .collect(Collectors.toSet());
        allowedDimensions.remove("minecraft:overworld");
        Set<String> allowedBiomes = player.server.registryAccess().registryOrThrow(Registries.BIOME).keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
        Set<String> allowedStructures = player.server.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());

        if (!containsOnlyAllowed(packet.dims(), allowedDimensions)
                || !containsOnlyAllowed(packet.biomes(), allowedBiomes)
                || !containsOnlyAllowed(packet.structs(), allowedStructures)) {
            sendSaveResult(player, false);
            return;
        }

        try {
            SetSpawnConfig.enableCustomSpawn = packet.globalEnable();
            SetSpawnConfig.enableDimensions = packet.dimEnable();
            SetSpawnConfig.setspawnDimensions = sanitizeStrings(packet.dims(), allowedDimensions);
            SetSpawnConfig.enableBiomes = packet.biomeEnable();
            SetSpawnConfig.setspawnBiomes = sanitizeStrings(packet.biomes(), allowedBiomes);
            SetSpawnConfig.enableStructures = packet.structEnable();
            SetSpawnConfig.setspawnStructures = sanitizeStrings(packet.structs(), allowedStructures);
            SetSpawnConfig.save();
            sendSaveResult(player, true);
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("Failed to save SetSpawn config", throwable);
            sendSaveResult(player, false);
        }
    }

    private static void sendSaveResult(ServerPlayer player, boolean success) {
        if (saveResultSender != null) {
            saveResultSender.send(player, new SaveSetSpawnResultPacket(success));
        }
    }

    private static boolean containsOnlyAllowed(List<String> rawIds, Set<String> allowed) {
        if (rawIds == null) return false;
        for (String raw : rawIds) {
            if (raw == null) return false;
            String id = raw.trim();
            if (id.isEmpty() || !allowed.contains(id)) return false;
        }
        return true;
    }

    private static List<String> sanitizeStrings(List<String> rawIds, Set<String> allowed) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : rawIds) {
            String id = raw.trim();
            if (allowed.contains(id) && seen.add(id)) {
                result.add(id);
            }
        }
        return result;
    }

    public record RequestOpenSetSpawnGuiPacket() {
    }

    public record OpenSetSpawnGuiPacket(
            boolean globalEnable,
            boolean dimEnable,
            List<String> dims,
            boolean biomeEnable,
            List<String> biomes,
            boolean structEnable,
            List<String> structs,
            String playerDim,
            String playerBiome,
            String playerStruct,
            List<String> allDims,
            List<String> allBiomes,
            List<String> allStructs
    ) {
    }

    public record SaveSetSpawnResultPacket(boolean success) {
    }

    public record SaveSetSpawnPacket(
            boolean globalEnable,
            boolean dimEnable,
            List<String> dims,
            boolean biomeEnable,
            List<String> biomes,
            boolean structEnable,
            List<String> structs
    ) {
    }
}
