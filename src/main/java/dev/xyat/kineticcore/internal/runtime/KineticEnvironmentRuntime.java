package dev.xyat.kineticcore.internal.runtime;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public final class KineticEnvironmentRuntime {
    private KineticEnvironmentRuntime() {
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public static boolean isDedicatedServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    public static void runOnClient(Supplier<Runnable> action) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, action);
    }

    public static <T> T callOnClient(Supplier<Supplier<T>> action, T fallback) {
        if (FMLEnvironment.dist != Dist.CLIENT) return fallback;
        T value = action.get().get();
        return value != null ? value : fallback;
    }

    public static void runOnDedicatedServer(Supplier<Runnable> action) {
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, action);
    }
}
