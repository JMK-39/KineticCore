package dev.xyat.kineticcore.feature.setspawn.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.*;

@KTModule
public class SetSpawnConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/setspawn.toml");
    private static CommentedFileConfig configData;

    public static boolean enableCustomSpawn = true;
    public static int spawnSearchRadius = 10000;
    public static int structureRadius = 256;
    public static int structureSearchTimeoutSeconds = 12;
    public static int biomeStep = 48;

    public static boolean enableDimensions = false;
    public static List<String> setspawnDimensions = new ArrayList<>();

    public static boolean enableBiomes = false;
    public static List<String> setspawnBiomes = new ArrayList<>();

    public static boolean enableStructures = true;
    public static List<String> setspawnStructures = new ArrayList<>();

    private static final List<String> DEFAULT_BIOMES = Arrays.asList(
            "minecraft:plains", "minecraft:desert", "minecraft:savanna"
    );
    private static final List<String> DEFAULT_STRUCTURES = Arrays.asList(
            "minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna",
            "minecraft:village_snowy", "minecraft:village_taiga"
    );

    public static void load() {
        try {
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build();
            configData.load();
            setupConfig();
            configData.save();
            readValues();
            registerServerConfig();
        } catch (Exception e) {
            KineticCore.LOGGER.error("出生地修改: SetSpawnConfig Load Failed", e);
        }
    }

    private static void registerServerConfig() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticcore:setspawn")
                .booleanValue("enable", () -> enableCustomSpawn, value -> enableCustomSpawn = value)
                .intValue("radius", () -> spawnSearchRadius, value -> spawnSearchRadius = value, 0, Integer.MAX_VALUE)
                .intValue("structure_radius", () -> structureRadius, value -> structureRadius = value, 0, Integer.MAX_VALUE)
                .intValue("structure_timeout_seconds", () -> structureSearchTimeoutSeconds, value -> structureSearchTimeoutSeconds = value, 1, Integer.MAX_VALUE)
                .intValue("biome_step", () -> biomeStep, value -> biomeStep = value, 1, Integer.MAX_VALUE)
                .onSave(SetSpawnConfig::save)
                .build());
        KTServerConfigApi.registerActionPage("kineticcore:setspawn_rules");
    }

    private static void setupConfig() {
        configData.setComment("setspawn", """
                 世界出生点优化核心设置
                 Core Settings for World SetSpawn Optimization""");

        define("setspawn.enable", true, """
                 是否启用自定义出生点搜索逻辑。(通过完全原生注入实现，不会影响玩家睡床设置出生点)
                 Whether to enable custom spawn point search logic. (Implemented via pure native injection, respects player beds)""");

        define("setspawn.radius", 10000, """
                 生物群系搜索的最大半径限制 (单位: 方块)。
                 Maximum search radius limit for biome mode (Unit: Blocks).""");

        define("setspawn.structure_radius", 256, """
                 【危险注意事项】
                 结构模式的最大搜索半径 (单位: 区块)。1 区块 = 16 方块。
                 由于搜索算法开销极大，设置过大 (如 > 512) 极易导致创建世界时严重卡顿甚至崩溃！
                 特别注意：末地 (The End) 存在 1000 格 (约 64 区块) 的虚空环，若将末地作为出生地且指定了外岛结构，必须将此值设置大于 64！
                 
                 [IMPORTANT WARNING]
                 Maximum search radius for structure mode (Unit: Chunks). 1 chunk = 16 blocks.
                 Because the search algorithm is highly expensive, setting this too high (e.g. > 512) can easily cause severe lag or crashes during world creation!
                 Special Note: The End has a 1000-block (approx. 64 chunks) void ring. If spawning in The End outer islands, this MUST be set > 64!""");

        define("setspawn.structure_timeout_seconds", 12, """
                 结构出生点搜索超时时间，单位秒。超过这个时间还没有找到结构，会自动回退到群系/维度出生逻辑，最后仍失败则交回原版默认出生。
                 Structure spawn search timeout in seconds. If no structure is found before timeout, the logic falls back to biome/dimension spawn, then vanilla default spawn if still unresolved.""");

        define("setspawn.biome_step", 48, """
                 生物群系搜索时的采样步长。(建议值: 32-64)
                 值越小，搜索越精确 (能找到微小或细长的群系)，但耗时更长；值越大，搜索速度越快，但可能会漏掉目标群系。
                 
                 Sampling step size for biome search. (Suggested value: 32-64)
                 Smaller values are more accurate (can find tiny or narrow biomes) but take longer; larger values are faster but might skip the target biome.""");

        define("setspawn.rule_dimension.enable", false, """
                 优先级 1: 是否启用维度限制。若启用，玩家只会出生在列表允许的维度中。
                 Priority 1: Enable dimension limit. If enabled, players will only spawn in the allowed dimensions.""");

        define("setspawn.rule_dimension.list", new ArrayList<>(), """
                 允许作为出生点的维度列表。留空表示在默认主世界生成。(注: 会自动无视并过滤 "minecraft:overworld")
                 List of allowed spawn dimensions. Leave empty to spawn in Overworld defaultly. (Note: "minecraft:overworld" will be ignored and filtered automatically)""");

        define("setspawn.rule_biome.enable", false, """
                 优先级 2: 是否启用生物群系搜索。若启用，将在指定维度内寻找列表中的群系作为出生点。
                 Priority 2: Enable biome search. If enabled, it will search for biomes in the list within the specified dimension.""");

        define("setspawn.rule_biome.list", DEFAULT_BIOMES, """
                 允许作为出生点的生物群系列表。默认: 平原、沙漠、热带草原。
                 List of allowed spawn biomes. Default: Plains, Desert, Savanna.""");

        define("setspawn.rule_structure.enable", true, """
                 优先级 3: 是否启用结构搜索。若启用，将在匹配的维度和群系中寻找列表中的结构作为出生点。
                 Priority 3: Enable structure search. If enabled, it will search for structures in the list within matching dimensions and biomes.""");

        define("setspawn.rule_structure.list", DEFAULT_STRUCTURES, """
                 允许作为出生点的结构列表。默认: 原版所有村庄类型。
                 List of allowed spawn structures. Default: All vanilla village types.""");
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) configData.set(path, def);
        configData.setComment(path, comment);
    }

    private static void readValues() {
        enableCustomSpawn = configData.getOrElse("setspawn.enable", true);
        spawnSearchRadius = Math.max(0, configData.getOrElse("setspawn.radius", 10000));
        structureRadius = Math.max(0, configData.getOrElse("setspawn.structure_radius", 256));
        structureSearchTimeoutSeconds = configData.getOrElse("setspawn.structure_timeout_seconds", 12);
        if (structureSearchTimeoutSeconds < 1) structureSearchTimeoutSeconds = 1;
        biomeStep = Math.max(1, configData.getOrElse("setspawn.biome_step", 48));

        enableDimensions = configData.getOrElse("setspawn.rule_dimension.enable", false);
        setspawnDimensions = configData.getOrElse("setspawn.rule_dimension.list", new ArrayList<>());
        setspawnDimensions.removeIf(dim -> dim.equals("minecraft:overworld")); // 自动过滤主世界

        enableBiomes = configData.getOrElse("setspawn.rule_biome.enable", false);
        setspawnBiomes = configData.getOrElse("setspawn.rule_biome.list", new ArrayList<>(DEFAULT_BIOMES));

        enableStructures = configData.getOrElse("setspawn.rule_structure.enable", true);
        setspawnStructures = configData.getOrElse("setspawn.rule_structure.list", new ArrayList<>(DEFAULT_STRUCTURES));
    }

    public static void save() {
        if (configData == null) {
            throw new IllegalStateException("SetSpawn config is not loaded");
        }
        configData.set("setspawn.enable", enableCustomSpawn);
        configData.set("setspawn.radius", spawnSearchRadius);
        configData.set("setspawn.structure_radius", structureRadius);
        configData.set("setspawn.structure_timeout_seconds", structureSearchTimeoutSeconds);
        configData.set("setspawn.biome_step", biomeStep);

        configData.set("setspawn.rule_dimension.enable", enableDimensions);
        configData.set("setspawn.rule_dimension.list", setspawnDimensions);

        configData.set("setspawn.rule_biome.enable", enableBiomes);
        configData.set("setspawn.rule_biome.list", setspawnBiomes);

        configData.set("setspawn.rule_structure.enable", enableStructures);
        configData.set("setspawn.rule_structure.list", setspawnStructures);

        configData.save();
        readValues();
    }
}
