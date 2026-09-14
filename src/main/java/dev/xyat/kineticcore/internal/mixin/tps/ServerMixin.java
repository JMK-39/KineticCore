package dev.xyat.kineticcore.internal.mixin.tps;

import dev.xyat.kineticcore.api.monitoring.ServerTickTracker;
import dev.xyat.kineticcore.internal.monitoring.ServerPerformanceAccess;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class ServerMixin implements ServerPerformanceAccess {
    @Unique
    private final ServerTickTracker kineticcore$tpsTracker = new ServerTickTracker();

    @Override
    public ServerTickTracker kineticcore$getTickTracker() {
        return kineticcore$tpsTracker;
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void kineticcore$recordTickTime(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        int lastIndex = (server.getTickCount() - 1 + 100) % 100;
        kineticcore$tpsTracker.addTick(server.tickTimes[lastIndex]);
    }
}
