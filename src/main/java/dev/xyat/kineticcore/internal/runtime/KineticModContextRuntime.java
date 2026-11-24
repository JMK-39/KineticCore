package dev.xyat.kineticcore.internal.runtime;

import dev.xyat.kineticcore.api.runtime.KineticPaths;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
//? if forge
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * The mod whose constructor is currently running. Content an addon registers through the Kinetic API
 * goes onto that addon's own mod event bus and config set, not KineticCore's.
 */
public final class KineticModContextRuntime {
    private KineticModContextRuntime() {
    }

    public static IEventBus modEventBus() {
        //? if forge {
        var context = FMLJavaModLoadingContext.get();
        IEventBus bus = context != null ? context.getModEventBus() : null;
        //?} else {
        /*var container = ModLoadingContext.get().getActiveContainer();
        IEventBus bus = container != null ? container.getEventBus() : null;
        *///?}
        if (bus == null) {
            throw new IllegalStateException("The mod event bus is only available while a mod is being constructed");
        }
        return bus;
    }

    public static void registerConfig(ModConfig.Type type, ForgeConfigSpec spec, String fileName) {
        var path = java.nio.file.Path.of(fileName);
        if (type != ModConfig.Type.SERVER && path.getNameCount() == 3
                && path.getName(0).toString().equals("kineticcore") && !path.isAbsolute()) {
            // Updated specs opt in by declaring their owner directory. Old add-ons still read their old paths.
            KineticPaths.migrateConfigFile("kineticcore/" + path.getFileName(), fileName);
            try {
                KineticPaths.ensureConfigDirectory(path.getParent().toString());
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException("Cannot create config directory for " + fileName, failure);
            }
        }
        //? if forge {
        ModLoadingContext.get().registerConfig(type, spec, fileName);
        //?} else {
        /*ModLoadingContext.get().getActiveContainer().registerConfig(type, spec, fileName);
        *///?}
    }
}
