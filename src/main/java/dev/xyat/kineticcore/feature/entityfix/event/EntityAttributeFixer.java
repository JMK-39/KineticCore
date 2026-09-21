package dev.xyat.kineticcore.feature.entityfix.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.entity.event.KineticLivingEvents;
import dev.xyat.kineticcore.api.world.event.KineticWorldEvents;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EntityAttributeFixer {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticWorldEvents.onEntityJoin(KineticEventPriority.NORMAL, EntityAttributeFixer::onEntityJoinWorld),
                () -> KineticLivingEvents.onAttack(KineticEventPriority.NORMAL, EntityAttributeFixer::onLivingAttack),
                () -> KineticLivingEvents.onDeath(KineticEventPriority.NORMAL, false, EntityAttributeFixer::onLivingDeath),
                () -> KineticWorldEvents.onLevelLoad(KineticEventPriority.NORMAL, EntityAttributeFixer::onLevelLoad),
                () -> KineticServerEvents.onPlayerLogout(KineticEventPriority.NORMAL, EntityAttributeFixer::onPlayerLogout),
                () -> KineticServerEvents.onPlayerChangedDimension(KineticEventPriority.NORMAL, EntityAttributeFixer::onPlayerDimensionChange)
        );
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

    public static void onEntityJoinWorld(KineticWorldEvents.EntityJoinContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        if (context.level().isClientSide()) return;

        if (context.entity() instanceof LivingEntity livingEntity) {
            fixGhostEntity(livingEntity);

            if (livingEntity instanceof Player player) {
                restorePlayerHealth(player);
            }
        }
    }

    public static void onLivingAttack(KineticLivingEvents.AttackContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        fixGhostEntity(context.entity());
    }

    public static void onLivingDeath(KineticLivingEvents.DeathContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        fixGhostEntity(context.entity());
    }

    public static void onLevelLoad(net.minecraft.world.level.LevelAccessor level) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "entity_fixer", true)) return;
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
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
