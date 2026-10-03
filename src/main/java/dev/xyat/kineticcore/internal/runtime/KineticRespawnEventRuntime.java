package dev.xyat.kineticcore.internal.runtime;

//? if neoforge && <26.1 {
/*import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;

// Before 1.21 PlayerList.respawn fell back to MinecraftServer.overworld() itself, and SetSpawnMixins redirects that
// call. Since 1.21 the fallback happens in ServerPlayer, which the End exit portal uses as well, so NeoForge's respawn
// event takes its place: respawns without a usable respawn block go to the custom spawn level instead.
@EventBusSubscriber(modid = "kineticcore")
public final class KineticRespawnEventRuntime {
    private KineticRespawnEventRuntime() {
    }

    @SubscribeEvent
    public static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (event.isFromEndFight()) {
            KineticServerHookRuntime.clearPendingRespawnPlacement();
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DimensionTransition transition = event.getDimensionTransition();
        if (player.getRespawnPosition() != null && !transition.missingRespawnBlock()) return;
        KineticServerHookRuntime.selectRespawnLevel(player.level().getServer()).ifPresent(level -> event.setDimensionTransition(
                new DimensionTransition(level, transition.pos(), transition.speed(), transition.yRot(), transition.xRot(),
                        transition.missingRespawnBlock(), transition.postDimensionTransition())));
    }
}
*///?}
//? if neoforge && >=26.1 {
/*import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;

// Respawns without a usable respawn point go to the custom spawn level; NeoForge keeps position and angle when the
// level changes.
@EventBusSubscriber(modid = "kineticcore")
public final class KineticRespawnEventRuntime {
    private KineticRespawnEventRuntime() {
    }

    @SubscribeEvent
    public static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (event.isFromEndFight()) {
            KineticServerHookRuntime.clearPendingRespawnPlacement();
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getRespawnConfig() != null && !event.getTeleportTransition().missingRespawnBlock()) return;
        KineticServerHookRuntime.selectRespawnLevel(player.level().getServer())
                .ifPresent(level -> event.setRespawnLevel(level.dimension()));
    }
}
*///?}
