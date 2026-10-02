package dev.xyat.kineticcore.internal.config.client;

import com.electronwill.nightconfig.core.CommentedConfig;
import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

/** Client settings remain readable between native binding and Forge's config load event. */
public final class KineticConfigStartupRegression {
    public static void main(String[] args) {
        var builder = KTClientConfigSpec.builder();
        var speed = builder.defineInt("speed", 2, 1, 10);
        var nativeSpec = KineticClientConfigSpecRuntime.nativeSpec(builder.build());
        check(speed.get() == 2, "construction must read the default before Forge loads the config");
        speed.set(3);
        check(speed.get() == 3, "preload edits must use the local fallback");

        var config = CommentedConfig.inMemory();
        config.set("speed", 5);
        //? if neoforge {
        /*// NeoForge corrects a config (values and comments) before handing it to the spec.
        nativeSpec.correct(config, (action, path, incorrectValue, correctedValue) -> { });
        nativeSpec.acceptConfig(loadedConfig(config));
        *///?} else {
        nativeSpec.setConfig(config);
        //?}
        check(speed.get() == 5, "loaded config must replace the startup fallback");
        speed.set(7);
        check(config.<Integer>get("speed") == 7, "loaded edits must update the native config");
        check(speed.get() == 7, "loaded reads must return the updated native value");
        System.out.println("PASS: 5 client config startup checks");
    }

    //? if neoforge {
    /*// FML only lets its own package-private LoadedConfig carry a loaded config; save() is never reached because
    // the config is corrected first.
    private static net.neoforged.fml.config.IConfigSpec.ILoadedConfig loadedConfig(CommentedConfig config) {
        try {
            var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(
                    CommentedConfig.class, java.nio.file.Path.class, net.neoforged.fml.config.ModConfig.class);
            constructor.setAccessible(true);
            return (net.neoforged.fml.config.IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("cannot create a loaded config", exception);
        }
    }

    *///?}
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
