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
 * 事件订阅入口。非取消型回调按注册顺序逐一执行；某个回调抛出 RuntimeException 时仍执行后续回调，结束后抛出首个异常并附加后续异常。 取消型回调也逐项处理异常：未取消时继续下一个处理器；一旦取消立即停止，
 * 即使取消方随后抛出异常也不会调用下一个处理器，最后报告首个异常及后续错误。
 *
 * <p>Event subscriptions. Non-cancellable callbacks run one by one in registration order; when one throws a
 * RuntimeException the rest still run, and the first exception is rethrown afterwards with later ones attached as
 * suppressed. Cancellable callbacks isolate exceptions the same way: while the event is not cancelled the next
 * handler runs; once it is cancelled dispatch stops, even if the cancelling handler then throws, and the first
 * exception and later errors are reported at the end.
 */
public final class KineticLivingEvents {
    /** Result of a potion-applicability check. */
    public enum Applicability {
        /** Let vanilla decide whether the effect applies. */
        DEFAULT,
        /** Apply the effect even if vanilla would refuse, for example on an undead mob. */
        ALLOW,
        /** Refuse the effect. */
        DENY
    }

    /** Entity size recalculation, fired when the pose or scale changes. */
    public interface SizeContext {
        /** Returns the living entity the event is about. */
        LivingEntity entity();

        /** Returns the pose the size is being computed for. */
        Pose pose();

        /** Returns the size before the change. */
        EntityDimensions oldSize();

        /** Returns the size that will be applied, including changes by earlier handlers. */
        EntityDimensions newSize();

        /**
         * Replaces the size that will be applied. The eye height is not recalculated; set it with
         * {@link #newEyeHeight(float)}.
         */
        void newSize(EntityDimensions size);

        /** Returns the eye height before the change. */
        float oldEyeHeight();

        /** Returns the eye height that will be applied. */
        float newEyeHeight();

        /** Replaces the eye height that will be applied. */
        void newEyeHeight(float height);
    }

