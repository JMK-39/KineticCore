package dev.xyat.kineticcore.feature.entityfix.event;

import net.minecraftforge.common.MinecraftForge;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.LevelEvent;

public class EntityAttributeFixer {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(EntityAttributeFixer::onEntityJoinWorld);
        MinecraftForge.EVENT_BUS.addListener(EntityAttributeFixer::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(EntityAttributeFixer::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EntityAttributeFixer::onLevelLoad);
        KineticServerEvents.onPlayerLogout(EntityAttributeFixer::onPlayerLogout);
        KineticServerEvents.onPlayerChangedDimension(EntityAttributeFixer::onPlayerDimensionChange);
    }


    private static final String NBT_KEY_HP = "kt_saved_hp";

    /**
     * 核心逻辑: 仅检查并修复 NaN (非数字) 血量。
     * 0血或负数血量将不再受此逻辑干预（交由原版死亡逻辑处理）。
     */
    private static void fixGhostEntity(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide() || entity.isRemoved()) {
            return;
        }

        float currentHealth = entity.getHealth();

        // 检测 NaN (由于模组运算错误导致的非法物理值)
        if (Float.isNaN(currentHealth)) {
            if (entity instanceof Player player) {
                // 玩家 NaN 会导致客户端无法操作，强制设为 0 以触发正常死亡重生逻辑
                player.setHealth(0.0f);
            } else {
                // 普通实体 NaN 直接移除，防止其变为无法杀死的“幽灵”实体
                entity.discard();
            }
        }
    }

    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof LivingEntity livingEntity) {
            fixGhostEntity(livingEntity);

            if (livingEntity instanceof Player player) {
                restorePlayerHealth(player);
            }
        }
    }

    public static void onLivingAttack(LivingAttackEvent event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        fixGhostEntity(event.getEntity());
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        fixGhostEntity(event.getEntity());
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.getEntities().getAll().forEach(entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                fixGhostEntity(livingEntity);
            }
        });
    }

    public static void onPlayerLogout(ServerPlayer player) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        if (player.isAlive()) {
            float health = player.getHealth();
            // 确保保存的不是 NaN
            if (!Float.isNaN(health)) {
                CompoundTag data = player.getPersistentData();
                data.putFloat(NBT_KEY_HP, health);
            }
        }
    }

    private static void restorePlayerHealth(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(NBT_KEY_HP)) {
            float savedHealth = data.getFloat(NBT_KEY_HP);
            // 仅在数据合法且玩家存活时恢复
            if (!Float.isNaN(savedHealth) && player.isAlive()) {
                // 依然建议保留 0.1f 最小值，防止玩家在恢复瞬间因极低血量死亡
                player.setHealth(Math.max(savedHealth, 0.1f));
            }
            data.remove(NBT_KEY_HP);
        }
    }

    // 修复跨维度传送后的状态同步 (通过经验值微调触发同步)
    public static void onPlayerDimensionChange(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        player.giveExperiencePoints(1);
        player.giveExperiencePoints(-1);
    }
}
