package dev.xyat.kineticcore.feature.flight.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.flight.api.FlightAPI;
import dev.xyat.kineticcore.feature.flight.network.FlightNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FlightEvents {
    @SubscribeEvent
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
                MutableComponent statusText = Component.translatable(noclip ? "options.on" : "options.off")
                        .withStyle(noclip ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD);

                // 构建动态按键组件（带黄色加粗样式）
                MutableComponent speedKey = Component.keybind("key.kineticcore.flying.speed.modifier")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
                MutableComponent noclipKey = Component.keybind("key.kineticcore.flying.noclip")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

                // 2. 发送动态提示消息
                // 提示第一行：微调
                player.displayClientMessage(
                        Component.literal("> ").withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("msg.kineticcore.flying.fine.tune", speedKey)),
                        false
                );

                // 提示第二行：快调
                player.displayClientMessage(
                        Component.literal("> ").withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("msg.kineticcore.flying.fast.tune", speedKey)),
                        false
                );

                // 提示第三行：穿墙状态与按键
                player.displayClientMessage(
                        Component.literal("> ").withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("msg.kineticcore.flying.noclip.status", statusText, noclipKey)),
                        false
                );
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> {
                FlightNetwork.applyServerNoclip(player, false);
                resyncFlightAbilities(player);
            });
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> {
                resyncFlightAbilities(player);
                FlightNetwork.syncNoclipState(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> {
                resyncFlightAbilities(player);
                FlightNetwork.syncNoclipState(player);
            });
        }
    }

    private static void resyncFlightAbilities(ServerPlayer player) {
        boolean forceFly = FlightAPI.shouldForceAllowFlight(player);
        boolean wasFlying = FlightAPI.getLastKnownFlying(player);
        if (forceFly || player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            if (wasFlying) player.getAbilities().flying = true;
        }
        FlightAPI.isInternalUpdate = true;
        player.onUpdateAbilities();
        FlightAPI.isInternalUpdate = false;
    }
}