package dev.xyat.kineticcore.feature.worldinit.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@KTModule
public class WorldInitConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/world_init.toml");
    private static CommentedFileConfig configData;

    public static boolean enableWorldInit = true;
    public static List<String> worldInitCommands = new ArrayList<>();

    public static void load() {
        try {
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync()
                    .preserveInsertionOrder()
                    .writingMode(WritingMode.REPLACE)
                    .build();

            configData.load();
            setupConfig();
            configData.save();
            readValues();
            registerServerConfig();
        } catch (Exception e) {
            KineticCore.LOGGER.error("WorldInitConfig Load Failed", e);
        }
    }

    private static void registerServerConfig() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticcore:worldinit")
                .booleanValue("enable_world_init", () -> enableWorldInit, value -> enableWorldInit = value)
                .stringList("commands", () -> worldInitCommands, value -> worldInitCommands = new ArrayList<>(value))
                .onSave(WorldInitConfig::save)
                .build());
    }

    private static void setupConfig() {
        configData.setComment("world_init", "世界初始化设置\nWorld Initialization Settings");

        define("world_init.enable", true,
                "是否启用首次加载存档初始化逻辑。\nEnable first world-load initialization logic.");

        define("world_init.commands", new ArrayList<>(),
                "首次加载这个存档时执行的指令列表。每个列表项或每一行都会被当作一条独立指令执行。不要加斜杠 /，如果误加也会自动去掉。某条指令失败不会阻止后续指令继续执行，失败内容会提醒在线管理员。\n示例:\n[\"say World Initialized!\", \"gamerule keepInventory true\", \"time set day\"]\n\nCommand list executed only on the first load of this world. Each list entry or each line is treated as one independent command. Do not add slash /. If slash is added by mistake, it will be removed automatically. A failed command will not stop later commands, and online admins will be notified.\nExamples:\n[\"say World Initialized!\", \"gamerule keepInventory true\", \"time set day\"]");

        if (configData.contains("gamerules")) {
            configData.remove("gamerules");
        }
        if (configData.contains("world_init.singleplayer_use_save_directory_player_data")) {
            configData.remove("world_init.singleplayer_use_save_directory_player_data");
        }
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) {
            configData.set(path, def);
        }
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enableWorldInit = configData.getOrElse("world_init.enable", true);
        worldInitCommands = configData.getOrElse("world_init.commands", new ArrayList<>());
    }

    public static void save() {
        if (configData == null) {
            throw new IllegalStateException("World init config is not loaded");
        }

        configData.set("world_init.enable", enableWorldInit);
        configData.set("world_init.commands", worldInitCommands);

        if (configData.contains("world_init.singleplayer_use_save_directory_player_data")) {
            configData.remove("world_init.singleplayer_use_save_directory_player_data");
        }

        configData.save();
        readValues();
    }
}
