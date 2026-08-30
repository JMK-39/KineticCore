package dev.xyat.kineticcore.feature.entityfix.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityAttributeFixer {

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

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!GeneralMechanicsConfig.enableEntityAttributeFixer) return;
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof LivingEntity livingEntity) {
            fixGhostEntity(livingEntity);

            if (livingEntity instanceof Player player) {
                restorePlayerHealth(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!GeneralMechanicsConfig.enableEntityAttributeFixer) return;
        fixGhostEntity(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!GeneralMechanicsConfig.enableEntityAttributeFixer) return;
        fixGhostEntity(event.getEntity());
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!GeneralMechanicsConfig.enableEntityAttributeFixer) return;
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.getEntities().getAll().forEach(entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                fixGhostEntity(livingEntity);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!GeneralMechanicsConfig.enableEntityAttributeFixer) return;
        Player player = event.getEntity();
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
    @SubscribeEvent
    public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!GeneralMechanicsConfig.enableEntityAttributeFixer) return;
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) {
            return;
        }
        player.giveExperiencePoints(1);
        player.giveExperiencePoints(-1);
    }
}