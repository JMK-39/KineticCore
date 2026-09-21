package dev.xyat.kineticcore.feature.mechanics.config;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class GeneralMechanicsConfigGui {
    public static final String PAGE_ID = "kineticcore:general_mechanics";

    public static void load() {
        KTConfigApi.register(buildPage());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createRegisteredPageScreen(parent, PAGE_ID);
    }

    private static KTConfigPage buildPage() {
        return KTConfigPage.builder(PAGE_ID, KineticI18n.translatable("cfg.kineticcore.mechanics"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(KineticI18n.translatable("cfg.kineticcore.mechanics.apply_notice"))
                .section(KineticI18n.translatable("cfg.kineticcore.mechanics"))
                .booleanValue(
                        "pvp_protection",
                        KineticI18n.translatable("cfg.kineticcore.mech.pvp_protection"),
                        () -> GeneralMechanicsConfig.enablePvpProtection,
                        value -> GeneralMechanicsConfig.enablePvpProtection = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.pvp_protection.tooltip")
                )
                .booleanValue(
                        "entity_fixer",
                        KineticI18n.translatable("cfg.kineticcore.mech.entity_fixer"),
                        () -> GeneralMechanicsConfig.enableEntityAttributeFixer,
                        value -> GeneralMechanicsConfig.enableEntityAttributeFixer = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.entity_fixer.tooltip")
                )
                .booleanValue(
                        "always_edible",
                        KineticI18n.translatable("cfg.kineticcore.mech.always_edible"),
                        () -> GeneralMechanicsConfig.enableAlwaysEdible,
                        value -> GeneralMechanicsConfig.enableAlwaysEdible = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.always_edible.tooltip")
                )
                .intValue(
                        "keep_xp",
                        KineticI18n.translatable("cfg.kineticcore.mech.keep_xp"),
                        () -> GeneralMechanicsConfig.keepInvXPDropPercentage,
                        value -> GeneralMechanicsConfig.keepInvXPDropPercentage = value,
                        50,
                        0,
                        100,
                        KineticI18n.translatable("cfg.kineticcore.mech.keep_xp.tooltip")
                )
                .booleanValue(
                        "farmland",
                        KineticI18n.translatable("cfg.kineticcore.mech.farmland"),
                        () -> GeneralMechanicsConfig.enableFarmlandProtection,
                        value -> GeneralMechanicsConfig.enableFarmlandProtection = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.farmland.tooltip")
                )
                .booleanValue(
                        "fast_web",
                        KineticI18n.translatable("cfg.kineticcore.mech.fast_web"),
                        () -> GeneralMechanicsConfig.fastCobWebBreaking,
                        value -> GeneralMechanicsConfig.fastCobWebBreaking = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.fast_web.tooltip")
                )
                .booleanValue(
                        "void_immunity",
                        KineticI18n.translatable("cfg.kineticcore.mech.void_immunity"),
                        () -> GeneralMechanicsConfig.enableCreativeVoidImmunity,
                        value -> GeneralMechanicsConfig.enableCreativeVoidImmunity = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.void_immunity.tooltip")
                )
                .booleanValue(
                        "no_recipe_book",
                        KineticI18n.translatable("cfg.kineticcore.mech.no_recipe_book"),
                        () -> GeneralMechanicsConfig.removeRecipeBook,
                        value -> GeneralMechanicsConfig.removeRecipeBook = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.no_recipe_book.tooltip")
                )
                .booleanValue(
                        "let_me_despawn",
                        KineticI18n.translatable("cfg.kineticcore.mech.let_me_despawn"),
                        () -> GeneralMechanicsConfig.enableLetMeDespawn,
                        value -> GeneralMechanicsConfig.enableLetMeDespawn = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.let_me_despawn.tooltip")
                )
                .entityList(
                        "despawn_whitelist",
                        KineticI18n.translatable("cfg.kineticcore.mech.despawn_whitelist"),
                        () -> GeneralMechanicsConfig.despawnWhiteList,
                        value -> GeneralMechanicsConfig.despawnWhiteList = new ArrayList<>(value),
                        List.of(),
                        KineticI18n.translatable("cfg.kineticcore.mech.despawn_whitelist.tooltip")
                )
                .booleanValue(
                        "recycle_bin",
                        KineticI18n.translatable("cfg.kineticcore.mech.recycle_bin"),
                        () -> GeneralMechanicsConfig.recycleBinWorlds,
                        value -> GeneralMechanicsConfig.recycleBinWorlds = value,
                        true,
                        KineticI18n.translatable("cfg.kineticcore.mech.recycle_bin.tooltip")
                )
                .stringList(
                        "void_damage_whitelist",
                        KineticI18n.translatable("cfg.kineticcore.mech.void_damage_whitelist"),
                        () -> GeneralMechanicsConfig.voidDamageWhiteList,
                        value -> GeneralMechanicsConfig.voidDamageWhiteList = new ArrayList<>(value),
                        List.of(),
                        KineticI18n.translatable("cfg.kineticcore.mech.void_damage_whitelist.tooltip")
                )
                .intValue(
                        "void_damage_percentage",
                        KineticI18n.translatable("cfg.kineticcore.mech.void_damage_percentage"),
                        () -> GeneralMechanicsConfig.voidDamagePercentage,
                        value -> GeneralMechanicsConfig.voidDamagePercentage = value,
                        10,
                        0,
                        100,
                        KineticI18n.translatable("cfg.kineticcore.mech.void_damage_percentage.tooltip")
                )
                .build();
    }
}
