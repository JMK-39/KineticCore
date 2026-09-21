package dev.xyat.kineticcore.bootstrap.client;

import dev.xyat.kineticcore.feature.worldmanagement.AsyncWorldDeleter;
import dev.xyat.kineticcore.feature.worldmanagement.client.NotificationOverlay;
import dev.xyat.kineticcore.feature.mining.event.MiningInputHandler;
import dev.xyat.kineticcore.feature.mining.client.MiningModeClient;
import dev.xyat.kineticcore.feature.flight.client.FlightClient;
import dev.xyat.kineticcore.feature.flight.client.FlightAttributeTooltipHandler;
import dev.xyat.kineticcore.feature.flight.config.SuperFlightClientConfig;
import dev.xyat.kineticcore.feature.farmland.client.FarmlandTooltipHandler;
import dev.xyat.kineticcore.feature.copyitem.client.ItemCopyManager;
import dev.xyat.kineticcore.feature.attribute.event.AttributeFixHandler;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.config.client.StartupFeatureConfigGui;
import dev.xyat.kineticcore.bootstrap.config.client.KineticUiConfigGui;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScrollSettings;
import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.feature.attribute.config.AttributeConfigGui;
import dev.xyat.kineticcore.feature.crawl.client.PlayerCrawlHandler;
import dev.xyat.kineticcore.feature.datapack.ResourcePackReloadNotifier;
import dev.xyat.kineticcore.feature.datapack.config.PackConfigGui;
import dev.xyat.kineticcore.feature.defaultoptions.config.DefaultOptionsConfigGui;
import dev.xyat.kineticcore.feature.defaultoptions.OptionsManager;
import dev.xyat.kineticcore.feature.effects.client.MiniEffectsFeature;
import dev.xyat.kineticcore.feature.effects.config.MiniEffectsConfigGui;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfigGui;
import dev.xyat.kineticcore.feature.fps.client.FpsRenderer;
import dev.xyat.kineticcore.feature.fps.config.FpsClientConfig;
import dev.xyat.kineticcore.feature.fps.config.FpsConfigGui;
import dev.xyat.kineticcore.api.client.gpu.KineticGpuCleanup;
import dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfigGui;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfigGui;
import dev.xyat.kineticcore.feature.networklimit.config.NetworkConfigGui;
import dev.xyat.kineticcore.feature.setspawn.config.SetSpawnConfigGui;
import dev.xyat.kineticcore.feature.spawnegg.client.SpawnEggClient;
import dev.xyat.kineticcore.feature.spawnegg.config.SpawnEggConfigGui;
import dev.xyat.kineticcore.feature.startup.client.StartupClientModule;
import dev.xyat.kineticcore.feature.startup.config.StartupConfig;
import dev.xyat.kineticcore.feature.startup.config.StartupConfigGui;
import dev.xyat.kineticcore.feature.tps.client.TpsRenderer;
import dev.xyat.kineticcore.feature.tps.config.TpsClientConfig;
import dev.xyat.kineticcore.feature.tps.config.TpsConfigGui;
import dev.xyat.kineticcore.feature.worldinit.config.WorldInitConfigGui;

public final class KineticCoreClientBootstrap {
    private static final KineticRegistrationBatch INFRASTRUCTURE = new KineticRegistrationBatch();
    private static final KineticRegistrationBatch MODULES = new KineticRegistrationBatch();

    private KineticCoreClientBootstrap() {
    }

    public static void registerInfrastructure() {
        INFRASTRUCTURE.run(
                () -> KTClientConfigAdapter.registerSpec(TpsClientConfig.SPEC, "kineticcore/tps_client.toml"),
                () -> KTClientConfigAdapter.registerSpec(FpsClientConfig.SPEC, "kineticcore/fps_client.toml"),
                () -> KTClientConfigAdapter.registerSpec(StartupConfig.SPEC, "kineticcore/startup.toml"),
                () -> KTClientConfigAdapter.registerSpec(SuperFlightClientConfig.SPEC, "kineticcore/super_flight_client.toml"),
                () -> KTClientConfigAdapter.registerSpec(KineticScrollSettings.configSpec(), "kineticcore/ui_client.toml"),
                KineticCoreConfigKeyBinding::register
        );
    }

    public static void registerModules() {
        MODULES.run(
                OptionsManager::registerHook,
                AsyncWorldDeleter::registerHook,
                ResourcePackReloadNotifier::registerHook,
                AttributeFixHandler::registerClient,
                ItemCopyManager::register,
                FarmlandTooltipHandler::register,
                FlightClient::register,
                FlightAttributeTooltipHandler::register,
                MiningModeClient::register,
                MiningInputHandler::register,
                NotificationOverlay::register,
                StartupFeatureConfigGui::load,
                KineticUiConfigGui::load,
                AttributeConfigGui::register,
                PlayerCrawlHandler::load,
                PackConfigGui::load,
                DefaultOptionsConfigGui::load,
                MiniEffectsFeature::load,
                MiniEffectsConfigGui::load,
                PlayerConfigGui::load,
                FpsRenderer::register,
                FpsConfigGui::load,
                KineticGpuCleanup::register,
                LogCleanerConfigGui::load,
                GeneralMechanicsConfigGui::load,
                NetworkConfigGui::load,
                SetSpawnConfigGui::load,
                SpawnEggClient::register,
                SpawnEggConfigGui::load,
                StartupClientModule::register,
                StartupConfigGui::load,
                TpsRenderer::register,
                TpsConfigGui::load,
                WorldInitConfigGui::load,
                () -> KTConfigApi.installConfigHub(KineticCore.MODID)
        );
    }
}