    /** Callback contract for size notifications. */
    @FunctionalInterface
    public interface SizeHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(SizeContext context);
    }

    /** Callback contract for living notifications. */
    @FunctionalInterface
    public interface LivingHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity.level().isClientSide()} when only one side matters. Runs every tick for every living
         * entity, so keep it cheap.
         */
        void handle(LivingEntity entity);
    }

    /** An entity finished using an item, such as eating food or drinking a potion. */
    public interface UseItemFinishContext {
        /** Returns the living entity the event is about. */
        LivingEntity entity();

        /** Returns a copy of the item as it was before it was used up. */
        ItemStack item();

        /** Returns the stack the entity holds after the use, for example an empty bottle after drinking. */
        ItemStack resultStack();

        /** Replaces the stack the entity holds after the use, for example to keep food from being consumed. */
        void setResultStack(ItemStack stack);
    }

    /** Callback contract for use item finish notifications. */
    @FunctionalInterface
    public interface UseItemFinishHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(UseItemFinishContext context);
    }

    /** An entity is about to be knocked back. */
    public interface KnockbackContext {
        /** Returns the living entity the event is about. */
        LivingEntity entity();

        /** Returns the knockback strength; vanilla melee uses about {@code 0.4}. */
        float strength();

        /** Replaces the knockback strength; {@code 0} removes the push but keeps the event. */
        void strength(float strength);

        /** Returns the X part of the direction towards the attacker; the entity is pushed the opposite way. */
        double ratioX();

        /** Replaces the X part of the direction towards the attacker. */
        void ratioX(double ratioX);

        /** Returns the Z part of the direction towards the attacker; the entity is pushed the opposite way. */
        double ratioZ();

        /** Replaces the Z part of the direction towards the attacker. */
        void ratioZ(double ratioZ);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the entity is not knocked back. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for knockback notifications. */
    @FunctionalInterface
    public interface KnockbackHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(KnockbackContext context);
    }

    /** Callback contract for equipment change notifications. */
    @FunctionalInterface
    public interface EquipmentChangeHandler {
        /**
         * Called after an equipment slot changed.
         *
         * @param entity entity whose equipment changed
         * @param slot changed slot
         * @param from previous stack; do not modify
         * @param to new stack; do not modify
         */
        void handle(LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to);
    }

    /** An entity is about to die. */
    public interface DeathContext {
        /** Returns the living entity the event is about. */
        LivingEntity entity();

        /** Returns the damage that killed the entity. */
        DamageSource source();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: the entity does not die. Raise its health above zero as well, or it dies again on the
         * next damage check. Later Kinetic handlers of this event are skipped unless they subscribed with
         * {@code receiveCancelled}.
         */
        void cancel();
    }

    /** Callback contract for death notifications. */
    @FunctionalInterface
    public interface DeathHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(DeathContext context);
    }

    /**
     * An entity is about to take damage, after invulnerability checks but before armor, enchantments and absorption
     * reduce it.
     */
    public interface HurtContext {
        /** Returns the living entity the event is about. */
        LivingEntity entity();

        /** Returns the damage source. */
        DamageSource source();

        /** Returns the damage before armor, enchantment and absorption reductions. */
        float amount();

        /** Replaces the damage before reductions; {@code 0} or less prevents the hit. */
        void amount(float amount);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: no damage is taken. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for hurt notifications. */
    @FunctionalInterface
    public interface HurtHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(HurtContext context);
    }

    /** An entity is about to regain health. */
    public interface HealContext {
        /** Returns the living entity the event is about. */
        LivingEntity entity();

        /** Returns the health about to be restored. */
        float amount();

        /** Replaces the health about to be restored. */
        void amount(float amount);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: no health is restored. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for heal notifications. */
    @FunctionalInterface
    public interface HealHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(HealContext context);
    }

    /**
     * Final damage is about to be subtracted from an entity's health, after every reduction. Not cancellable; set
     * the amount to {@code 0} instead.
     */
    public interface DamageContext {
        /** Returns the living entity the event is about. */
        LivingEntity entity();

        /** Returns the damage source. */
        DamageSource source();

        /** Returns the final damage after armor, enchantments and absorption. */
        float amount();

        /** Replaces the final damage; {@code 0} means the entity loses no health. */
        void amount(float amount);
    }

    /** Callback contract for damage notifications. */
    @FunctionalInterface
    public interface DamageHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(DamageContext context);
    }

    /**
     * An entity is being attacked, before any invulnerability or damage calculation. The earliest point to block a
     * hit completely.
     */
    public interface AttackContext {
        /** Returns the entity being attacked. */
        LivingEntity entity();

        /** Returns the damage source. */
        DamageSource source();

        /** Returns the raw damage of the attack. Change it later in the hurt or damage events. */
        float amount();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: the attack is ignored completely, including the hurt animation and knockback. Later
         * Kinetic handlers of this event are skipped.
         */
        void cancel();
    }

    /** Callback contract for attack notifications. */
    @FunctionalInterface
    public interface AttackHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(AttackContext context);
    }

    /** A mob or brain behavior is about to change its attack target. */
    public interface TargetChangeContext {
        /** Returns the entity whose target changes. */
        LivingEntity entity();

        /** Returns the target before the change, or {@code null} when there was none. */
        @Nullable
        LivingEntity originalTarget();

        /** Returns the target that will be set, or {@code null} when the target is being cleared. */
        @Nullable
        LivingEntity newTarget();

        /** Replaces the target that will be set; {@code null} clears it. */
        void newTarget(@Nullable LivingEntity target);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: the entity keeps its current target. Later Kinetic handlers of this event are skipped.
         */
        void cancel();
    }

    /** Callback contract for target change notifications. */
    @FunctionalInterface
    public interface TargetChangeHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(TargetChangeContext context);
    }

    /** A dying entity is about to drop experience. */
    public interface ExperienceDropContext {
        /** Returns the dying entity. */
        LivingEntity entity();

        /** Returns the player credited with the kill, or {@code null} when no player was involved. */
        @Nullable
        Player attackingPlayer();

        /** Returns the experience that will drop. */
        int droppedExperience();

        /** Replaces the experience that will drop. */
        void droppedExperience(int experience);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: no experience drops. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for experience drop notifications. */
    @FunctionalInterface
    public interface ExperienceDropHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(ExperienceDropContext context);
    }

    /** A dying entity is about to spawn its item drops. Fired on the server only. */
    public interface DropsContext {
        /** Returns the dying entity. */
        LivingEntity entity();

        /** Returns the damage that killed the entity. */
        DamageSource source();

        /**
         * Returns the item entities about to spawn. The collection is live: add or remove entries to change the
         * drops.
         */
        Collection<ItemEntity> drops();

        /** Returns whether a player hit the entity recently, which enables player-kill-only loot. */
        boolean recentlyHit();

        /** Returns the looting level applied to the drops. */
        int lootingLevel();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: no items drop. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for drops notifications. */
    @FunctionalInterface
    public interface DropsHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(DropsContext context);
    }

    /** A mob effect is about to be applied; handlers may force or refuse it. */
    public interface PotionApplicableContext {
        /** Returns the entity that would receive the effect. */
        LivingEntity entity();

        /** Returns the effect about to be applied. */
        MobEffectInstance effectInstance();

        /** Returns the current decision; {@link Applicability#DEFAULT} until a handler changes it. */
        Applicability applicability();

        /** Sets the decision; the last handler to set it wins. */
        void applicability(Applicability applicability);
    }

    /** Callback contract for potion applicable notifications. */
    @FunctionalInterface
    public interface PotionApplicableHandler {
        /**
         * Called for every matching event, on the thread of the logical side that fired it; check
         * {@code entity().level().isClientSide()} when only one side matters.
         */
        void handle(PotionApplicableContext context);
    }

    private KineticLivingEvents() {
    }

    /**
     * Subscribes to entity size recalculation ({@code EntityEvent.Size}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onSize(KineticEventPriority priority, SizeHandler handler) {
        return KineticLivingEventRuntime.registerSize(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to every living entity tick ({@code LivingEvent.LivingTickEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onTick(KineticEventPriority priority, LivingHandler handler) {
        return KineticLivingEventRuntime.registerTick(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to finished item use ({@code LivingEntityUseItemEvent.Finish}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onUseItemFinish(KineticEventPriority priority, UseItemFinishHandler handler) {
        return KineticLivingEventRuntime.registerUseItemFinish(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to knockback ({@code LivingKnockBackEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onKnockback(KineticEventPriority priority, KnockbackHandler handler) {
        return KineticLivingEventRuntime.registerKnockback(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to equipment changes ({@code LivingEquipmentChangeEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onEquipmentChange(KineticEventPriority priority, EquipmentChangeHandler handler) {
        return KineticLivingEventRuntime.registerEquipmentChange(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to entity deaths ({@code LivingDeathEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param receiveCancelled whether the handler also runs after an earlier handler cancelled the death
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if {@code priority} or {@code handler} is {@code null}
     */
    public static KineticEventSubscription onDeath(KineticEventPriority priority, boolean receiveCancelled, DeathHandler handler) {
        return KineticLivingEventRuntime.registerDeath(
                require(priority),
                receiveCancelled,
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to incoming damage before reductions ({@code LivingHurtEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onHurt(KineticEventPriority priority, HurtHandler handler) {
        return KineticLivingEventRuntime.registerHurt(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to healing ({@code LivingHealEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onHeal(KineticEventPriority priority, HealHandler handler) {
        return KineticLivingEventRuntime.registerHeal(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to final damage after reductions ({@code LivingDamageEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onDamage(KineticEventPriority priority, DamageHandler handler) {
        return KineticLivingEventRuntime.registerDamage(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to attacks, before any damage calculation ({@code LivingAttackEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onAttack(KineticEventPriority priority, AttackHandler handler) {
        return KineticLivingEventRuntime.registerAttack(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to attack target changes ({@code LivingChangeTargetEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onTargetChange(KineticEventPriority priority, TargetChangeHandler handler) {
        return KineticLivingEventRuntime.registerTargetChange(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to experience drops ({@code LivingExperienceDropEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onExperienceDrop(KineticEventPriority priority, ExperienceDropHandler handler) {
        return KineticLivingEventRuntime.registerExperienceDrop(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to item drops of dying entities ({@code LivingDropsEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onDrops(KineticEventPriority priority, DropsHandler handler) {
        return KineticLivingEventRuntime.registerDrops(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to mob effect applicability checks ({@code MobEffectEvent.Applicable}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPotionApplicable(KineticEventPriority priority, PotionApplicableHandler handler) {
        return KineticLivingEventRuntime.registerPotionApplicable(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    private static KineticEventPriority require(KineticEventPriority priority) {
        return Objects.requireNonNull(priority, "priority");
    }
}
