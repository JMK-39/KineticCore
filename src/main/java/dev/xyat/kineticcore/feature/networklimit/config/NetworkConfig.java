package dev.xyat.kineticcore.feature.networklimit.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@KTModule
public class NetworkConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/network.toml");
    private static CommentedFileConfig configData;

    public static int timeout = 120;
    public static int packetSize = 67108864;
    public static int decoderSize = 67108864;
    public static int chunkPacketData = 16777216;
    public static long nbtMaxSize = 33554432L;
    public static int stringSize = 262144;
    public static int varInt = 5;
    public static int varLong = 10;
    public static int varInt21 = 8;

    public static void load() {
        try {
            configData = CommentedFileConfig.builder(CONFIG_PATH).sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build();
            configData.load();
            setupConfig();
            configData.save();
            readValues();

            System.setProperty("forge.disablePacketCompressionDebug", "true");
            System.setProperty("forge.readTimeout", String.valueOf(timeout));
        } catch (Exception e) {
            KineticCore.LOGGER.error("NetworkConfig Load Failed", e);
        }
    }

    private static void setIfAbsentAndComment(String path, Object defaultVal, String comment) {
        if (!configData.contains(path)) {
            configData.set(path, defaultVal);
        }
        configData.setComment(path, comment);
    }

    private static void setupConfig() {
        setIfAbsentAndComment("timeout", 120,
                """
                 网络超时时间（秒）。原版默认 30。
                 Network timeout in seconds. Vanilla default is 30.
                 [范围：30 ~ 99999] 填错将自动还原为 120。
                 [Range: 30 ~ 99999] Invalid values will be reset to 120.""");

        setIfAbsentAndComment("packetSize", 67108864,
                """
                 自定义 Payload 数据包最大限制。原版默认 1048576。
                 Custom max limit for Payload packets. Vanilla default is 1048576.
                 [范围：1048576 ~ 2147483647] 填错将自动还原为 67108864。
                 [Range: 1048576 ~ 2147483647] Invalid values will be reset to 67108864.""");

        setIfAbsentAndComment("decoderSize", 67108864,
                """
                 网络解码器容量上限。原版默认 8388608。
                 Network decoder capacity limit. Vanilla default is 8388608.
                 [范围：8388608 ~ 2147483647] 填错将自动还原为 67108864。
                 [Range: 8388608 ~ 2147483647] Invalid values will be reset to 67108864.""");

        setIfAbsentAndComment("chunkPacketData", 16777216,
                """
                 区块数据包容量上限。原版默认 2097152。
                 Chunk packet data capacity limit. Vanilla default is 2097152.
                 [范围：2097152 ~ 2147483647] 填错将自动还原为 16777216。
                 [Range: 2097152 ~ 2147483647] Invalid values will be reset to 16777216.""");

        setIfAbsentAndComment("nbtMaxSize", 33554432L,
                """
                 NBT 数据最大读取限制（字节）。原版默认 2097152。
                 Max NBT data read limit in bytes. Vanilla default is 2097152.
                 [范围：2097152 ~ 8589934592 (8GB)] 填错将自动还原为 33554432。
                 [Range: 2097152 ~ 8589934592 (8GB)] Invalid values will be reset to 33554432.""");

        setIfAbsentAndComment("stringSize", 262144,
                """
                 字符串最大长度限制。原版默认 32767。
                 Max string length limit. Vanilla default is 32767.
                 [范围：32767 ~ 2147483647] 填错将自动还原为 262144。
                 [Range: 32767 ~ 2147483647] Invalid values will be reset to 262144.""");

        setIfAbsentAndComment("varInt", 5,
                """
                 VarInt 字节数限制。原版默认 5。
                 VarInt byte limit. Vanilla default is 5.
                 [范围：5 ~ 10] 填错将自动还原为 5。
                 [Range: 5 ~ 10] Invalid values will be reset to 5.""");

        setIfAbsentAndComment("varLong", 10,
                """
                 VarLong 字节数限制。原版默认 10。
                 VarLong byte limit. Vanilla default is 10.
                 [范围：10 ~ 20] 填错将自动还原为 10。
                 [Range: 10 ~ 20] Invalid values will be reset to 10.""");

        setIfAbsentAndComment("varInt21", 8,
                """
                 VarInt21FrameDecoder 字节限制。原版默认 3。
                 VarInt21FrameDecoder byte limit. Vanilla default is 3.
                 [范围：3 ~ 16] 填错将自动还原为 8。
                 [Range: 3 ~ 16] Invalid values will be reset to 8.""");
    }

    private static int getIntSafe(String key, int def, int min, int max) {
        Object val = configData.get(key);
        if (val instanceof Number num) {
            long l = num.longValue();
            if (l >= min && l <= max) {
                return (int) l;
            }
        }
        return def;
    }

    private static long getNbtMaxSizeSafe() {
        Object val = configData.get("nbtMaxSize");
        if (val instanceof Number num) {
            long l = num.longValue();
            if (l >= 2097152L && l <= 8589934592L) {
                return l;
            }
        } else if (val instanceof String str) {
            try {
                long l = Long.parseLong(str);
                if (l >= 2097152L && l <= 8589934592L) {
                    return l;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 33554432L;
    }

    private static void readValues() {
        timeout = getIntSafe("timeout", 120, 30, 99999);
        packetSize = getIntSafe("packetSize", 67108864, 1048576, Integer.MAX_VALUE);
        decoderSize = getIntSafe("decoderSize", 67108864, 8388608, Integer.MAX_VALUE);
        chunkPacketData = getIntSafe("chunkPacketData", 16777216, 2097152, Integer.MAX_VALUE);
        nbtMaxSize = getNbtMaxSizeSafe();
        stringSize = getIntSafe("stringSize", 262144, 32767, Integer.MAX_VALUE);
        varInt = getIntSafe("varInt", 5, 5, 10);
        varLong = getIntSafe("varLong", 10, 10, 20);
        varInt21 = getIntSafe("varInt21", 8, 3, 16);
    }

    public static void save() {
        if (configData == null) return;
        configData.set("timeout", timeout);
        configData.set("packetSize", packetSize);
        configData.set("decoderSize", decoderSize);
        configData.set("chunkPacketData", chunkPacketData);
        configData.set("nbtMaxSize", nbtMaxSize);
        configData.set("stringSize", stringSize);
        configData.set("varInt", varInt);
        configData.set("varLong", varLong);
        configData.set("varInt21", varInt21);
        configData.save();
    }
}