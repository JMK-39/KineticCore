package dev.xyat.kineticcore.feature.mechanics.config;

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
public class GeneralMechanicsConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/general.toml");
    private static CommentedFileConfig configData;

    public static boolean enablePvpProtection = true;
    public static boolean enableEntityAttributeFixer = true;
    public static boolean enableAlwaysEdible = true;
    public static boolean enableFarmlandProtection = true;
    public static boolean fastCobWebBreaking = true;
    public static boolean enableCreativeVoidImmunity = true;
    public static boolean removeRecipeBook = true;
    public static int keepInvXPDropPercentage = 50;
    public static boolean enableLetMeDespawn = true;
    public static List<String> despawnWhiteList = new ArrayList<>();
    public static boolean recycleBinWorlds = true;
    public static List<String> voidDamageWhiteList = new ArrayList<>();
    public static int voidDamagePercentage = 10;

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
            KineticCore.LOGGER.error("GeneralMechanicsConfig Load Failed", e);
        }
    }

    private static void registerServerConfig() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticcore:general_mechanics")
                .booleanValue("pvp_protection", () -> enablePvpProtection, value -> enablePvpProtection = value)
                .booleanValue("entity_fixer", () -> enableEntityAttributeFixer, value -> enableEntityAttributeFixer = value)
                .booleanValue("always_edible", () -> enableAlwaysEdible, value -> enableAlwaysEdible = value)
                .intValue("keep_xp", () -> keepInvXPDropPercentage, value -> keepInvXPDropPercentage = value, 0, 100)
                .booleanValue("farmland", () -> enableFarmlandProtection, value -> enableFarmlandProtection = value)
                .booleanValue("fast_web", () -> fastCobWebBreaking, value -> fastCobWebBreaking = value)
                .booleanValue("void_immunity", () -> enableCreativeVoidImmunity, value -> enableCreativeVoidImmunity = value)
                .booleanValue("no_recipe_book", () -> removeRecipeBook, value -> removeRecipeBook = value)
                .booleanValue("let_me_despawn", () -> enableLetMeDespawn, value -> enableLetMeDespawn = value)
                .stringList("despawn_whitelist", () -> despawnWhiteList, value -> despawnWhiteList = new ArrayList<>(value))
                .booleanValue("recycle_bin", () -> recycleBinWorlds, value -> recycleBinWorlds = value)
                .stringList("void_damage_whitelist", () -> voidDamageWhiteList, value -> voidDamageWhiteList = new ArrayList<>(value))
                .intValue("void_damage_percentage", () -> voidDamagePercentage, value -> voidDamagePercentage = value, 0, 100)
                .onSave(GeneralMechanicsConfig::save)
                .build());
    }

    private static void setupConfig() {
        configData.setComment("mechanics", """
             游戏机制设置
             Game Mechanics Settings""");

        define("mechanics.enablePvpProtection", true, """
             是否开启增强型 PVP 保护 (受保护玩家及其仆从免受他人伤害，且自动消除相关仇恨)
             Enable enhanced PVP protection, preventing damage and aggro between protected players, others, and their minions.""");

        define("mechanics.enableEntityAttributeFixer", true, """
             是否开启实体属性修复 (自动清除或修复血量为 NaN 的幽灵实体)
             Enable entity attribute fixer (fixes NaN health entities).""");

        define("mechanics.enableAlwaysEdible", true, """
             是否开启“暴食模式” (满腹度也可进食，即不饥饿时也能吃东西)
             Enable 'Gluttony Mode' (Always edible), allows eating food even when not hungry.""");

        define("mechanics.enableFarmlandProtection", true, """
             是否开启耕地保护 (穿着带有“保护”附魔的靴子踩踏不破坏耕地)
             Enable Farmland Protection. Players wearing boots enchanted with 'Protection' will not destroy Farmland by stepping on it.""");

        define("mechanics.fastCobWebBreaking", true, """
             允许斧头等工具像剑/剪刀一样快速破坏蜘蛛网
             Allows tools like Axes to break Cobwebs as fast as Swords or Shears.""");

        define("mechanics.recycleBinWorlds", true, """
             删除世界存档时将其移至系统回收站而非直接销毁 (仅支持桌面环境)
             Move deleted worlds to the system recycle bin instead of permanent deletion (Desktop only).""");

        configData.setComment("creative", """
             创造模式增强
             Creative Mode Enhancements""");

        define("creative.enableVoidImmunity", true, """
             是否为创造模式玩家开启虚空与Kill指令免疫保护
             Enable Void and Kill Immunity for Creative Mode players.""");

        configData.setComment("recipe_book", """
             配方书设置
             Recipe Book Settings""");

        define("recipe_book.removeRecipeBook", true, """
             彻底移除配方书功能以显著提升加载速度并降低内存占用
             Completely remove the Recipe Book function to improve loading speed and reduce memory usage.""");

        configData.setComment("death", """
             死亡惩罚设置
             Death Penalty Settings""");

        define("death.keep_inventory_drop_xp_percentage", 50, """
             开启死亡不掉落时的经验掉落比例 (0-100)
             Percentage of XP to drop when the 'keepInventory' gamerule is enabled (0-100).""");

        configData.setComment("mobs", """
             生物消失优化
             Mob Despawn Tweaks""");

        define("mobs.enableLetMeDespawn", true, """
             是否开启“捡取物品后仍可消失”功能
             Enable 'Let Me Despawn' feature.""");

        define("mobs.despawnWhiteList", new ArrayList<>(), """
             强制保持永久存在的生物名单，支持使用 @modid 排除整个模组
             Whitelist of entities, support @modid to exclude entire mod.""");

        configData.setComment("void_damage", """
             虚空伤害设置
             Void Damage Settings""");

        define("void_damage.whiteList", new ArrayList<>(), """
             保持原版虚空伤害的生物白名单，支持使用 @modid 排除模组，#标签 排除实体标签，或直接填写实体ID
             Whitelist of entities to keep vanilla void damage. Supports @modid, #tag, or entity ID.""");

        define("void_damage.percentage", 10, """
             虚空伤害每次扣除最大生命值的百分比 (0-100)
             Percentage of max health deducted per void damage tick (0-100).""");
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) configData.set(path, def);
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enablePvpProtection = configData.getOrElse("mechanics.enablePvpProtection", true);
        enableEntityAttributeFixer = configData.getOrElse("mechanics.enableEntityAttributeFixer", true);
        enableAlwaysEdible = configData.getOrElse("mechanics.enableAlwaysEdible", true);
        enableFarmlandProtection = configData.getOrElse("mechanics.enableFarmlandProtection", true);
        fastCobWebBreaking = configData.getOrElse("mechanics.fastCobWebBreaking", true);
        enableCreativeVoidImmunity = configData.getOrElse("creative.enableVoidImmunity", true);
        removeRecipeBook = configData.getOrElse("recipe_book.removeRecipeBook", true);
        keepInvXPDropPercentage = configData.getOrElse("death.keep_inventory_drop_xp_percentage", 50);
        enableLetMeDespawn = configData.getOrElse("mobs.enableLetMeDespawn", true);
        despawnWhiteList = configData.getOrElse("mobs.despawnWhiteList", new ArrayList<>());
        recycleBinWorlds = configData.getOrElse("mechanics.recycleBinWorlds", true);
        voidDamageWhiteList = configData.getOrElse("void_damage.whiteList", new ArrayList<>());
        voidDamagePercentage = configData.getOrElse("void_damage.percentage", 10);
    }

    public static void save() {
        if (configData == null) return;
        configData.set("mechanics.enablePvpProtection", enablePvpProtection);
        configData.set("mechanics.enableEntityAttributeFixer", enableEntityAttributeFixer);
        configData.set("mechanics.enableAlwaysEdible", enableAlwaysEdible);
        configData.set("mechanics.enableFarmlandProtection", enableFarmlandProtection);
        configData.set("mechanics.fastCobWebBreaking", fastCobWebBreaking);
        configData.set("creative.enableVoidImmunity", enableCreativeVoidImmunity);
        configData.set("recipe_book.removeRecipeBook", removeRecipeBook);
        configData.set("death.keep_inventory_drop_xp_percentage", keepInvXPDropPercentage);
        configData.set("mobs.enableLetMeDespawn", enableLetMeDespawn);
        configData.set("mobs.despawnWhiteList", despawnWhiteList);
        configData.set("mechanics.recycleBinWorlds", recycleBinWorlds);
        configData.set("void_damage.whiteList", voidDamageWhiteList);
        configData.set("void_damage.percentage", voidDamagePercentage);
        configData.save();
        readValues();
    }
}