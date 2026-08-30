package dev.xyat.kineticcore.feature.firstjoin.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = KineticCore.MODID)
public class FirstJoinHandler {

    private static final String NBT_KEY = "kineticcore:first_join_received";
    private static final String PENDING_NBT_KEY = "kineticcore:first_join_pending";
    private static final String DATA_NAME = "kineticcore_first_join_received";
    private static final Map<UUID, Integer> PENDING_REWARDS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!PlayerConfig.enableFirstJoin || event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        CompoundTag persistentData = player.getPersistentData();
        FirstJoinRewardData rewardData = getRewardData(player.server);

        if (rewardData.hasReceived(uuid) || persistentData.getBoolean(NBT_KEY)) {
            markReceived(player, rewardData);
            return;
        }

        boolean pending = rewardData.isPending(uuid) || persistentData.getBoolean(PENDING_NBT_KEY);
        if (!pending && hasExistingPlayerState(player)) {
            markReceived(player, rewardData);
            return;
        }

        markPending(player, rewardData);
        scheduleOrGrant(player, rewardData);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_REWARDS.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_REWARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;

            if (ticksLeft <= 0) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null && player.isAlive()) {
                    grantAndMark(player, getRewardData(event.getServer()));
                }
                iterator.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }
    }

    private static void scheduleOrGrant(ServerPlayer player, FirstJoinRewardData rewardData) {
        int delay = Math.max(0, PlayerConfig.firstJoinDelay);
        if (delay > 0) {
            PENDING_REWARDS.put(player.getUUID(), delay);
            return;
        }

        grantAndMark(player, rewardData);
    }

    private static void grantAndMark(ServerPlayer player, FirstJoinRewardData rewardData) {
        try {
            grantRewards(player);
            markReceived(player, rewardData);
            PENDING_REWARDS.remove(player.getUUID());
        } catch (Throwable throwable) {
            KineticCore.LOGGER.error("首次进服奖励发放失败，保留待发放状态: {}", player.getGameProfile().getName(), throwable);
            markPending(player, rewardData);
        }
    }

    private static void grantRewards(ServerPlayer player) {
        if (PlayerConfig.clearInvBeforeJoin) {
            player.getInventory().clearContent();
        }

        PlayerConfig.getJoinItems().forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                if (slot >= 0 && slot < player.getInventory().items.size()) {
                    ItemStack existing = player.getInventory().getItem(slot);
                    if (existing.isEmpty()) {
                        player.getInventory().setItem(slot, copy);
                    } else {
                        player.getInventory().add(copy);
                    }
                } else {
                    player.getInventory().add(copy);
                }
            }
        });

        PlayerConfig.getArmor().forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                player.setItemSlot(slot, stack.copy());
            }
        });

        if (!PlayerConfig.firstJoinCommands.isEmpty()) {
            CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(2)
                    .withSuppressedOutput();

            for (String cmd : PlayerConfig.firstJoinCommands) {
                try {
                    String parsedCmd = cmd.replace("@s", player.getScoreboardName())
                            .replace("@player", player.getScoreboardName())
                            .trim();
                    while (parsedCmd.startsWith("/")) {
                        parsedCmd = parsedCmd.substring(1).trim();
                    }
                    if (!parsedCmd.isEmpty()) {
                        player.server.getCommands().performPrefixedCommand(source, parsedCmd);
                    }
                } catch (Exception e) {
                    KineticCore.LOGGER.error("首次进服指令执行失败: {}", cmd, e);
                }
            }
        }

        player.inventoryMenu.broadcastChanges();
    }

    private static boolean hasExistingPlayerState(ServerPlayer player) {
        int playTicks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        if (playTicks > 20) return true;
        if (player.totalExperience > 0) return true;
        if (player.experienceLevel > 0) return true;
        if (player.getHealth() < player.getMaxHealth()) return true;
        return hasAnyInventoryItem(player);
    }

    private static boolean hasAnyInventoryItem(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) return true;
        }

        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty()) return true;
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty()) return true;
        }

        return false;
    }

    private static void markPending(ServerPlayer player, FirstJoinRewardData rewardData) {
        player.getPersistentData().putBoolean(PENDING_NBT_KEY, true);
        rewardData.markPending(player.getUUID());
    }

    private static void markReceived(ServerPlayer player, FirstJoinRewardData rewardData) {
        player.getPersistentData().putBoolean(NBT_KEY, true);
        player.getPersistentData().remove(PENDING_NBT_KEY);
        rewardData.markReceived(player.getUUID());
    }

    private static FirstJoinRewardData getRewardData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FirstJoinRewardData::load, FirstJoinRewardData::new, DATA_NAME);
    }

    private static final class FirstJoinRewardData extends SavedData {
        private final Set<UUID> receivedPlayers = new HashSet<>();
        private final Set<UUID> pendingPlayers = new HashSet<>();

        private static FirstJoinRewardData load(CompoundTag tag) {
            FirstJoinRewardData data = new FirstJoinRewardData();
            loadUuidSet(tag.getList("players", Tag.TAG_STRING), data.receivedPlayers);
            loadUuidSet(tag.getList("pending", Tag.TAG_STRING), data.pendingPlayers);
            data.pendingPlayers.removeAll(data.receivedPlayers);
            return data;
        }

        private static void loadUuidSet(ListTag list, Set<UUID> target) {
            for (int i = 0; i < list.size(); i++) {
                try {
                    target.add(UUID.fromString(list.getString(i)));
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
            tag.put("players", saveUuidSet(receivedPlayers));
            tag.put("pending", saveUuidSet(pendingPlayers));
            return tag;
        }

        private static ListTag saveUuidSet(Set<UUID> source) {
            ListTag list = new ListTag();
            for (UUID uuid : source) {
                list.add(StringTag.valueOf(uuid.toString()));
            }
            return list;
        }

        private boolean hasReceived(UUID uuid) {
            return receivedPlayers.contains(uuid);
        }

        private boolean isPending(UUID uuid) {
            return pendingPlayers.contains(uuid);
        }

        private void markPending(UUID uuid) {
            if (!receivedPlayers.contains(uuid) && pendingPlayers.add(uuid)) {
                setDirty();
            }
        }

        private void markReceived(UUID uuid) {
            boolean changed = receivedPlayers.add(uuid);
            changed |= pendingPlayers.remove(uuid);
            if (changed) {
                setDirty();
            }
        }
    }
}
