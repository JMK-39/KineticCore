package dev.xyat.kineticcore.feature.tps.mixin;

import dev.xyat.kineticcore.feature.tps.logic.ITpsServer;
import dev.xyat.kineticcore.feature.tps.logic.TpsTracker;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class ServerMixin implements ITpsServer {
    @Unique
    private final TpsTracker kineticcore$tpsTracker = new TpsTracker();

    @Override
    public TpsTracker kineticcore$getTpsTracker() {
        return kineticcore$tpsTracker;
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void kineticcore$recordTickTime(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        int lastIndex = (server.getTickCount() - 1 + 100) % 100;
        kineticcore$tpsTracker.addTick(server.tickTimes[lastIndex]);
    }
}
