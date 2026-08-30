package dev.xyat.kineticcore.feature.pvp.event;

import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import dev.xyat.kineticcore.feature.pvp.command.PvpCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PvpEventHandler {

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

    private static boolean isPvpProtected(ServerPlayer player) {
        return PvpCommand.isPvpProtected(player);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!GeneralMechanicsConfig.enablePvpProtection) return;
        if (event.getSource() == null) return;

        Entity trueSource = event.getSource().getEntity();
        Entity victim = event.getEntity();
        ServerPlayer attackerPlayer = getPlayerOwner(trueSource);
        ServerPlayer victimPlayer = getPlayerOwner(victim);

        if (attackerPlayer == null || victimPlayer == null || attackerPlayer.equals(victimPlayer)) {
            return;
        }

        if (isPvpProtected(attackerPlayer) || isPvpProtected(victimPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSetTarget(LivingChangeTargetEvent event) {
        if (!GeneralMechanicsConfig.enablePvpProtection) return;

        LivingEntity attacker = event.getEntity();
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null) return;

        ServerPlayer attackerOwner = getPlayerOwner(attacker);
        ServerPlayer targetPlayer = getPlayerOwner(newTarget);
        if (attackerOwner == null || targetPlayer == null || attackerOwner.equals(targetPlayer)) {
            return;
        }

        if (isPvpProtected(attackerOwner) || isPvpProtected(targetPlayer)) {
            event.setCanceled(true);
        }
    }

    public static void clearConflictingTargets(ServerPlayer changedPlayer) {
        if (!GeneralMechanicsConfig.enablePvpProtection || changedPlayer == null || !isPvpProtected(changedPlayer)) {
            return;
        }

        for (ServerLevel level : changedPlayer.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob) {
                    clearConflictingTarget(mob, changedPlayer);
                }
            }
        }
    }

    private static void clearConflictingTarget(Mob mob, ServerPlayer changedPlayer) {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        ServerPlayer attackerOwner = getPlayerOwner(mob);
        ServerPlayer targetOwner = getPlayerOwner(target);
        if (attackerOwner == null || targetOwner == null || attackerOwner.equals(targetOwner)) {
            return;
        }

        if (!attackerOwner.equals(changedPlayer) && !targetOwner.equals(changedPlayer)) {
            return;
        }

        if (isPvpProtected(attackerOwner) || isPvpProtected(targetOwner)) {
            mob.setTarget(null);
            if (mob.getLastHurtByMob() == target) {
                mob.setLastHurtByMob(null);
            }
        }
    }
}
