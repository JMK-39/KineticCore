package dev.xyat.kineticcore.feature.setspawn.network;

import dev.xyat.kineticcore.api.network.PacketChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.PacketRegistrations;
import dev.xyat.kineticcore.api.network.NetworkProtocolLimits;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticLog;
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
    private static final PacketChannel CHANNEL = PacketChannel.create(
                                                          KineticResourceIds.of("kineticcore", "setspawn"),
                                                          "1",
                                                          NetworkVersionPolicy.EXACT
                                                  );
    private static boolean networkRegistered;
    private static boolean openGuiRegistered;
    private static boolean savePacketRegistered;
    private static boolean saveResultRegistered;
    private static boolean requestOpenRegistered;

    private SetSpawnNetwork() {
    }

    public static synchronized void register() {
        PacketRegistrations.runIndependent(
        () -> {
            if (!openGuiRegistered) {
                CHANNEL.registerClientboundLazy(0,
                                OpenSetSpawnGuiPacket.class,
                        NetworkCodec.of(
                                (buffer, packet) -> {
                                    buffer.writeBoolean(packet.globalEnable());
                                    buffer.writeBoolean(packet.dimEnable());
                                    buffer.writeStringList(packet.dims(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                    buffer.writeBoolean(packet.biomeEnable());
                                    buffer.writeStringList(packet.biomes(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                    buffer.writeBoolean(packet.structEnable());
                                    buffer.writeStringList(packet.structs(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                    buffer.writeUtf(packet.playerDim(), MAX_STRING_LENGTH);
                                    buffer.writeUtf(packet.playerBiome(), MAX_STRING_LENGTH);
                                    buffer.writeUtf(packet.playerStruct(), MAX_STRING_LENGTH);
                                    buffer.writeStringList(packet.allDims(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                    buffer.writeStringList(packet.allBiomes(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                    buffer.writeStringList(packet.allStructs(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                },
                                buffer -> new OpenSetSpawnGuiPacket(
                                        buffer.readBoolean(),
                                        buffer.readBoolean(),
                                        buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                        buffer.readBoolean(),
                                        buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                        buffer.readBoolean(),
                                        buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                        buffer.readUtf(MAX_STRING_LENGTH),
                                        buffer.readUtf(MAX_STRING_LENGTH),
                                        buffer.readUtf(MAX_STRING_LENGTH),
                                        buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                        buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH),
                                        buffer.readStringList(MAX_LIST_ENTRIES, MAX_STRING_LENGTH)
                                )
                        ),
                        () -> packet -> SetSpawnNetworkClient.handleOpenGui(packet)
                );

                openGuiRegistered = true;
            }
        },
        () -> {
            if (!savePacketRegistered) {
                CHANNEL.registerServerbound(1,
                                SaveSetSpawnPacket.class,
                        NetworkCodec.of(
                                (buffer, packet) -> {
                                    buffer.writeBoolean(packet.globalEnable());
                                    buffer.writeBoolean(packet.dimEnable());
                                    buffer.writeStringList(packet.dims(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                    buffer.writeBoolean(packet.biomeEnable());
                                    buffer.writeStringList(packet.biomes(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
                                    buffer.writeBoolean(packet.structEnable());
                                    buffer.writeStringList(packet.structs(), MAX_LIST_ENTRIES, MAX_STRING_LENGTH);
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

                savePacketRegistered = true;
            }
        },
        () -> {
            if (!saveResultRegistered) {
                CHANNEL.registerClientboundLazy(2,
                                SaveSetSpawnResultPacket.class,
                        NetworkCodec.of(
                                (buffer, packet) -> buffer.writeBoolean(packet.success()),
                                buffer -> new SaveSetSpawnResultPacket(buffer.readBoolean())
                        ),
                        () -> packet -> SetSpawnNetworkClient.handleSaveResult(packet.success())
                );

                saveResultRegistered = true;
            }
        },
        () -> {
            if (!requestOpenRegistered) {
                CHANNEL.registerServerbound(3,
                                RequestOpenSetSpawnGuiPacket.class,
                        NetworkCodec.of((buffer, packet) -> { }, buffer -> new RequestOpenSetSpawnGuiPacket()),
                        (packet, context) -> {
                            ServerPlayer player = context.sender();
                            if (player.hasPermissions(2)) {
                                openEditorForPlayer(player);
                            }
                        }
                );

                requestOpenRegistered = true;
            }
        },
        () -> networkRegistered = openGuiRegistered && savePacketRegistered && saveResultRegistered && requestOpenRegistered
        );
    }

    public static void requestOpenEditor() {
        if (networkRegistered) {
            CHANNEL.sendToServer(new RequestOpenSetSpawnGuiPacket());
        }
    }

    public static void saveToServer(SaveSetSpawnPacket packet) {
        if (networkRegistered) {
            CHANNEL.sendToServer(packet);
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

        if (networkRegistered) {
            CHANNEL.sendToPlayer(
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
        // The editor displays the overworld. Accept it as an input choice, then
        // normalize it away as documented by the SetSpawn configuration.
        Set<String> selectableDimensions = new HashSet<>(allowedDimensions);
        allowedDimensions.remove("minecraft:overworld");
        Set<String> allowedBiomes = player.server.registryAccess().registryOrThrow(Registries.BIOME).keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
        Set<String> allowedStructures = player.server.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());

        if (containsDisallowed(packet.dims(), selectableDimensions)
                || containsDisallowed(packet.biomes(), allowedBiomes)
                || containsDisallowed(packet.structs(), allowedStructures)) {
            sendSaveResult(player, false);
            return;
        }

        // Complete all conversions before changing any live server setting.
        List<String> dimensions = sanitizeStrings(packet.dims(), allowedDimensions);
        List<String> biomes = sanitizeStrings(packet.biomes(), allowedBiomes);
        List<String> structures = sanitizeStrings(packet.structs(), allowedStructures);
        SpawnSettings previous = SpawnSettings.capture();
        boolean saved = false;
        try {
            SetSpawnConfig.enableCustomSpawn = packet.globalEnable();
            SetSpawnConfig.enableDimensions = packet.dimEnable();
            SetSpawnConfig.setspawnDimensions = dimensions;
            SetSpawnConfig.enableBiomes = packet.biomeEnable();
            SetSpawnConfig.setspawnBiomes = biomes;
            SetSpawnConfig.enableStructures = packet.structEnable();
            SetSpawnConfig.setspawnStructures = structures;
            SetSpawnConfig.save();
            saved = true;
        } catch (Throwable throwable) {
            previous.restore();
            try {
                SetSpawnConfig.save();
            } catch (Throwable rollbackFailure) {
                if (rollbackFailure != throwable) throwable.addSuppressed(rollbackFailure);
            }
            KineticLog.error("Failed to save SetSpawn config", throwable);
        }
        // Reporting a result is not part of the configuration transaction.
        sendSaveResult(player, saved);
    }

    /** Captures only the seven fields modified by this editor, not unrelated config. */
    private record SpawnSettings(
            boolean custom, boolean dimensionsEnabled, List<String> dimensions,
            boolean biomesEnabled, List<String> biomes,
            boolean structuresEnabled, List<String> structures
    ) {
        private static SpawnSettings capture() {
            return new SpawnSettings(SetSpawnConfig.enableCustomSpawn,
                    SetSpawnConfig.enableDimensions, new ArrayList<>(SetSpawnConfig.setspawnDimensions),
                    SetSpawnConfig.enableBiomes, new ArrayList<>(SetSpawnConfig.setspawnBiomes),
                    SetSpawnConfig.enableStructures, new ArrayList<>(SetSpawnConfig.setspawnStructures));
        }

        private void restore() {
            SetSpawnConfig.enableCustomSpawn = custom;
            SetSpawnConfig.enableDimensions = dimensionsEnabled;
            SetSpawnConfig.setspawnDimensions = new ArrayList<>(dimensions);
            SetSpawnConfig.enableBiomes = biomesEnabled;
            SetSpawnConfig.setspawnBiomes = new ArrayList<>(biomes);
            SetSpawnConfig.enableStructures = structuresEnabled;
            SetSpawnConfig.setspawnStructures = new ArrayList<>(structures);
        }
    }

    private static void sendSaveResult(ServerPlayer player, boolean success) {
        if (networkRegistered) {
            CHANNEL.sendToPlayer(player, new SaveSetSpawnResultPacket(success));
        }
    }

    private static boolean containsDisallowed(List<String> rawIds, Set<String> allowed) {
        if (rawIds == null) return true;
        for (String raw : rawIds) {
            if (raw == null) return true;
            String id = raw.trim();
            if (id.isEmpty() || !allowed.contains(id)) return true;
        }
        return false;
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
