package dev.xyat.kineticcore.api.entity.event;

import dev.xyat.kineticcore.internal.runtime.event.KineticLivingEventRuntime;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

public final class KineticLivingEvents {
    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

    public enum Applicability {
        DEFAULT,
        ALLOW,
        DENY
    }

    @FunctionalInterface
    public interface LivingHandler {
        void handle(LivingEntity entity);
    }

    public interface UseItemFinishContext {
        LivingEntity entity();

        ItemStack item();
    }

    @FunctionalInterface
    public interface UseItemFinishHandler {
        void handle(UseItemFinishContext context);
    }

    public interface KnockbackContext {
        LivingEntity entity();

        float strength();

        void strength(float strength);

        double ratioX();

        void ratioX(double ratioX);

        double ratioZ();

        void ratioZ(double ratioZ);

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface KnockbackHandler {
        void handle(KnockbackContext context);
    }

    @FunctionalInterface
    public interface EquipmentChangeHandler {
        void handle(LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to);
    }

    public interface DeathContext {
        LivingEntity entity();

        DamageSource source();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface DeathHandler {
        void handle(DeathContext context);
    }

    public interface HurtContext {
        LivingEntity entity();

        DamageSource source();

        float amount();

        void amount(float amount);

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface HurtHandler {
        void handle(HurtContext context);
    }

    public interface HealContext {
        LivingEntity entity();

        float amount();

        void amount(float amount);

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface HealHandler {
        void handle(HealContext context);
    }

    public interface DamageContext {
        LivingEntity entity();

        DamageSource source();

        float amount();

        void amount(float amount);
    }

    @FunctionalInterface
    public interface DamageHandler {
        void handle(DamageContext context);
    }

    public interface AttackContext {
        LivingEntity entity();

        DamageSource source();

        float amount();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface AttackHandler {
        void handle(AttackContext context);
    }

    public interface ExperienceDropContext {
        LivingEntity entity();

        @Nullable
        Player attackingPlayer();

        int droppedExperience();

        void droppedExperience(int experience);

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface ExperienceDropHandler {
        void handle(ExperienceDropContext context);
    }

    public interface DropsContext {
        LivingEntity entity();

        DamageSource source();

        Collection<ItemEntity> drops();

        boolean recentlyHit();

        int lootingLevel();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface DropsHandler {
        void handle(DropsContext context);
    }

    public interface PotionApplicableContext {
        LivingEntity entity();

        MobEffectInstance effectInstance();

        Applicability applicability();

        void applicability(Applicability applicability);
    }

    @FunctionalInterface
    public interface PotionApplicableHandler {
        void handle(PotionApplicableContext context);
    }

    private KineticLivingEvents() {
    }

    public static HookRegistration onTick(LivingHandler handler) {
        return onTick(Priority.NORMAL, handler);
    }

    public static HookRegistration onTick(Priority priority, LivingHandler handler) {
        return KineticLivingEventRuntime.registerTick(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onUseItemFinish(UseItemFinishHandler handler) {
        return onUseItemFinish(Priority.NORMAL, handler);
    }

    public static HookRegistration onUseItemFinish(Priority priority, UseItemFinishHandler handler) {
        return KineticLivingEventRuntime.registerUseItemFinish(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onKnockback(KnockbackHandler handler) {
        return onKnockback(Priority.NORMAL, handler);
    }

    public static HookRegistration onKnockback(Priority priority, KnockbackHandler handler) {
        return KineticLivingEventRuntime.registerKnockback(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onEquipmentChange(EquipmentChangeHandler handler) {
        return onEquipmentChange(Priority.NORMAL, handler);
    }

    public static HookRegistration onEquipmentChange(Priority priority, EquipmentChangeHandler handler) {
        return KineticLivingEventRuntime.registerEquipmentChange(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onDeath(DeathHandler handler) {
        return onDeath(Priority.NORMAL, handler);
    }

    public static HookRegistration onDeath(Priority priority, DeathHandler handler) {
        return onDeath(priority, false, handler);
    }

    public static HookRegistration onDeath(Priority priority, boolean receiveCancelled, DeathHandler handler) {
        return KineticLivingEventRuntime.registerDeath(
                require(priority),
                receiveCancelled,
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onHurt(HurtHandler handler) {
        return onHurt(Priority.NORMAL, handler);
    }

    public static HookRegistration onHurt(Priority priority, HurtHandler handler) {
        return KineticLivingEventRuntime.registerHurt(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onHeal(HealHandler handler) {
        return onHeal(Priority.NORMAL, handler);
    }

    public static HookRegistration onHeal(Priority priority, HealHandler handler) {
        return KineticLivingEventRuntime.registerHeal(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onDamage(DamageHandler handler) {
        return onDamage(Priority.NORMAL, handler);
    }

    public static HookRegistration onDamage(Priority priority, DamageHandler handler) {
        return KineticLivingEventRuntime.registerDamage(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onAttack(AttackHandler handler) {
        return onAttack(Priority.NORMAL, handler);
    }

    public static HookRegistration onAttack(Priority priority, AttackHandler handler) {
        return KineticLivingEventRuntime.registerAttack(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onExperienceDrop(ExperienceDropHandler handler) {
        return onExperienceDrop(Priority.NORMAL, handler);
    }

    public static HookRegistration onExperienceDrop(Priority priority, ExperienceDropHandler handler) {
        return KineticLivingEventRuntime.registerExperienceDrop(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onDrops(DropsHandler handler) {
        return onDrops(Priority.NORMAL, handler);
    }

    public static HookRegistration onDrops(Priority priority, DropsHandler handler) {
        return KineticLivingEventRuntime.registerDrops(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onPotionApplicable(PotionApplicableHandler handler) {
        return onPotionApplicable(Priority.NORMAL, handler);
    }

    public static HookRegistration onPotionApplicable(Priority priority, PotionApplicableHandler handler) {
        return KineticLivingEventRuntime.registerPotionApplicable(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    private static Priority require(Priority priority) {
        return Objects.requireNonNull(priority, "priority");
    }
}
