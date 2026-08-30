package dev.xyat.kineticcore.feature.datapack;

import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.feature.datapack.util.ColorText;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@KTModule
public class PackModule {
    public static File BASE_PACK_DIR;
    public static Path DATA_PACK_DIR;
    public static Path RESOURCE_PACK_DIR;
    public static File CONFIG_TOML;
    public static String LAST_ERROR_ID = null;
    private static final List<String> DATAPACK_ORDER = new ArrayList<>();
    private static final List<String> RESOURCEPACK_ORDER = new ArrayList<>();
    public static final Set<Component> FAILED_PACK_COMPONENTS = new HashSet<>();

    private static final Set<String> FAILED_PACK_NAMES = new HashSet<>();
    private static boolean forgeEventsRegistered;

    public static final FileFilter PACK_FILTER = file -> {
        if (file.getName().equals("logs")) return false;
        return (file.isFile() && file.getName().endsWith(".zip")) || file.isDirectory();
    };

    public static void load() {
        LAST_ERROR_ID = null;

        initializePaths();
        syncAndLoadConfig();
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticcore:datapack_pack_order")
                .stringList("datapacks", PackModule::datapackOrderSnapshot, PackModule::replaceDatapackOrder)
                .onSave(PackModule::saveDatapackOrder)
                .build());
        PackErrorAppender.register();
    }

    public static synchronized void refreshDataPacksOnly() {

        initializePaths();

        try {
            Map<String, List<String>> config = readToml(CONFIG_TOML);
            List<String> currentDataPacks = scanFolder(DATA_PACK_DIR.toFile());

            DATAPACK_ORDER.clear();
            DATAPACK_ORDER.addAll(syncLists(
                    config.getOrDefault("datapacks", new ArrayList<>()),
                    currentDataPacks
            ));

            if (RESOURCEPACK_ORDER.isEmpty()) {
                RESOURCEPACK_ORDER.addAll(config.getOrDefault("resourcepacks", new ArrayList<>()));
            }

            writeStandardToml();
        } catch (Exception e) {
            KineticCore.LOGGER.error("kineticcore: Failed to refresh datapack config", e);
        }
    }

    private static void initializePaths() {
        if (BASE_PACK_DIR == null) {
            BASE_PACK_DIR = new File(FMLPaths.CONFIGDIR.get().toString(), "kineticcore/datapack");
        }
        if (DATA_PACK_DIR == null) {
            DATA_PACK_DIR = Paths.get(BASE_PACK_DIR.toString(), "data");
        }
        if (RESOURCE_PACK_DIR == null) {
            RESOURCE_PACK_DIR = Paths.get(BASE_PACK_DIR.toString(), "resources");
        }
        if (CONFIG_TOML == null) {
            CONFIG_TOML = new File(BASE_PACK_DIR, "pack_order.toml");
        }

        ensureDirectory(BASE_PACK_DIR);
        ensureDirectory(DATA_PACK_DIR.toFile());
        ensureDirectory(RESOURCE_PACK_DIR.toFile());
    }

    private static synchronized void syncAndLoadConfig() {
        try {
            List<String> currentDataPacks = scanFolder(DATA_PACK_DIR.toFile());
            List<String> currentResourcePacks = scanFolder(RESOURCE_PACK_DIR.toFile());
            Map<String, List<String>> config = readToml(CONFIG_TOML);

            DATAPACK_ORDER.clear();
            DATAPACK_ORDER.addAll(syncLists(
                    config.getOrDefault("datapacks", new ArrayList<>()),
                    currentDataPacks
            ));

            RESOURCEPACK_ORDER.clear();
            RESOURCEPACK_ORDER.addAll(syncLists(
                    config.getOrDefault("resourcepacks", new ArrayList<>()),
                    currentResourcePacks
            ));

            writeStandardToml();
        } catch (Exception e) {
            KineticCore.LOGGER.error("kineticcore: Failed to sync datapack config", e);
        }
    }

    private static List<String> syncLists(List<String> saved, List<String> actual) {
        List<String> result = new ArrayList<>();

        for (String name : actual) {
            if (!saved.contains(name)) {
                result.add(name);
            }
        }

        for (String name : saved) {
            if (actual.contains(name)) {
                result.add(name);
            }
        }

        return result;
    }

    private static void writeStandardToml() throws Exception {
        writeStandardToml(DATAPACK_ORDER, RESOURCEPACK_ORDER);
    }

    private static void writeStandardToml(List<String> datapacks, List<String> resourcepacks) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("# kineticcore 全局资源管理与自动化排序系统\n");
        builder.append("# kineticcore Global Resource Management & Auto-Ordering System\n\n");
        builder.append("# [加载顺序与优先级说明 (Priority & Loading Rules)]\n");
        builder.append("# 1. 列表顶部的包具有最高优先级 (TOP Priority)。\n");
        builder.append("#    1. Packs at the top of the list have the absolute highest priority.\n");
        builder.append("# 2. 覆盖逻辑：如果两个包修改了同一个文件，排在上面的包会完全覆盖下方的包。\n");
        builder.append("#    2. Overwrite Logic: Upper packs in the list completely override lower packs.\n");
        builder.append("# 3. 新加入的文件会自动插入列表顶部。\n");
        builder.append("#    3. Newly discovered files are automatically inserted at the top.\n");
        builder.append("# 4. data 目录会在重新进入存档时重新扫描。resources 目录不会跟随该扫描。\n");
        builder.append("#    4. The data directory is rescanned when re-entering a world. The resources directory is not rescanned by this operation.\n\n");
        builder.append("# [支持格式 (Supported Formats)]\n");
        builder.append("# ZIP、带 pack.mcmeta 的标准文件夹、含 data/assets 的文件夹、散装命名空间文件夹。\n");
        builder.append("# ZIP, standard folders with pack.mcmeta, folders containing data/assets, and loose namespace folders.\n\n");
        builder.append("datapacks =[\n");

        for (String name : datapacks) {
            builder.append("    \"").append(name).append("\",\n");
        }

        builder.append("]\n\n");
        builder.append("resourcepacks =[\n");

        for (String name : resourcepacks) {
            builder.append("    \"").append(name).append("\",\n");
        }

        builder.append("]\n");

        Files.writeString(CONFIG_TOML.toPath(), builder.toString(), StandardCharsets.UTF_8);
    }

    public static synchronized void savePackOrder() {
        initializePaths();
        try {
            writeStandardToml();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save pack order", exception);
        }
    }

    public static synchronized void saveDatapackOrder() {
        initializePaths();
        try {
            Map<String, List<String>> existing = readToml(CONFIG_TOML);
            List<String> resources = existing.getOrDefault("resourcepacks", new ArrayList<>());
            writeStandardToml(DATAPACK_ORDER, resources);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save datapack order", exception);
        }
    }

    public static synchronized void saveResourcePackOrder() {
        initializePaths();
        try {
            Map<String, List<String>> existing = readToml(CONFIG_TOML);
            List<String> datapacks = existing.getOrDefault("datapacks", new ArrayList<>());
            writeStandardToml(datapacks, RESOURCEPACK_ORDER);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save resource pack order", exception);
        }
    }

    public static synchronized List<String> datapackOrderSnapshot() {
        return List.copyOf(DATAPACK_ORDER);
    }

    public static synchronized List<String> resourcePackOrderSnapshot() {
        return List.copyOf(RESOURCEPACK_ORDER);
    }

    public static synchronized void replaceDatapackOrder(List<String> values) {
        DATAPACK_ORDER.clear();
        DATAPACK_ORDER.addAll(values);
    }

    public static synchronized void replaceResourcePackOrder(List<String> values) {
        RESOURCEPACK_ORDER.clear();
        RESOURCEPACK_ORDER.addAll(values);
    }

    private static Map<String, List<String>> readToml(File file) {
        Map<String, List<String>> result = new HashMap<>();
        if (!file.exists()) return result;

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            String section = "";

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("datapacks")) {
                    section = "datapacks";
                } else if (line.startsWith("resourcepacks")) {
                    section = "resourcepacks";
                } else if (line.startsWith("]")) {
                    section = "";
                } else if (!section.isEmpty()) {
                    String name = line.replace("\"", "").replace(",", "").trim();
                    if (!name.isEmpty() && !name.equals("[")) {
                        result.computeIfAbsent(section, key -> new ArrayList<>()).add(name);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private static List<String> scanFolder(File folder) {
        File[] files = folder.listFiles(PACK_FILTER);
        if (files == null) return new ArrayList<>();

        return Arrays.stream(files)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    private static void ensureDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            KineticCore.LOGGER.error("Failed to create directory {}", directory.getAbsolutePath());
        }
    }

    public static void register(IEventBus modEventBus) {

        modEventBus.addListener(PackModule::addPackFinders);

        if (!forgeEventsRegistered) {
            MinecraftForge.EVENT_BUS.addListener(PackModule::onPlayerLogin);
            forgeEventsRegistered = true;
        }
    }

    private static void addPackFinders(AddPackFindersEvent event) {
        initializePaths();

        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(new RepositorySource(
                    RESOURCE_PACK_DIR,
                    PackType.CLIENT_RESOURCES
            ));
        } else if (event.getPackType() == PackType.SERVER_DATA) {
            refreshDataPacksOnly();
            event.addRepositorySource(new RepositorySource(
                    DATA_PACK_DIR,
                    PackType.SERVER_DATA
            ));
        }
    }

    public static void addFailedPack(String packName, Component i18nReason) {
        if (FAILED_PACK_NAMES.contains(packName)) return;
        FAILED_PACK_NAMES.add(packName);

        FAILED_PACK_COMPONENTS.add(ColorText.translatable("datapack.kineticcore.failed.entry", packName, i18nReason));
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {

        Player player = event.getEntity();
        if (FAILED_PACK_COMPONENTS.isEmpty()) return;

        player.sendSystemMessage(ColorText.translatable("datapack.kineticcore.failed.title"));

        for (Component failedPack : FAILED_PACK_COMPONENTS) {
            player.sendSystemMessage(Component.literal("- ").append(failedPack));
        }
    }

}
