package dev.xyat.kineticcore.feature.pvp.event;

import dev.xyat.kineticcore.feature.pvp.command.PvpCommand;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PvpEventHandler {

    // 获取实体的真实拥有者（如果是玩家）
    private static ServerPlayer getPlayerOwner(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (entity instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof ServerPlayer player) {
                return player;
            }
        }
        if (entity instanceof TamableAnimal tamable) {
            LivingEntity owner = tamable.getOwner();
            if (owner instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }

    // 改为调用 PvpCommand 中处理过 Forge 持久化数据的判定方法
    private static boolean isPvpProtected(ServerPlayer player) {
        return PvpCommand.isPvpProtected(player);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!GeneralMechanicsConfig.enablePvpProtection) return;
        if (event.getSource() == null) return;

        Entity trueSource = event.getSource().getEntity(); // 攻击发起者
        Entity victim = event.getEntity(); // 受害者

        // 1. 获取攻击者背后的玩家
        ServerPlayer attackerPlayer = getPlayerOwner(trueSource);
        // 2. 获取受害者背后的玩家
        ServerPlayer victimPlayer = getPlayerOwner(victim);

        // 只有当攻击者和受害者【同时】都关联到玩家时，才判定为 PVP/PVPE 行为
        if (attackerPlayer != null && victimPlayer != null) {

            // 自己打自己，或者玩家打自己的仆从/仆从打自己，不拦截
            if (attackerPlayer.equals(victimPlayer)) return;

            // 只要其中一方（攻击方或受害方）开启了 PVP 保护，就取消这次玩家间或其仆从间的伤害
            if (isPvpProtected(attackerPlayer) || isPvpProtected(victimPlayer)) {
                event.setCanceled(true);
            }
        }
    }

    // 防止生物（如狼、召唤物）将受保护的玩家（或其宠物）选定为攻击目标
    @SubscribeEvent
    public static void onSetTarget(LivingChangeTargetEvent event) {
        if (!GeneralMechanicsConfig.enablePvpProtection) return;

        LivingEntity attacker = event.getEntity(); // 发起仇恨的生物（如狼）
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null) return;

        ServerPlayer attackerOwner = getPlayerOwner(attacker);
        ServerPlayer targetPlayer = getPlayerOwner(newTarget);

        // 只有当【攻击的生物是有主人的】且【目标也是玩家或有主人的】时，才判定拦截
        if (attackerOwner != null && targetPlayer != null && !attackerOwner.equals(targetPlayer)) {
            if (isPvpProtected(attackerOwner) || isPvpProtected(targetPlayer)) {
                event.setCanceled(true);
            }
        }
    }

    // 主动监测并自动移除现有仇恨（当玩家中途开启 PVP 保护时自动生效）
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!GeneralMechanicsConfig.enablePvpProtection) return;

        LivingEntity entity = event.getEntity();
        // 为降低性能消耗，仅在服务端运行且每秒（20 tick）检测一次即可
        if (entity.level().isClientSide() || entity.tickCount % 20 != 0) return;

        // 只有 Mob（AI控制的生物）才有主动仇恨目标 (Target)
        if (entity instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                ServerPlayer attackerOwner = getPlayerOwner(mob);
                ServerPlayer targetOwner = getPlayerOwner(target);

                // 如果相互对立的双方都有玩家归属，且并非同一个玩家
                if (attackerOwner != null && targetOwner != null && !attackerOwner.equals(targetOwner)) {
                    if (isPvpProtected(attackerOwner) || isPvpProtected(targetOwner)) {
                        // 强制清空当前锁定目标和反击目标，移除仇恨
                        mob.setTarget(null);
                        if (mob.getLastHurtByMob() == target) {
                            mob.setLastHurtByMob(null);
                        }
                    }
                }
            }
        }
    }
}