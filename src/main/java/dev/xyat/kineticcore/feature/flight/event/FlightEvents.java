package dev.xyat.kineticcore.feature.flight.event;

import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraftforge.common.MinecraftForge;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.api.flight.KineticFlight;
import dev.xyat.kineticcore.feature.flight.network.FlightNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class FlightEvents {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(FlightEvents::onGameModeChange);
        KineticServerEvents.onPlayerLogin(FlightEvents::onPlayerLogin);
        KineticServerEvents.onPlayerChangedDimension((player, from, to) -> onDimensionChange(player));
        KineticServerEvents.onPlayerRespawn((player, endConquered) -> onPlayerRespawn(player));
    }

    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GameType newMode = event.getNewGameMode();
            if (newMode != GameType.CREATIVE) {
                player.server.execute(() -> FlightNetwork.applyServerNoclip(player, false));
            }
            if (newMode == GameType.CREATIVE) {
                // 1. 获取动态按键名称组件 (例如: "左 Shift" 或 "左 Ctrl")
                // 注意：这些方法只在客户端有效，但在服务端执行此 Event 时需要注意
                // 由于 getTranslatedKeyMessage 是客户端方法，
                // 我们通过语言文件的 %s 占位符和 Keybind 组件来优雅地处理

                // 穿墙状态
                boolean noclip = player.getPersistentData().getBoolean("kt_noclip");
                MutableComponent statusText = KineticI18n.translatable(
                        noclip ? "msg.kineticcore.flying.on" : "msg.kineticcore.flying.off"
                );

                // 构建动态按键组件（带黄色加粗样式）
                MutableComponent speedKey = Component.keybind("key.kineticcore.flying.speed.modifier");
                MutableComponent noclipKey = Component.keybind("key.kineticcore.flying.noclip");

                // 2. 发送动态提示消息
                // 提示第一行：微调
                player.displayClientMessage(
                        KineticI18n.translatable("msg.kineticcore.flying.fine.tune", speedKey),
                        false
                );

                // 提示第二行：快调
                player.displayClientMessage(
                        KineticI18n.translatable("msg.kineticcore.flying.fast.tune", speedKey),
                        false
                );

                // 提示第三行：穿墙状态与按键
                player.displayClientMessage(
                        KineticI18n.translatable("msg.kineticcore.flying.noclip.status", statusText, noclipKey),
                        false
                );
            }
        }
    }

    public static void onPlayerLogin(ServerPlayer player) {
        player.server.execute(() -> {
            FlightNetwork.applyServerNoclip(player, false);
            resyncFlightAbilities(player);
        });
    }

    public static void onDimensionChange(ServerPlayer player) {
        player.server.execute(() -> {
            resyncFlightAbilities(player);
            FlightNetwork.syncNoclipState(player);
        });
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        player.server.execute(() -> {
            resyncFlightAbilities(player);
            FlightNetwork.syncNoclipState(player);
        });
    }

    private static void resyncFlightAbilities(ServerPlayer player) {
        boolean forceFly = KineticFlight.isFlightAllowed(player);
        boolean wasFlying = KineticFlight.lastKnownFlying(player);
        if (forceFly || player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            if (wasFlying) player.getAbilities().flying = true;
        }
        KineticFlight.isInternalUpdate = true;
        player.onUpdateAbilities();
        KineticFlight.isInternalUpdate = false;
    }
}
