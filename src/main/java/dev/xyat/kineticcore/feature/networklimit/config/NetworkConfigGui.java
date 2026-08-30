package dev.xyat.kineticcore.feature.networklimit.config;

import dev.xyat.kineticcore.ConfigGui;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@KTClientModule
public final class NetworkConfigGui {
    public static final String PAGE_ID = "kineticcore:network";

    private NetworkConfigGui() {
    }

    public static void load() {
        ConfigGui.register(KTConfigPage.builder(PAGE_ID, Component.translatable("cfg.kineticcore.network"))
                .scope(KTConfigScope.LOCAL_INSTALLATION)
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(Component.translatable("cfg.kineticcore.network.apply_notice"))
                .intValue("packet_size", Component.translatable("cfg.kineticcore.network.packet_size"),
                        () -> NetworkConfig.packetSize, value -> NetworkConfig.packetSize = value,
                        67108864, 1048576, Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.network.packet_size.desc"))
                .intValue("decoder_size", Component.translatable("cfg.kineticcore.network.decoder_size"),
                        () -> NetworkConfig.decoderSize, value -> NetworkConfig.decoderSize = value,
                        67108864, 8388608, Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.network.decoder_size.desc"))
                .intValue("chunk_packet_data", Component.translatable("cfg.kineticcore.network.chunk_packet_data"),
                        () -> NetworkConfig.chunkPacketData, value -> NetworkConfig.chunkPacketData = value,
                        16777216, 2097152, Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.network.chunk_packet_data.desc"))
                .longValue("nbt_max_size", Component.translatable("cfg.kineticcore.network.nbt_max_size"),
                        () -> NetworkConfig.nbtMaxSize, value -> NetworkConfig.nbtMaxSize = value,
                        33554432L, 2097152L, 8589934592L,
                        Component.translatable("cfg.kineticcore.network.nbt_max_size.desc"))
                .intValue("string_size", Component.translatable("cfg.kineticcore.network.string_size"),
                        () -> NetworkConfig.stringSize, value -> NetworkConfig.stringSize = value,
                        262144, 32767, Integer.MAX_VALUE,
                        Component.translatable("cfg.kineticcore.network.string_size.desc"))
                .intValue("var_int", Component.translatable("cfg.kineticcore.network.var_int"),
                        () -> NetworkConfig.varInt, value -> NetworkConfig.varInt = value,
                        5, 5, 10, Component.translatable("cfg.kineticcore.network.var_int.desc"))
                .intValue("var_long", Component.translatable("cfg.kineticcore.network.var_long"),
                        () -> NetworkConfig.varLong, value -> NetworkConfig.varLong = value,
                        10, 10, 20, Component.translatable("cfg.kineticcore.network.var_long.desc"))
                .intValue("var_int21", Component.translatable("cfg.kineticcore.network.var_int21"),
                        () -> NetworkConfig.varInt21, value -> NetworkConfig.varInt21 = value,
                        8, 3, 16, Component.translatable("cfg.kineticcore.network.var_int21.desc"))
                .intValue("timeout", Component.translatable("cfg.kineticcore.network.timeout"),
                        () -> NetworkConfig.timeout, value -> NetworkConfig.timeout = value,
                        120, 30, 99999, Component.translatable("cfg.kineticcore.network.timeout.desc"))
                .onSave(NetworkConfig::save)
                .build());
    }

    public static Screen create(Screen parent) {
        return ConfigGui.create(parent, PAGE_ID);
    }
}
