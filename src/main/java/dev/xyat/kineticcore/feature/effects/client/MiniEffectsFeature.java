package dev.xyat.kineticcore.feature.effects.client;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@KTClientModule
public class MiniEffectsFeature {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    public static boolean hasEffectsLeft;
    private static boolean initialized;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new ClientConfig(builder);
        CLIENT_SPEC = builder.build();
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue effectsOnLeft;
        public final ForgeConfigSpec.BooleanValue requiresHoldingTab;
        public final ForgeConfigSpec.BooleanValue potionItemIcon;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment(
                    "紧凑状态效果的客户端显示设置。",
                    "Client display settings for compact status effects."
            ).translation("cfg.kineticcore.mini_effects.title").push("MiniEffects");
            effectsOnLeft = builder.comment(
                            "在物品栏左侧显示状态效果。",
                            "Display effects on the left side of the inventory."
                    )
                    .translation("cfg.kineticcore.mini_effects.left")
                    .define("effectsOnLeft", false);
            requiresHoldingTab = builder.comment(
                            "仅在按住 TAB 时展开状态效果。",
                            "Require holding TAB to show expanded effects."
                    )
                    .translation("cfg.kineticcore.mini_effects.hold_tab")
                    .define("requiresHoldingTab", false);
            potionItemIcon = builder.comment(
                            "紧凑模式使用药水物品图标，而不是效果纹理。",
                            "Use potion items instead of effect textures in compact mode."
                    )
                    .translation("cfg.kineticcore.mini_effects.potion_icon")
                    .define("potionItemIcon", false);
            builder.pop();
        }
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                CLIENT_SPEC,
                KineticCore.MODID + "/mini_effects_client.toml"
        );
        hasEffectsLeft = ModList.get().isLoaded("effectsleft");
        MinecraftForge.EVENT_BUS.register(new MiniEffectsFeature());
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

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPotionSizeEvent(ScreenEvent.RenderInventoryMobEffects event) {
        event.setCompact(event.getAvailableSpace() < 120);
    }
}
