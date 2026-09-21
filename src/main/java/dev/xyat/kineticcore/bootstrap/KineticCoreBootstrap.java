package dev.xyat.kineticcore.bootstrap;

import dev.xyat.kineticcore.feature.attribute.event.AttributeFixHandler;
import dev.xyat.kineticcore.api.flight.KineticFlightAttributes;
import dev.xyat.kineticcore.feature.worldinit.event.WorldInitHandler;
import dev.xyat.kineticcore.feature.voiddamage.event.VoidDamageEvent;
import dev.xyat.kineticcore.feature.tps.logic.TpsHudManager;
import dev.xyat.kineticcore.feature.pvp.event.PvpEventHandler;
import dev.xyat.kineticcore.feature.farmland.event.FarmlandProtectionHandler;
import dev.xyat.kineticcore.feature.food.event.FoodAndToolTweaks;
import dev.xyat.kineticcore.feature.flight.event.FlightEvents;
import dev.xyat.kineticcore.feature.firstjoin.event.FirstJoinHandler;
import dev.xyat.kineticcore.feature.experience.event.XPDropHandler;
import dev.xyat.kineticcore.feature.entityfix.event.EntityAttributeFixer;
import dev.xyat.kineticcore.feature.creativeimmunity.event.GeneralEvents;
import dev.xyat.kineticcore.feature.cobweb.event.AxesEventHandler;
import dev.xyat.kineticcore.feature.bee.event.BeeSizeHandler;
import dev.xyat.kineticcore.bootstrap.command.KineticCoreCommandExtension;
import dev.xyat.kineticcore.api.hook.CommonHooks;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.feature.crawl.event.CrawlingStateHandler;
import dev.xyat.kineticcore.feature.despawn.LetMeDespawnLogic;
import dev.xyat.kineticcore.feature.crawl.network.PlayerNetwork;
import dev.xyat.kineticcore.feature.datapack.PackModule;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import dev.xyat.kineticcore.feature.flight.network.FlightNetwork;
import dev.xyat.kineticcore.feature.logcleaner.LogCleanerModule;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import dev.xyat.kineticcore.feature.mining.network.MiningModeNetwork;
import dev.xyat.kineticcore.feature.nbt.network.NbtNetwork;
import dev.xyat.kineticcore.feature.networklimit.config.NetworkConfig;
import dev.xyat.kineticcore.feature.pvp.network.PvpNetwork;
import dev.xyat.kineticcore.feature.setspawn.config.SetSpawnConfig;
import dev.xyat.kineticcore.feature.setspawn.event.SetSpawnHandler;
import dev.xyat.kineticcore.feature.setspawn.network.SetSpawnNetwork;
import dev.xyat.kineticcore.feature.spawnegg.SpawnEggInit;
import dev.xyat.kineticcore.feature.spawnegg.config.SpawnEggConfig;
import dev.xyat.kineticcore.feature.spawnegg.event.ThrowSpawnEggEvent;
import dev.xyat.kineticcore.feature.spawnegg.network.SpawnEggNetwork;
import dev.xyat.kineticcore.feature.tps.network.TpsNetwork;
import dev.xyat.kineticcore.feature.worldinit.config.WorldInitConfig;

public final class KineticCoreBootstrap {
    private static final KineticRegistrationBatch REGISTRATIONS = new KineticRegistrationBatch();

    private KineticCoreBootstrap() {
    }

    public static void register() {
        REGISTRATIONS.run(
                KineticFlightAttributes::register,
                PlayerNetwork::register,
                FlightNetwork::register,
                MiningModeNetwork::register,
                NbtNetwork::register,
                PvpNetwork::register,
                SetSpawnNetwork::register,
                SpawnEggNetwork::register,
                TpsNetwork::register,
                KineticCoreCommandExtension::register,
                SpawnEggInit::register,
                PackModule::register,
                AttributeFixHandler::register,
                CrawlingStateHandler::load,
                PackModule::load,
                PlayerConfig::load,
                LogCleanerModule::load,
                GeneralMechanicsConfig::load,
                () -> CommonHooks.onRecipeBookRemoval(() -> GeneralMechanicsConfig.removeRecipeBook),
                LetMeDespawnLogic::registerHooks,
                NetworkConfig::load,
                SetSpawnConfig::load,
                SetSpawnHandler::registerHooks,
                SpawnEggConfig::load,
                ThrowSpawnEggEvent::register,
                WorldInitConfig::load,
                BeeSizeHandler::register,
                AxesEventHandler::register,
                GeneralEvents::register,
                EntityAttributeFixer::register,
                XPDropHandler::register,
                FirstJoinHandler::register,
                FlightEvents::register,
                FoodAndToolTweaks::register,
                FarmlandProtectionHandler::register,
                PvpEventHandler::register,
                TpsHudManager::register,
                VoidDamageEvent::register,
                WorldInitHandler::register
        );
    }
}
