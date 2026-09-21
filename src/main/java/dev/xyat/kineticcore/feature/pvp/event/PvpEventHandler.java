package dev.xyat.kineticcore.feature.pvp.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.entity.event.KineticLivingEvents;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.feature.pvp.command.PvpCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;

public class PvpEventHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticLivingEvents.onAttack(KineticEventPriority.NORMAL, PvpEventHandler::onLivingAttack),
                () -> KineticLivingEvents.onTargetChange(KineticEventPriority.NORMAL, PvpEventHandler::onSetTarget)
        );
    }


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

    public static void onLivingAttack(KineticLivingEvents.AttackContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "pvp_protection", true)) return;
        if (context.source() == null) return;

        Entity trueSource = context.source().getEntity();
        Entity victim = context.entity();
        ServerPlayer attackerPlayer = getPlayerOwner(trueSource);
        ServerPlayer victimPlayer = getPlayerOwner(victim);

        if (attackerPlayer == null || victimPlayer == null || attackerPlayer.equals(victimPlayer)) {
            return;
        }

        if (isPvpProtected(attackerPlayer) || isPvpProtected(victimPlayer)) {
            context.cancel();
        }
    }

    public static void onSetTarget(KineticLivingEvents.TargetChangeContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "pvp_protection", true)) return;

        LivingEntity attacker = context.entity();
        LivingEntity newTarget = context.newTarget();
        if (newTarget == null) return;

        ServerPlayer attackerOwner = getPlayerOwner(attacker);
        ServerPlayer targetPlayer = getPlayerOwner(newTarget);
        if (attackerOwner == null || targetPlayer == null || attackerOwner.equals(targetPlayer)) {
            return;
        }

        if (isPvpProtected(attackerOwner) || isPvpProtected(targetPlayer)) {
            context.cancel();
        }
    }

    public static void clearConflictingTargets(ServerPlayer changedPlayer) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "pvp_protection", true) || changedPlayer == null || !isPvpProtected(changedPlayer)) {
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
