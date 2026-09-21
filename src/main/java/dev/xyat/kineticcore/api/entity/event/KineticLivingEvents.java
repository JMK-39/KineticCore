package dev.xyat.kineticcore.api.entity.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticLivingEventRuntime;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

/**
 * 事件订阅入口。非取消型回调按注册顺序逐一执行；某个回调抛出 RuntimeException
 * 时仍执行后续回调，结束后抛出首个异常并附加后续异常。
 * 取消型回调也逐项处理异常：未取消时继续下一个处理器；一旦取消立即停止，
 * 即使取消方随后抛出异常也不会调用下一个处理器，最后报告首个异常及后续错误。
 */
public final class KineticLivingEvents {
    /** Supported applicability values exposed by this API. */
    public enum Applicability {
        DEFAULT,
        ALLOW,
        DENY
    }

    /** Context exposed to size callbacks. */
    public interface SizeContext {
        LivingEntity entity();

        Pose pose();

        EntityDimensions oldSize();

        EntityDimensions newSize();

        void newSize(EntityDimensions size);

        float oldEyeHeight();

        float newEyeHeight();

        void newEyeHeight(float height);
    }

    /** Callback contract for size notifications. */
    @FunctionalInterface
    public interface SizeHandler {
        void handle(SizeContext context);
    }

    /** Callback contract for living notifications. */
    @FunctionalInterface
    public interface LivingHandler {
        void handle(LivingEntity entity);
    }

    /** Context exposed to use item finish callbacks. */
    public interface UseItemFinishContext {
        LivingEntity entity();

        ItemStack item();
    }

    /** Callback contract for use item finish notifications. */
    @FunctionalInterface
    public interface UseItemFinishHandler {
        void handle(UseItemFinishContext context);
    }

    /** Context exposed to knockback callbacks. */
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

    /** Callback contract for knockback notifications. */
    @FunctionalInterface
    public interface KnockbackHandler {
        void handle(KnockbackContext context);
    }

    /** Callback contract for equipment change notifications. */
    @FunctionalInterface
    public interface EquipmentChangeHandler {
        void handle(LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to);
    }

    /** Context exposed to death callbacks. */
    public interface DeathContext {
        LivingEntity entity();

        DamageSource source();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for death notifications. */
    @FunctionalInterface
    public interface DeathHandler {
        void handle(DeathContext context);
    }

    /** Context exposed to hurt callbacks. */
    public interface HurtContext {
        LivingEntity entity();

        DamageSource source();

        float amount();

        void amount(float amount);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for hurt notifications. */
    @FunctionalInterface
    public interface HurtHandler {
        void handle(HurtContext context);
    }

    /** Context exposed to heal callbacks. */
    public interface HealContext {
        LivingEntity entity();

        float amount();

        void amount(float amount);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for heal notifications. */
    @FunctionalInterface
    public interface HealHandler {
        void handle(HealContext context);
    }

    /** Context exposed to damage callbacks. */
    public interface DamageContext {
        LivingEntity entity();

        DamageSource source();

        float amount();

        void amount(float amount);
    }

    /** Callback contract for damage notifications. */
    @FunctionalInterface
    public interface DamageHandler {
        void handle(DamageContext context);
    }

    /** Context exposed to attack callbacks. */
    public interface AttackContext {
        LivingEntity entity();

        DamageSource source();

        float amount();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for attack notifications. */
    @FunctionalInterface
    public interface AttackHandler {
        void handle(AttackContext context);
    }

    /** Context exposed to target change callbacks. */
    public interface TargetChangeContext {
        LivingEntity entity();

        @Nullable
        LivingEntity originalTarget();

        @Nullable
        LivingEntity newTarget();

        void newTarget(@Nullable LivingEntity target);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for target change notifications. */
    @FunctionalInterface
    public interface TargetChangeHandler {
        void handle(TargetChangeContext context);
    }

