package dev.xyat.kineticcore.feature.flight.api;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FlightAPI {
    private static final String NBT_FLIGHT_SOURCES = "flight_sources_list";
    private static final String NBT_LAST_FLYING = "last_known_flying";

    // --- 三大核心拦截状态标记 ---
    public static boolean isProcessingExplicitCancel = false; // 玩家双击空格
    public static boolean isInternalUpdate = false;           // 我们自己的系统强制发包
    public static boolean isGamemodeSwitching = false;        // 正在执行 /gamemode 切换逻辑

    // 内存级防抖记录仪
    private static final Set<UUID> debouncingPlayers = new HashSet<>();

    public static boolean isDebouncing(Player player) {
        return debouncingPlayers.contains(player.getUUID());
    }

    public static void setDebouncing(Player player, boolean value) {
        if (value) debouncingPlayers.add(player.getUUID());
        else debouncingPlayers.remove(player.getUUID());
    }

    // ================== 内部源列表管理 ==================
    public static Set<String> getInternalFlightSources(Player player) {
        Set<String> sources = new HashSet<>();
        ListTag list = player.getPersistentData().getList(NBT_FLIGHT_SOURCES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            sources.add(list.getString(i));
        }
        return sources;
    }

    public static void addFlightSource(LivingEntity entity, String sourceKey) {
        if (!(entity instanceof Player player)) return;
        Set<String> sources = getInternalFlightSources(player);
        if (sources.add(sourceKey)) {
            saveSources(player, sources);
            updateServerFlightState(player);
        }
    }

    public static void removeFlightSource(LivingEntity entity, String sourceKey) {
        if (!(entity instanceof Player player)) return;
        Set<String> sources = getInternalFlightSources(player);
        if (sources.remove(sourceKey)) {
            saveSources(player, sources);
            updateServerFlightState(player);
        }
    }

    private static void saveSources(Player player, Set<String> sources) {
        ListTag list = new ListTag();
        for (String s : sources) list.add(StringTag.valueOf(s));
        player.getPersistentData().put(NBT_FLIGHT_SOURCES, list);
    }

    // ================== 客户端姿态记忆 ==================
    public static void setLastKnownFlying(Player player, boolean isFlying) {
        player.getPersistentData().putBoolean(NBT_LAST_FLYING, isFlying);
    }

    public static boolean getLastKnownFlying(Player player) {
        return player.getPersistentData().getBoolean(NBT_LAST_FLYING);
    }

    // ================== 核心判定与执行 ==================
    public static boolean shouldForceAllowFlight(Player player) {
        return player.isCreative() || player.isSpectator() || !getInternalFlightSources(player).isEmpty();
    }

    public static void updateServerFlightState(Player player) {
        if (player.level().isClientSide) return;

        if (shouldForceAllowFlight(player)) {
            player.getAbilities().mayfly = true;
            FlightAPI.isInternalUpdate = true;
            player.onUpdateAbilities();
            FlightAPI.isInternalUpdate = false;
        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                FlightAPI.isInternalUpdate = true;
                player.onUpdateAbilities();
                FlightAPI.isInternalUpdate = false;
            }
        }
    }
}