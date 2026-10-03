package dev.xyat.kineticcore.feature.firstjoin.config;

import dev.xyat.kineticcore.api.inventory.KineticItemText;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.api.config.server.KTServerConfigSpec;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerConfig {
    // Item data follows /give: {NBT} on 1.20.1, [components] from 1.20.5 on. 26.1 writes enchantments as a plain
    // enchantment-to-level map; 1.21.1 wraps it in levels.
    //? if >=26.1 {
    /*public static final String DEFAULT_HELMET = "1x minecraft:leather_helmet[enchantments={\"minecraft:unbreaking\":2,\"minecraft:protection\":2,\"minecraft:respiration\":1}]";
    public static final String DEFAULT_CHESTPLATE = "1x minecraft:leather_chestplate[enchantments={\"minecraft:unbreaking\":2,\"minecraft:protection\":2}]";
    public static final String DEFAULT_LEGGINGS = "1x minecraft:leather_leggings[enchantments={\"minecraft:unbreaking\":2,\"minecraft:protection\":2}]";
    public static final String DEFAULT_BOOTS = "1x minecraft:leather_boots[enchantments={\"minecraft:unbreaking\":2,\"minecraft:protection\":2}]";
    private static final String ITEM_DATA = "[components]";
    *///?} else if >=1.20.5 {
    /*public static final String DEFAULT_HELMET = "1x minecraft:leather_helmet[enchantments={levels:{\"minecraft:unbreaking\":2,\"minecraft:protection\":2,\"minecraft:respiration\":1}}]";
    public static final String DEFAULT_CHESTPLATE = "1x minecraft:leather_chestplate[enchantments={levels:{\"minecraft:unbreaking\":2,\"minecraft:protection\":2}}]";
    public static final String DEFAULT_LEGGINGS = "1x minecraft:leather_leggings[enchantments={levels:{\"minecraft:unbreaking\":2,\"minecraft:protection\":2}}]";
    public static final String DEFAULT_BOOTS = "1x minecraft:leather_boots[enchantments={levels:{\"minecraft:unbreaking\":2,\"minecraft:protection\":2}}]";
    private static final String ITEM_DATA = "[components]";
    *///?} else {
    public static final String DEFAULT_HELMET = "1x minecraft:leather_helmet{Enchantments:[{id:\"minecraft:unbreaking\",lvl:2s},{id:\"minecraft:protection\",lvl:2s},{id:\"minecraft:respiration\",lvl:1s}]}";
    public static final String DEFAULT_CHESTPLATE = "1x minecraft:leather_chestplate{Enchantments:[{id:\"minecraft:unbreaking\",lvl:2s},{id:\"minecraft:protection\",lvl:2s}]}";
    public static final String DEFAULT_LEGGINGS = "1x minecraft:leather_leggings{Enchantments:[{id:\"minecraft:unbreaking\",lvl:2s},{id:\"minecraft:protection\",lvl:2s}]}";
    public static final String DEFAULT_BOOTS = "1x minecraft:leather_boots{Enchantments:[{id:\"minecraft:unbreaking\",lvl:2s},{id:\"minecraft:protection\",lvl:2s}]}";
    private static final String ITEM_DATA = "{NBT}";
    //?}
    public static final String DEFAULT_OFFHAND = "1x minecraft:shield";

    private static final Path CONFIG_PATH = KineticPlatform.configDirectory().resolve("kineticcore/player.toml");
    private static CommentedFileConfig configData;

    // 逻辑开关
    public static boolean enableFirstJoin = true;
    public static boolean clearInvBeforeJoin = true;
    public static int firstJoinDelay = 20;

    // 原始字符串存储 (用于延迟解析)
    public static List<String> firstJoinItemsRaw = new ArrayList<>();
    public static List<String> firstJoinCommands = new ArrayList<>();

    public static String helmetId = "";
    public static String chestplateId = "";
    public static String leggingsId = "";
    public static String bootsId = "";
    public static String offhandId = "";

    // 缓存对象 (改为 Map 以支持槽位绑定)
    private static final Map<Integer, ItemStack> CACHED_JOIN_ITEMS = new HashMap<>();
    private static final Map<EquipmentSlot, ItemStack> CACHED_ARMOR = new EnumMap<>(EquipmentSlot.class);
    private static boolean isCacheInitialized = false;
    // Whether every configured reward parsed; a reward with a broken entry is not handed out at all.
    private static boolean rewardsValid = true;

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
            KineticRuntime.logger().error("PlayerConfig Load Failed", e);
        }
    }

    private static void registerServerConfig() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticcore:first_join")
                .booleanValue("enabled", () -> enableFirstJoin, value -> enableFirstJoin = value)
                .booleanValue("clear_inventory", () -> clearInvBeforeJoin, value -> clearInvBeforeJoin = value)
                .doubleValue("delay", () -> firstJoinDelay / 20.0D,
                        value -> firstJoinDelay = (int) Math.round(value * 20.0D),
                        0.0D, Integer.MAX_VALUE / 20.0D)
                .stringList("items", () -> firstJoinItemsRaw, value -> firstJoinItemsRaw = new ArrayList<>(value))
                .stringList("commands", () -> firstJoinCommands, value -> firstJoinCommands = new ArrayList<>(value))
                .stringValue("helmet", () -> helmetId, value -> helmetId = value)
                .stringValue("chestplate", () -> chestplateId, value -> chestplateId = value)
                .stringValue("leggings", () -> leggingsId, value -> leggingsId = value)
                .stringValue("boots", () -> bootsId, value -> bootsId = value)
                .stringValue("offhand", () -> offhandId, value -> offhandId = value)
                .onSave(PlayerConfig::save)
                .build());
    }

    private static void setupConfig() {
        configData.setComment("first_join", """
              首次进入奖励设置
              First Join Reward Settings""");
        define("first_join.enable", true, """
              启用首次加入奖励功能
               Enable the First Join reward feature""");
        define("first_join.clear_inventory", true, """
               发放奖励前清空快捷栏（背包和装备不动）
               Clear the hotbar before giving rewards (backpack and equipment are kept)""");
        define("first_join.delay_ticks", 20, """
               发放奖励的延迟时间 (刻)
               Delay before giving rewards (in ticks)""");
        define("first_join.items", new ArrayList<>(List.of(
                "[0] 1x minecraft:iron_sword",
                "[1] 32x minecraft:bread",
                "[2] 16x minecraft:apple"
        )), """
     奖励物品列表 (支持 '[槽位] 数量x 物品ID%s' 格式)
     Reward item list (Supports '[Slot] Countx ItemID%s' format)""".formatted(ITEM_DATA, ITEM_DATA));

        define("first_join.commands", new ArrayList<>(List.of("say Welcome @s!")), """
     玩家首次加入时执行的指令列表
     List of commands to execute when a player first joins""");

        configData.setComment("first_join.armor", """
     初始装备 (支持 %s，每个部位可独立设定)
     Starting Equipment (Supports %s, each slot can be configured independently)""".formatted(ITEM_DATA, ITEM_DATA));

        define("first_join.armor.helmet", DEFAULT_HELMET,
                " 头盔\n Helmet");

        define("first_join.armor.chestplate", DEFAULT_CHESTPLATE,
                " 胸甲\n Chestplate");

        define("first_join.armor.leggings", DEFAULT_LEGGINGS,
                " 护腿\n Leggings");

        define("first_join.armor.boots", DEFAULT_BOOTS,
                " 靴子\n Boots");

        define("first_join.armor.offhand", DEFAULT_OFFHAND,
                " 副手物品\n Offhand Item");
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) configData.set(path, def);
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enableFirstJoin = configData.getOrElse("first_join.enable", true);
        clearInvBeforeJoin = configData.getOrElse("first_join.clear_inventory", true);
        firstJoinDelay = configData.getOrElse("first_join.delay_ticks", 20);
        firstJoinItemsRaw = configData.getOrElse("first_join.items", new ArrayList<>());
        firstJoinCommands = configData.getOrElse("first_join.commands", new ArrayList<>());
        helmetId = configData.getOrElse("first_join.armor.helmet", DEFAULT_HELMET);
        chestplateId = configData.getOrElse("first_join.armor.chestplate", DEFAULT_CHESTPLATE);
        leggingsId = configData.getOrElse("first_join.armor.leggings", DEFAULT_LEGGINGS);
        bootsId = configData.getOrElse("first_join.armor.boots", DEFAULT_BOOTS);
        offhandId = configData.getOrElse("first_join.armor.offhand", DEFAULT_OFFHAND);

        isCacheInitialized = false;
    }

    public static Map<Integer, ItemStack> getJoinItems() {
        if (!isCacheInitialized) rebuildCache();
        return CACHED_JOIN_ITEMS;
    }

    public static Map<EquipmentSlot, ItemStack> getArmor() {
        if (!isCacheInitialized) rebuildCache();
        return CACHED_ARMOR;
    }

    /** Returns whether every configured first-join item and armor entry parsed. */
    public static boolean rewardsValid() {
        if (!isCacheInitialized) rebuildCache();
        return rewardsValid;
    }

    private static void rebuildCache() {
        CACHED_JOIN_ITEMS.clear();
        CACHED_ARMOR.clear();
        boolean valid = true;

        int defaultSlot = 0;
        for (String s : firstJoinItemsRaw) {
            int slot = defaultSlot;
            String itemStr = s.trim();

            // 解析前置槽位标签，如 "[8] 64x minecraft:stone"
            if (itemStr.startsWith("[")) {
                int end = itemStr.indexOf("]");
                if (end != -1) {
                    try {
                        slot = Integer.parseInt(itemStr.substring(1, end));
                        itemStr = itemStr.substring(end + 1).trim();
                    } catch (Exception ignored) {}
                }
            }

            ItemStack stack = parseItemStackInternal(itemStr);
            if (!stack.isEmpty()) {
                CACHED_JOIN_ITEMS.put(slot, stack);
            } else if (!itemStr.isBlank()) {
                valid = false;
            }
            defaultSlot++;
        }

        valid &= cacheArmor(EquipmentSlot.HEAD, helmetId);
        valid &= cacheArmor(EquipmentSlot.CHEST, chestplateId);
        valid &= cacheArmor(EquipmentSlot.LEGS, leggingsId);
        valid &= cacheArmor(EquipmentSlot.FEET, bootsId);
        valid &= cacheArmor(EquipmentSlot.OFFHAND, offhandId);

        rewardsValid = valid;
        isCacheInitialized = true;
    }

    // Caches one armor entry; an empty entry means no item, a non-empty one that does not parse is an error.
    private static boolean cacheArmor(EquipmentSlot slot, String entry) {
        ItemStack stack = parseItemStackInternal(entry);
        CACHED_ARMOR.put(slot, stack);
        return !stack.isEmpty() || entry == null || entry.isBlank();
    }

    private static ItemStack parseItemStackInternal(String input) {
        if (input == null || input.isBlank()) return ItemStack.EMPTY;

        try {
            String s = input.replace("\r", "").replace("\n", "").replaceAll("\\s{2,}", " ").trim();
            int count = 1;
            String itemPart = s;

            Matcher matcher = Pattern.compile("^(\\d+)[xX]\\s+(.*)$").matcher(s);
            if (matcher.matches()) {
                count = Integer.parseInt(matcher.group(1));
                itemPart = matcher.group(2).trim();
            }

            // The item data follows the id in this version's /give syntax only: {NBT} on 1.20.1, [components] since 1.20.5.
            String itemId;
            String dataStr = "";
            //? if >=1.20.5 {
            /*int dataStart = itemPart.indexOf('[');
            *///?} else {
            int dataStart = itemPart.indexOf('{');
            //?}
            if (dataStart >= 0) {
                itemId = itemPart.substring(0, dataStart).trim();
                dataStr = itemPart.substring(dataStart).trim();
            } else {
                itemId = itemPart;
            }
            if (itemId.indexOf('{') >= 0 || itemId.indexOf('[') >= 0) {
                throw new IllegalArgumentException("item data is not in this Minecraft version's /give syntax");
            }

            itemId = itemId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-:.]", "");
            if (!itemId.contains(":")) itemId = "minecraft:" + itemId;

            ResourceLocation loc = KineticResourceIds.tryParse(itemId);
            if (loc != null) {
                Item item = KineticRegistries.items().get(loc);
                if (item != null && item != Items.AIR) {
                    ItemStack stack = KineticItemText.parse(itemId + dataStr);
                    stack.setCount(count);
                    return stack;
                }
            }
            KineticRuntime.logger().error("[FirstJoin] 物品解析失败: {} | 错误: 未知物品 {}", input, itemId);
        } catch (Exception e) {
            KineticRuntime.logger().error("[FirstJoin] 物品解析失败: {} | 错误: {}", input, e.getMessage());
        }
        return ItemStack.EMPTY;
    }

    /**
     * 将 ItemStack 序列化为配置文件支持的字符串格式
     */
    public static String serializeItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = KineticRegistries.items().id(stack.getItem());
        if (id == null) return "";

        return stack.getCount() + "x " + KineticItemText.format(stack);
    }

    public static ItemStack parseItemStack(String input) {
        return parseItemStackInternal(input);
    }

    public record JoinItemEntry(int slot, ItemStack stack) {
    }

    public static void save() {
        if (configData == null) {
            throw new IllegalStateException("First join config is not loaded");
        }
        configData.set("first_join.enable", enableFirstJoin);
        configData.set("first_join.clear_inventory", clearInvBeforeJoin);
        configData.set("first_join.delay_ticks", firstJoinDelay);
        configData.set("first_join.items", firstJoinItemsRaw);
        configData.set("first_join.commands", firstJoinCommands);
        configData.set("first_join.armor.helmet", helmetId);
        configData.set("first_join.armor.chestplate", chestplateId);
        configData.set("first_join.armor.leggings", leggingsId);
        configData.set("first_join.armor.boots", bootsId);
        configData.set("first_join.armor.offhand", offhandId);
        configData.save();
        readValues();
    }
}