    /** Context exposed to experience drop callbacks. */
    public interface ExperienceDropContext {
        LivingEntity entity();

        @Nullable
        Player attackingPlayer();

        int droppedExperience();

        void droppedExperience(int experience);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for experience drop notifications. */
    @FunctionalInterface
    public interface ExperienceDropHandler {
        void handle(ExperienceDropContext context);
    }

    /** Context exposed to drops callbacks. */
    public interface DropsContext {
        LivingEntity entity();

        DamageSource source();

        Collection<ItemEntity> drops();

        boolean recentlyHit();

        int lootingLevel();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for drops notifications. */
    @FunctionalInterface
    public interface DropsHandler {
        void handle(DropsContext context);
    }

    /** Context exposed to potion applicable callbacks. */
    public interface PotionApplicableContext {
        LivingEntity entity();

        MobEffectInstance effectInstance();

        Applicability applicability();

        void applicability(Applicability applicability);
    }

    /** Callback contract for potion applicable notifications. */
    @FunctionalInterface
    public interface PotionApplicableHandler {
        void handle(PotionApplicableContext context);
    }

    private KineticLivingEvents() {
    }

    /**
     * Registers a listener for size.
     */
    public static KineticEventSubscription onSize(KineticEventPriority priority, SizeHandler handler) {
        return KineticLivingEventRuntime.registerSize(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for tick.
     */
    public static KineticEventSubscription onTick(KineticEventPriority priority, LivingHandler handler) {
        return KineticLivingEventRuntime.registerTick(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for use item finish.
     */
    public static KineticEventSubscription onUseItemFinish(KineticEventPriority priority, UseItemFinishHandler handler) {
        return KineticLivingEventRuntime.registerUseItemFinish(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for knockback.
     */
    public static KineticEventSubscription onKnockback(KineticEventPriority priority, KnockbackHandler handler) {
        return KineticLivingEventRuntime.registerKnockback(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for equipment change.
     */
    public static KineticEventSubscription onEquipmentChange(KineticEventPriority priority, EquipmentChangeHandler handler) {
        return KineticLivingEventRuntime.registerEquipmentChange(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for death.
     */
    public static KineticEventSubscription onDeath(KineticEventPriority priority, boolean receiveCancelled, DeathHandler handler) {
        return KineticLivingEventRuntime.registerDeath(
                require(priority),
                receiveCancelled,
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for hurt.
     */
    public static KineticEventSubscription onHurt(KineticEventPriority priority, HurtHandler handler) {
        return KineticLivingEventRuntime.registerHurt(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for heal.
     */
    public static KineticEventSubscription onHeal(KineticEventPriority priority, HealHandler handler) {
        return KineticLivingEventRuntime.registerHeal(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for damage.
     */
    public static KineticEventSubscription onDamage(KineticEventPriority priority, DamageHandler handler) {
        return KineticLivingEventRuntime.registerDamage(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for attack.
     */
    public static KineticEventSubscription onAttack(KineticEventPriority priority, AttackHandler handler) {
        return KineticLivingEventRuntime.registerAttack(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for target change.
     */
    public static KineticEventSubscription onTargetChange(KineticEventPriority priority, TargetChangeHandler handler) {
        return KineticLivingEventRuntime.registerTargetChange(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for experience drop.
     */
    public static KineticEventSubscription onExperienceDrop(KineticEventPriority priority, ExperienceDropHandler handler) {
        return KineticLivingEventRuntime.registerExperienceDrop(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for drops.
     */
    public static KineticEventSubscription onDrops(KineticEventPriority priority, DropsHandler handler) {
        return KineticLivingEventRuntime.registerDrops(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for potion applicable.
     */
    public static KineticEventSubscription onPotionApplicable(KineticEventPriority priority, PotionApplicableHandler handler) {
        return KineticLivingEventRuntime.registerPotionApplicable(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    private static KineticEventPriority require(KineticEventPriority priority) {
        return Objects.requireNonNull(priority, "priority");
    }
}
