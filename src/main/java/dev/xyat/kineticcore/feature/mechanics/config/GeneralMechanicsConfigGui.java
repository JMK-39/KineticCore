package dev.xyat.kineticcore.feature.mechanics.config;

import dev.xyat.kineticcore.ConfigGui;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

@KTClientModule
public class GeneralMechanicsConfigGui {
    public static final String PAGE_ID = "kineticcore:general_mechanics";

    public static void load() {
        ConfigGui.register(buildPage());
    }

    public static Screen create(Screen parent) {
        return ConfigGui.create(parent, PAGE_ID);
    }

    private static KTConfigPage buildPage() {
        return KTConfigPage.builder(PAGE_ID, Component.translatable("cfg.kineticcore.mechanics"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(Component.translatable("cfg.kineticcore.mechanics.apply_notice"))
                .section(Component.translatable("cfg.kineticcore.mechanics"))
                .booleanValue(
                        "pvp_protection",
                        Component.translatable("cfg.kineticcore.mech.pvp_protection"),
                        () -> GeneralMechanicsConfig.enablePvpProtection,
                        value -> GeneralMechanicsConfig.enablePvpProtection = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.pvp_protection.tooltip")
                )
                .booleanValue(
                        "entity_fixer",
                        Component.translatable("cfg.kineticcore.mech.entity_fixer"),
                        () -> GeneralMechanicsConfig.enableEntityAttributeFixer,
                        value -> GeneralMechanicsConfig.enableEntityAttributeFixer = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.entity_fixer.tooltip")
                )
                .booleanValue(
                        "always_edible",
                        Component.translatable("cfg.kineticcore.mech.always_edible"),
                        () -> GeneralMechanicsConfig.enableAlwaysEdible,
                        value -> GeneralMechanicsConfig.enableAlwaysEdible = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.always_edible.tooltip")
                )
                .intValue(
                        "keep_xp",
                        Component.translatable("cfg.kineticcore.mech.keep_xp"),
                        () -> GeneralMechanicsConfig.keepInvXPDropPercentage,
                        value -> GeneralMechanicsConfig.keepInvXPDropPercentage = value,
                        50,
                        0,
                        100,
                        Component.translatable("cfg.kineticcore.mech.keep_xp.tooltip")
                )
                .booleanValue(
                        "farmland",
                        Component.translatable("cfg.kineticcore.mech.farmland"),
                        () -> GeneralMechanicsConfig.enableFarmlandProtection,
                        value -> GeneralMechanicsConfig.enableFarmlandProtection = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.farmland.tooltip")
                )
                .booleanValue(
                        "fast_web",
                        Component.translatable("cfg.kineticcore.mech.fast_web"),
                        () -> GeneralMechanicsConfig.fastCobWebBreaking,
                        value -> GeneralMechanicsConfig.fastCobWebBreaking = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.fast_web.tooltip")
                )
                .booleanValue(
                        "void_immunity",
                        Component.translatable("cfg.kineticcore.mech.void_immunity"),
                        () -> GeneralMechanicsConfig.enableCreativeVoidImmunity,
                        value -> GeneralMechanicsConfig.enableCreativeVoidImmunity = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.void_immunity.tooltip")
                )
                .booleanValue(
                        "no_recipe_book",
                        Component.translatable("cfg.kineticcore.mech.no_recipe_book"),
                        () -> GeneralMechanicsConfig.removeRecipeBook,
                        value -> GeneralMechanicsConfig.removeRecipeBook = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.no_recipe_book.tooltip")
                )
                .booleanValue(
                        "let_me_despawn",
                        Component.translatable("cfg.kineticcore.mech.let_me_despawn"),
                        () -> GeneralMechanicsConfig.enableLetMeDespawn,
                        value -> GeneralMechanicsConfig.enableLetMeDespawn = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.let_me_despawn.tooltip")
                )
                .entityList(
                        "despawn_whitelist",
                        Component.translatable("cfg.kineticcore.mech.despawn_whitelist"),
                        () -> GeneralMechanicsConfig.despawnWhiteList,
                        value -> GeneralMechanicsConfig.despawnWhiteList = value,
                        List.of(),
                        Component.translatable("cfg.kineticcore.mech.despawn_whitelist.tooltip")
                )
                .booleanValue(
                        "recycle_bin",
                        Component.translatable("cfg.kineticcore.mech.recycle_bin"),
                        () -> GeneralMechanicsConfig.recycleBinWorlds,
                        value -> GeneralMechanicsConfig.recycleBinWorlds = value,
                        true,
                        Component.translatable("cfg.kineticcore.mech.recycle_bin.tooltip")
                )
                .stringList(
                        "void_damage_whitelist",
                        Component.translatable("cfg.kineticcore.mech.void_damage_whitelist"),
                        () -> GeneralMechanicsConfig.voidDamageWhiteList,
                        value -> GeneralMechanicsConfig.voidDamageWhiteList = value,
                        List.of(),
                        Component.translatable("cfg.kineticcore.mech.void_damage_whitelist.tooltip")
                )
                .intValue(
                        "void_damage_percentage",
                        Component.translatable("cfg.kineticcore.mech.void_damage_percentage"),
                        () -> GeneralMechanicsConfig.voidDamagePercentage,
                        value -> GeneralMechanicsConfig.voidDamagePercentage = value,
                        10,
                        0,
                        100,
                        Component.translatable("cfg.kineticcore.mech.void_damage_percentage.tooltip")
                )
                .build();
    }
}
