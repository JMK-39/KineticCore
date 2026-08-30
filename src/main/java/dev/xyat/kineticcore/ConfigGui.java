package dev.xyat.kineticcore;

import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Backwards-friendly facade for KT's own configuration API. New dependent
 * mods may import {@link KTConfigApi} directly.
 */
@OnlyIn(Dist.CLIENT)
public final class ConfigGui {
    private ConfigGui() {
    }

    public static void register(KTConfigPage page) {
        KTConfigApi.register(page);
    }

    public static void registerClientConfig(
            String pageId,
            Component title,
            ForgeConfigSpec spec
    ) {
        KTConfigApi.registerClientConfig(pageId, title, spec);
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent);
    }

    public static Screen create(Screen parent, String pageId) {
        return KTConfigApi.createScreen(parent, pageId);
    }
}
