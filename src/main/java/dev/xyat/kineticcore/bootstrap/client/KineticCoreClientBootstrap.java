package dev.xyat.kineticcore.bootstrap.client;

import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.feature.fps.config.FpsClientConfig;
import dev.xyat.kineticcore.feature.startup.config.StartupConfig;
import dev.xyat.kineticcore.feature.tps.config.TpsClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@OnlyIn(Dist.CLIENT)
public final class KineticCoreClientBootstrap {
    private KineticCoreClientBootstrap() {
    }

    public static void beforeModuleScan(FMLJavaModLoadingContext context, IEventBus modEventBus) {
        context.registerConfig(ModConfig.Type.CLIENT, TpsClientConfig.SPEC, "kineticcore/tps_client.toml");
        context.registerConfig(ModConfig.Type.CLIENT, FpsClientConfig.SPEC, "kineticcore/fps_client.toml");
        context.registerConfig(ModConfig.Type.CLIENT, StartupConfig.SPEC, "kineticcore/startup.toml");
        KineticCoreConfigKeyBinding.register(modEventBus);
    }

    public static void afterModuleScan(ModContainer modContainer) {
        KTConfigApi.installForgeConfigHub(modContainer);
    }
}
