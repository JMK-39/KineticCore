package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;

import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.entity.event.KineticLivingEvents;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.Collection;
import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticLivingEventRuntime {
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.SizeHandler>> SIZE = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.LivingHandler>> TICK = livingHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.UseItemFinishHandler>> USE_ITEM_FINISH = useItemFinishHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.KnockbackHandler>> KNOCKBACK = knockbackHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.EquipmentChangeHandler>> EQUIPMENT_CHANGE = equipmentHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.DeathHandler>> DEATH = deathHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.DeathHandler>> DEATH_RECEIVE_CANCELLED = deathHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.HurtHandler>> HURT = hurtHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.DamageHandler>> DAMAGE = damageHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.HealHandler>> HEAL = healHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.AttackHandler>> ATTACK = attackHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.TargetChangeHandler>> TARGET_CHANGE = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.PotionApplicableHandler>> POTION_APPLICABLE = potionHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.ExperienceDropHandler>> EXPERIENCE_DROP = experienceDropHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.DropsHandler>> DROPS = dropsHandlers();

    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;

    private KineticLivingEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;

        for (KineticEventPriority priority : KineticEventPriority.values()) {
            EventPriority forgePriority = toForge(priority);
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (EntityEvent.Size event) -> onSize(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingEvent.LivingTickEvent event) -> onTick(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingEntityUseItemEvent.Finish event) -> onUseItemFinish(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingKnockBackEvent event) -> onKnockback(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingEquipmentChangeEvent event) -> onEquipmentChange(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingDeathEvent event) -> onDeath(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, true, (LivingDeathEvent event) -> onDeathReceiveCancelled(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingHurtEvent event) -> onHurt(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingDamageEvent event) -> onDamage(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingHealEvent event) -> onHeal(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingAttackEvent event) -> onAttack(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingChangeTargetEvent event) -> onTargetChange(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (MobEffectEvent.Applicable event) -> onPotionApplicable(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingExperienceDropEvent event) -> onExperienceDrop(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingDropsEvent event) -> onDrops(priority, event)));
        }
        attempt.finish();
        initialized = true;
    }

    public static KineticEventSubscription registerSize(KineticEventPriority priority, KineticLivingEvents.SizeHandler handler) {
        initialize();
        return add(SIZE, priority, handler);
    }

    public static KineticEventSubscription registerTick(KineticEventPriority priority, KineticLivingEvents.LivingHandler handler) {
        initialize();
        return add(TICK, priority, handler);
    }

    public static KineticEventSubscription registerUseItemFinish(KineticEventPriority priority, KineticLivingEvents.UseItemFinishHandler handler) {
        initialize();
        return add(USE_ITEM_FINISH, priority, handler);
    }

    public static KineticEventSubscription registerKnockback(KineticEventPriority priority, KineticLivingEvents.KnockbackHandler handler) {
        initialize();
        return add(KNOCKBACK, priority, handler);
    }

    public static KineticEventSubscription registerEquipmentChange(KineticEventPriority priority, KineticLivingEvents.EquipmentChangeHandler handler) {
        initialize();
        return add(EQUIPMENT_CHANGE, priority, handler);
    }

    public static KineticEventSubscription registerDeath(
            KineticEventPriority priority,
            boolean receiveCancelled,
            KineticLivingEvents.DeathHandler handler
    ) {
        initialize();
        return add(receiveCancelled ? DEATH_RECEIVE_CANCELLED : DEATH, priority, handler);
    }

    public static KineticEventSubscription registerHurt(KineticEventPriority priority, KineticLivingEvents.HurtHandler handler) {
        initialize();
        return add(HURT, priority, handler);
    }

    public static KineticEventSubscription registerDamage(KineticEventPriority priority, KineticLivingEvents.DamageHandler handler) {
        initialize();
        return add(DAMAGE, priority, handler);
    }

    public static KineticEventSubscription registerHeal(KineticEventPriority priority, KineticLivingEvents.HealHandler handler) {
        initialize();
        return add(HEAL, priority, handler);
    }

    public static KineticEventSubscription registerAttack(KineticEventPriority priority, KineticLivingEvents.AttackHandler handler) {
        initialize();
        return add(ATTACK, priority, handler);
    }

    public static KineticEventSubscription registerTargetChange(KineticEventPriority priority, KineticLivingEvents.TargetChangeHandler handler) {
        initialize();
        return add(TARGET_CHANGE, priority, handler);
    }

    public static KineticEventSubscription registerPotionApplicable(KineticEventPriority priority, KineticLivingEvents.PotionApplicableHandler handler) {
        initialize();
        return add(POTION_APPLICABLE, priority, handler);
    }

    public static KineticEventSubscription registerExperienceDrop(KineticEventPriority priority, KineticLivingEvents.ExperienceDropHandler handler) {
        initialize();
        return add(EXPERIENCE_DROP, priority, handler);
    }

    public static KineticEventSubscription registerDrops(KineticEventPriority priority, KineticLivingEvents.DropsHandler handler) {
        initialize();
        return add(DROPS, priority, handler);
    }

    private static void onSize(KineticEventPriority priority, EntityEvent.Size event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        SizeContextImpl context = new SizeContextImpl(event, living);
        KineticCallbackBatch.runAll(SIZE.get(priority), handler -> handler.handle(context));
    }

    private static void onTick(KineticEventPriority priority, LivingEvent.LivingTickEvent event) {
        KineticCallbackBatch.runAll(TICK.get(priority), handler -> handler.handle(event.getEntity()));
    }

    private static void onUseItemFinish(KineticEventPriority priority, LivingEntityUseItemEvent.Finish event) {
        UseItemFinishContextImpl context = new UseItemFinishContextImpl(event);
        KineticCallbackBatch.runAll(USE_ITEM_FINISH.get(priority), handler -> handler.handle(context));
    }

    private static void onKnockback(KineticEventPriority priority, LivingKnockBackEvent event) {
        KnockbackContextImpl context = new KnockbackContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                KNOCKBACK.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onEquipmentChange(KineticEventPriority priority, LivingEquipmentChangeEvent event) {
        KineticCallbackBatch.runAll(EQUIPMENT_CHANGE.get(priority), handler -> handler.handle(event.getEntity(), event.getSlot(), event.getFrom(), event.getTo()));
    }

    private static void onDeath(KineticEventPriority priority, LivingDeathEvent event) {
        DeathContextImpl context = new DeathContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                DEATH.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onDeathReceiveCancelled(KineticEventPriority priority, LivingDeathEvent event) {
        DeathContextImpl context = new DeathContextImpl(event);
        KineticCallbackBatch.runAll(DEATH_RECEIVE_CANCELLED.get(priority), handler -> handler.handle(context));
    }

    private static void onHurt(KineticEventPriority priority, LivingHurtEvent event) {
        HurtContextImpl context = new HurtContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                HURT.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onDamage(KineticEventPriority priority, LivingDamageEvent event) {
        DamageContextImpl context = new DamageContextImpl(event);
        KineticCallbackBatch.runAll(DAMAGE.get(priority), handler -> handler.handle(context));
    }

    private static void onHeal(KineticEventPriority priority, LivingHealEvent event) {
        HealContextImpl context = new HealContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                HEAL.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onAttack(KineticEventPriority priority, LivingAttackEvent event) {
        AttackContextImpl context = new AttackContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                ATTACK.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onTargetChange(KineticEventPriority priority, LivingChangeTargetEvent event) {
        TargetChangeContextImpl context = new TargetChangeContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                TARGET_CHANGE.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onPotionApplicable(KineticEventPriority priority, MobEffectEvent.Applicable event) {
        PotionApplicableContextImpl context = new PotionApplicableContextImpl(event);
        KineticCallbackBatch.runAll(POTION_APPLICABLE.get(priority), handler -> handler.handle(context));
    }

    private static void onExperienceDrop(KineticEventPriority priority, LivingExperienceDropEvent event) {
        ExperienceDropContextImpl context = new ExperienceDropContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                EXPERIENCE_DROP.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onDrops(KineticEventPriority priority, LivingDropsEvent event) {
        DropsContextImpl context = new DropsContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                DROPS.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static EventPriority toForge(KineticEventPriority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private record SizeContextImpl(EntityEvent.Size event, LivingEntity entity) implements KineticLivingEvents.SizeContext {
        @Override
        public Pose pose() { return event.getPose(); }

        @Override
        public EntityDimensions oldSize() { return event.getOldSize(); }

        @Override
        public EntityDimensions newSize() { return event.getNewSize(); }

        @Override
        public void newSize(EntityDimensions size) { event.setNewSize(size); }

        @Override
        public float oldEyeHeight() { return event.getOldEyeHeight(); }

        @Override
        public float newEyeHeight() { return event.getNewEyeHeight(); }

        @Override
        public void newEyeHeight(float height) { event.setNewEyeHeight(height); }
    }

    private record UseItemFinishContextImpl(LivingEntityUseItemEvent.Finish event) implements KineticLivingEvents.UseItemFinishContext {
        @Override
        public LivingEntity entity() { return event.getEntity(); }

        @Override
        public ItemStack item() { return event.getItem(); }
    }

    private record KnockbackContextImpl(LivingKnockBackEvent event) implements KineticLivingEvents.KnockbackContext {
        @Override
        public LivingEntity entity() { return event.getEntity(); }

        @Override
        public float strength() { return event.getStrength(); }

        @Override
        public void strength(float strength) { event.setStrength(strength); }

        @Override
        public double ratioX() { return event.getRatioX(); }

        @Override
        public void ratioX(double ratioX) { event.setRatioX(ratioX); }

        @Override
        public double ratioZ() { return event.getRatioZ(); }

        @Override
        public void ratioZ(double ratioZ) { event.setRatioZ(ratioZ); }

        @Override
        public boolean cancelled() { return event.isCanceled(); }

        @Override
        public void cancel() { event.setCanceled(true); }
    }

    private record DeathContextImpl(LivingDeathEvent event) implements KineticLivingEvents.DeathContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public DamageSource source() {
            return event.getSource();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record HurtContextImpl(LivingHurtEvent event) implements KineticLivingEvents.HurtContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public DamageSource source() {
            return event.getSource();
        }

        @Override
        public float amount() {
            return event.getAmount();
        }

        @Override
        public void amount(float amount) {
            event.setAmount(amount);
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record HealContextImpl(LivingHealEvent event) implements KineticLivingEvents.HealContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public float amount() {
            return event.getAmount();
        }

        @Override
        public void amount(float amount) {
            event.setAmount(amount);
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record DamageContextImpl(LivingDamageEvent event) implements KineticLivingEvents.DamageContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public DamageSource source() {
            return event.getSource();
        }

        @Override
        public float amount() {
            return event.getAmount();
        }

        @Override
        public void amount(float amount) {
            event.setAmount(amount);
        }
    }

    private record AttackContextImpl(LivingAttackEvent event) implements KineticLivingEvents.AttackContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public DamageSource source() {
            return event.getSource();
        }

        @Override
        public float amount() {
            return event.getAmount();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record TargetChangeContextImpl(LivingChangeTargetEvent event) implements KineticLivingEvents.TargetChangeContext {
        @Override
        public LivingEntity entity() { return event.getEntity(); }

        @Override
        public LivingEntity originalTarget() { return event.getOriginalTarget(); }

        @Override
        public LivingEntity newTarget() { return event.getNewTarget(); }

        @Override
        public void newTarget(LivingEntity target) { event.setNewTarget(target); }

        @Override
        public boolean cancelled() { return event.isCanceled(); }

        @Override
        public void cancel() { event.setCanceled(true); }
    }

    private record ExperienceDropContextImpl(LivingExperienceDropEvent event) implements KineticLivingEvents.ExperienceDropContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public Player attackingPlayer() {
            return event.getAttackingPlayer();
        }

        @Override
        public int droppedExperience() {
            return event.getDroppedExperience();
        }

        @Override
        public void droppedExperience(int experience) {
            event.setDroppedExperience(experience);
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record DropsContextImpl(LivingDropsEvent event) implements KineticLivingEvents.DropsContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public DamageSource source() {
            return event.getSource();
        }

        @Override
        public Collection<ItemEntity> drops() {
            return event.getDrops();
        }

        @Override
        public boolean recentlyHit() {
            return event.isRecentlyHit();
        }

        @Override
        public int lootingLevel() {
            return event.getLootingLevel();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record PotionApplicableContextImpl(MobEffectEvent.Applicable event) implements KineticLivingEvents.PotionApplicableContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public MobEffectInstance effectInstance() {
            return event.getEffectInstance();
        }

        @Override
        public KineticLivingEvents.Applicability applicability() {
            return switch (event.getResult()) {
                case ALLOW -> KineticLivingEvents.Applicability.ALLOW;
                case DENY -> KineticLivingEvents.Applicability.DENY;
                default -> KineticLivingEvents.Applicability.DEFAULT;
            };
        }

        @Override
        public void applicability(KineticLivingEvents.Applicability applicability) {
            event.setResult(switch (applicability) {
                case ALLOW -> Event.Result.ALLOW;
                case DENY -> Event.Result.DENY;
                case DEFAULT -> Event.Result.DEFAULT;
            });
        }
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.UseItemFinishHandler>> useItemFinishHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.KnockbackHandler>> knockbackHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.LivingHandler>> livingHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.EquipmentChangeHandler>> equipmentHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.DeathHandler>> deathHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.HurtHandler>> hurtHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.DamageHandler>> damageHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.HealHandler>> healHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.AttackHandler>> attackHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.PotionApplicableHandler>> potionHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.ExperienceDropHandler>> experienceDropHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLivingEvents.DropsHandler>> dropsHandlers() {
        return buckets();
    }

    private static <T> EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> buckets() {
        EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> result = new EnumMap<>(KineticEventPriority.class);
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }

    private static <T> KineticEventSubscription add(EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> listeners,
                                            KineticEventPriority priority,
                                            T listener) {
        CopyOnWriteArrayList<T> bucket = listeners.get(priority);
        bucket.add(listener);
        return KineticEventSubscription.once(() -> bucket.remove(listener));
    }
}
