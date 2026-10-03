package dev.xyat.kineticcore.feature.firstjoin.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 首次进入奖励。只有同时满足以下两点才会发放：
 * <ol>
 *     <li>玩家没有“已领取”标记（存档数据或玩家 NBT 任一处有标记即视为已领取）；</li>
 *     <li>玩家在这个世界的游玩时间不超过 1 分钟（原版统计 PLAY_TIME）。</li>
 * </ol>
 * 任一条件不满足就既不清理也不发放。发放时只清空快捷栏（9 格），背包与装备不动；
 * 奖励指定的槽位上已有物品时直接替换为奖励。只有全部奖励成功发放后才打上标记：配置中有物品解析失败时
 * 本次既不清理也不发放，修好配置后玩家在游玩 1 分钟内重新进入仍可领取。
 */
public class FirstJoinHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticServerEvents.onPlayerLogin(KineticEventPriority.NORMAL, FirstJoinHandler::onPlayerLogin),
                () -> KineticServerEvents.onTick(KineticEventPriority.NORMAL, KineticServerEvents.TickPhase.END, FirstJoinHandler::onServerTick)
        );
    }

    /** 游玩时间上限：超过 1 分钟（1200 tick）的玩家不再发放。 */
    private static final int MAX_PLAY_TICKS = 1200;
    private static final String NBT_KEY = "kineticcore:first_join_received";
    private static final String LEGACY_PENDING_NBT_KEY = "kineticcore:first_join_pending";
    private static final String DATA_NAME = "kineticcore_first_join_received";
    private static final Map<UUID, Integer> PENDING_REWARDS = new ConcurrentHashMap<>();

    public static void onPlayerLogin(ServerPlayer player) {
        if (!PlayerConfig.enableFirstJoin) return;
        if (!isEligible(player)) return;

        int delay = Math.max(0, PlayerConfig.firstJoinDelay);
        if (delay > 0) {
            PENDING_REWARDS.put(player.getUUID(), delay);
        } else {
            grantAndMark(player);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (PENDING_REWARDS.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_REWARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft > 0) {
                entry.setValue(ticksLeft);
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                // 延迟期间离开：下次进入时按游玩时间重新判断。
                iterator.remove();
            } else if (!player.isAlive()) {
                entry.setValue(1);
            } else {
                iterator.remove();
                grantAndMark(player);
            }
        }
    }

    /** 没有标记，且在这个世界游玩不超过 1 分钟。 */
    private static boolean isEligible(ServerPlayer player) {
        if (hasReceived(player)) return false;
        return playTicks(player) <= MAX_PLAY_TICKS;
    }

    private static int playTicks(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
    }

    private static boolean hasReceived(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NBT_KEY) || getRewardData(player.level().getServer()).hasReceived(player.getUUID());
    }

    private static void grantAndMark(ServerPlayer player) {
        // 延迟期间可能已被其它途径标记（例如同一玩家重复登录事件）。
        if (hasReceived(player)) return;
        // 配置有误时整份奖励都不发，也不清理快捷栏，避免玩家只拿到一部分却被标记为已领取。
        if (!PlayerConfig.rewardsValid()) {
            KineticRuntime.logger().error("首次进服奖励配置中有物品解析失败，本次不发放也不标记: {}", player.getGameProfile().getName());
            return;
        }
        boolean granted;
        try {
            granted = grantRewards(player);
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("首次进服奖励发放失败: {}", player.getGameProfile().getName(), throwable);
            granted = false;
        }
        // 只有成功发放才打标记。
        if (granted) markReceived(player);
    }

    private static boolean grantRewards(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        if (PlayerConfig.clearInvBeforeJoin) {
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        PlayerConfig.getJoinItems().forEach((slot, stack) -> {
            if (stack.isEmpty()) return;
            ItemStack copy = stack.copy();
            if (slot >= 0 && slot < inventory.items.size()) {
                // 指定槽位：已有物品直接替换为奖励。
                inventory.setItem(slot, copy);
            } else if (!inventory.add(copy) && !copy.isEmpty()) {
                player.drop(copy, false);
            }
        });

        PlayerConfig.getArmor().forEach((slot, stack) -> {
            if (!stack.isEmpty()) player.setItemSlot(slot, stack.copy());
        });

        boolean commandsRan = runCommands(player);
        player.inventoryMenu.broadcastChanges();
        return commandsRan;
    }

    // Returns whether every command ran without throwing.
    private static boolean runCommands(ServerPlayer player) {
        if (PlayerConfig.firstJoinCommands.isEmpty()) return true;
        boolean allRan = true;
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        for (String cmd : PlayerConfig.firstJoinCommands) {
            try {
                String parsedCmd = cmd.replace("@s", player.getScoreboardName())
                        .replace("@player", player.getScoreboardName())
                        .trim();
                while (parsedCmd.startsWith("/")) {
                    parsedCmd = parsedCmd.substring(1).trim();
                }
                if (!parsedCmd.isEmpty()) {
                    player.level().getServer().getCommands().performPrefixedCommand(source, parsedCmd);
                }
            } catch (Exception e) {
                KineticRuntime.logger().error("首次进服指令执行失败: {}", cmd, e);
                allRan = false;
            }
        }
        return allRan;
    }

    private static void markReceived(ServerPlayer player) {
        player.getPersistentData().putBoolean(NBT_KEY, true);
        player.getPersistentData().remove(LEGACY_PENDING_NBT_KEY);
        getRewardData(player.level().getServer()).markReceived(player.getUUID());
    }

    private static FirstJoinRewardData getRewardData(MinecraftServer server) {
        //? if >=26.1 {
        /*return server.overworld().getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedDataType<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kineticcore", DATA_NAME), FirstJoinRewardData::new,
                CompoundTag.CODEC.xmap(FirstJoinRewardData::load, data -> data.save(new CompoundTag()))));
        *///?} else if >=1.20.5 {
        /*return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(FirstJoinRewardData::new, (tag, registries) -> FirstJoinRewardData.load(tag)), DATA_NAME);
        *///?} else {
        return server.overworld().getDataStorage().computeIfAbsent(FirstJoinRewardData::load, FirstJoinRewardData::new, DATA_NAME);
        //?}
    }

    private static final class FirstJoinRewardData extends SavedData {
        private final Set<UUID> receivedPlayers = new HashSet<>();

        private static FirstJoinRewardData load(CompoundTag tag) {
            FirstJoinRewardData data = new FirstJoinRewardData();
            ListTag list = tag.getList("players", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    data.receivedPlayers.add(UUID.fromString(list.getString(i)));
                } catch (Exception ignored) {
                }
            }
            return data;
        }


        //? if <26.1
        @Override
        //? if >=1.20.5 <26.1 {
        /*public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull net.minecraft.core.HolderLookup.Provider registries) {
        *///?} else {
        public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        //?}
            ListTag list = new ListTag();
            for (UUID uuid : receivedPlayers) {
                list.add(StringTag.valueOf(uuid.toString()));
            }
            tag.put("players", list);
            return tag;
        }

        private boolean hasReceived(UUID uuid) {
            return receivedPlayers.contains(uuid);
        }

        private void markReceived(UUID uuid) {
            if (receivedPlayers.add(uuid)) setDirty();
        }
    }
}
