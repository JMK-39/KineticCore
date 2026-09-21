package dev.xyat.kineticcore.feature.effects.client;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.client.effect.KineticEffectDisplay;
import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;

public class MiniEffectsFeature {

    public static final KTClientConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    public static boolean hasEffectsLeft;
    private static final KineticRegistrationBatch INITIALIZATION = new KineticRegistrationBatch();

    static {
        KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
        CLIENT = new ClientConfig(builder);
        CLIENT_SPEC = builder.build();
    }

    public static class ClientConfig {
        public final KTClientConfigSpec.BooleanValue effectsOnLeft;
        public final KTClientConfigSpec.BooleanValue requiresHoldingTab;
        public final KTClientConfigSpec.BooleanValue potionItemIcon;

        public ClientConfig(KTClientConfigSpec.Builder builder) {
            builder.comment(
                    "紧凑状态效果的客户端显示设置。",
                    "Client display settings for compact status effects."
            ).translation("cfg.kineticcore.mini_effects.title").push("MiniEffects");
            effectsOnLeft = builder.comment(
                            "在物品栏左侧显示状态效果。",
                            "Display effects on the left side of the inventory."
                    )
                    .translation("cfg.kineticcore.mini_effects.left")
                    .defineBoolean("effectsOnLeft", false);
            requiresHoldingTab = builder.comment(
                            "仅在按住 TAB 时展开状态效果。",
                            "Require holding TAB to show expanded effects."
                    )
                    .translation("cfg.kineticcore.mini_effects.hold_tab")
                    .defineBoolean("requiresHoldingTab", false);
            potionItemIcon = builder.comment(
                            "紧凑模式使用药水物品图标，而不是效果纹理。",
                            "Use potion items instead of effect textures in compact mode."
                    )
                    .translation("cfg.kineticcore.mini_effects.potion_icon")
                    .defineBoolean("potionItemIcon", false);
            builder.pop();
        }
    }

    public static void init() {
        INITIALIZATION.run(
                () -> KTClientConfigAdapter.registerSpec(
                        CLIENT_SPEC,
                        KineticRuntime.MOD_ID + "/mini_effects_client.toml"
                ),
                () -> {
                    hasEffectsLeft = KineticPlatform.isModLoaded("effectsleft");
                },
                () -> KineticEffectDisplay.configure(
                        MiniEffectsFeature::isLeftSide,
                        MiniEffectsFeature::requiresHoldingTab,
                        MiniEffectsFeature::potionItemIcon,
                        availableSpace -> availableSpace < 120
                )
        );
    }

    public static void load() {
        init();
    }

    public static boolean effectsOnLeft() {
        return CLIENT.effectsOnLeft.get();
    }

    public static void setEffectsOnLeft(boolean value) {
        CLIENT.effectsOnLeft.set(value);
    }

    public static boolean requiresHoldingTab() {
        return CLIENT.requiresHoldingTab.get();
    }

    public static void setRequiresHoldingTab(boolean value) {
        CLIENT.requiresHoldingTab.set(value);
    }

    public static boolean potionItemIcon() {
        return CLIENT.potionItemIcon.get();
    }

    public static void setPotionItemIcon(boolean value) {
        CLIENT.potionItemIcon.set(value);
    }

    public static void save() {
        CLIENT_SPEC.save();
    }

    public static boolean isLeftSide() {
        return hasEffectsLeft || CLIENT.effectsOnLeft.get();
    }
}
