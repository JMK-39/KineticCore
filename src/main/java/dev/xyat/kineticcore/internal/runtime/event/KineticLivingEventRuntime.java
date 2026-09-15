package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.entity.event.KineticLivingEvents;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.LivingHandler>> TICK = livingHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.UseItemFinishHandler>> USE_ITEM_FINISH = useItemFinishHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.KnockbackHandler>> KNOCKBACK = knockbackHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.EquipmentChangeHandler>> EQUIPMENT_CHANGE = equipmentHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.DeathHandler>> DEATH = deathHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.DeathHandler>> DEATH_RECEIVE_CANCELLED = deathHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.HurtHandler>> HURT = hurtHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.DamageHandler>> DAMAGE = damageHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.HealHandler>> HEAL = healHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.AttackHandler>> ATTACK = attackHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.PotionApplicableHandler>> POTION_APPLICABLE = potionHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.ExperienceDropHandler>> EXPERIENCE_DROP = experienceDropHandlers();
    private static final EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.DropsHandler>> DROPS = dropsHandlers();

    private static boolean initialized;

    private KineticLivingEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        for (KineticLivingEvents.Priority priority : KineticLivingEvents.Priority.values()) {
            EventPriority forgePriority = toForge(priority);
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingEvent.LivingTickEvent event) -> onTick(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingEntityUseItemEvent.Finish event) -> onUseItemFinish(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingKnockBackEvent event) -> onKnockback(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingEquipmentChangeEvent event) -> onEquipmentChange(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingDeathEvent event) -> onDeath(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, true, (LivingDeathEvent event) -> onDeathReceiveCancelled(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingHurtEvent event) -> onHurt(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingDamageEvent event) -> onDamage(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingHealEvent event) -> onHeal(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingAttackEvent event) -> onAttack(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (MobEffectEvent.Applicable event) -> onPotionApplicable(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingExperienceDropEvent event) -> onExperienceDrop(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LivingDropsEvent event) -> onDrops(priority, event));
        }
    }

    public static HookRegistration registerTick(KineticLivingEvents.Priority priority, KineticLivingEvents.LivingHandler handler) {
        initialize();
        return add(TICK, priority, handler);
    }

    public static HookRegistration registerUseItemFinish(KineticLivingEvents.Priority priority, KineticLivingEvents.UseItemFinishHandler handler) {
        initialize();
        return add(USE_ITEM_FINISH, priority, handler);
    }

    public static HookRegistration registerKnockback(KineticLivingEvents.Priority priority, KineticLivingEvents.KnockbackHandler handler) {
        initialize();
        return add(KNOCKBACK, priority, handler);
    }

    public static HookRegistration registerEquipmentChange(KineticLivingEvents.Priority priority, KineticLivingEvents.EquipmentChangeHandler handler) {
        initialize();
        return add(EQUIPMENT_CHANGE, priority, handler);
    }

    public static HookRegistration registerDeath(
            KineticLivingEvents.Priority priority,
            boolean receiveCancelled,
            KineticLivingEvents.DeathHandler handler
    ) {
        initialize();
        return add(receiveCancelled ? DEATH_RECEIVE_CANCELLED : DEATH, priority, handler);
    }

    public static HookRegistration registerHurt(KineticLivingEvents.Priority priority, KineticLivingEvents.HurtHandler handler) {
        initialize();
        return add(HURT, priority, handler);
    }

    public static HookRegistration registerDamage(KineticLivingEvents.Priority priority, KineticLivingEvents.DamageHandler handler) {
        initialize();
        return add(DAMAGE, priority, handler);
    }

    public static HookRegistration registerHeal(KineticLivingEvents.Priority priority, KineticLivingEvents.HealHandler handler) {
        initialize();
        return add(HEAL, priority, handler);
    }

    public static HookRegistration registerAttack(KineticLivingEvents.Priority priority, KineticLivingEvents.AttackHandler handler) {
        initialize();
        return add(ATTACK, priority, handler);
    }

    public static HookRegistration registerPotionApplicable(KineticLivingEvents.Priority priority, KineticLivingEvents.PotionApplicableHandler handler) {
        initialize();
        return add(POTION_APPLICABLE, priority, handler);
    }

    public static HookRegistration registerExperienceDrop(KineticLivingEvents.Priority priority, KineticLivingEvents.ExperienceDropHandler handler) {
        initialize();
        return add(EXPERIENCE_DROP, priority, handler);
    }

    public static HookRegistration registerDrops(KineticLivingEvents.Priority priority, KineticLivingEvents.DropsHandler handler) {
        initialize();
        return add(DROPS, priority, handler);
    }

    private static void onTick(KineticLivingEvents.Priority priority, LivingEvent.LivingTickEvent event) {
        for (KineticLivingEvents.LivingHandler handler : TICK.get(priority)) {
            handler.handle(event.getEntity());
        }
    }

    private static void onUseItemFinish(KineticLivingEvents.Priority priority, LivingEntityUseItemEvent.Finish event) {
        UseItemFinishContextImpl context = new UseItemFinishContextImpl(event);
        for (KineticLivingEvents.UseItemFinishHandler handler : USE_ITEM_FINISH.get(priority)) {
            handler.handle(context);
        }
    }

    private static void onKnockback(KineticLivingEvents.Priority priority, LivingKnockBackEvent event) {
        KnockbackContextImpl context = new KnockbackContextImpl(event);
        for (KineticLivingEvents.KnockbackHandler handler : KNOCKBACK.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onEquipmentChange(KineticLivingEvents.Priority priority, LivingEquipmentChangeEvent event) {
        for (KineticLivingEvents.EquipmentChangeHandler handler : EQUIPMENT_CHANGE.get(priority)) {
            handler.handle(event.getEntity(), event.getSlot(), event.getFrom(), event.getTo());
        }
    }

    private static void onDeath(KineticLivingEvents.Priority priority, LivingDeathEvent event) {
        DeathContextImpl context = new DeathContextImpl(event);
        for (KineticLivingEvents.DeathHandler handler : DEATH.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onDeathReceiveCancelled(KineticLivingEvents.Priority priority, LivingDeathEvent event) {
        DeathContextImpl context = new DeathContextImpl(event);
        for (KineticLivingEvents.DeathHandler handler : DEATH_RECEIVE_CANCELLED.get(priority)) {
            handler.handle(context);
        }
    }

    private static void onHurt(KineticLivingEvents.Priority priority, LivingHurtEvent event) {
        HurtContextImpl context = new HurtContextImpl(event);
        for (KineticLivingEvents.HurtHandler handler : HURT.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onDamage(KineticLivingEvents.Priority priority, LivingDamageEvent event) {
        DamageContextImpl context = new DamageContextImpl(event);
        for (KineticLivingEvents.DamageHandler handler : DAMAGE.get(priority)) {
            handler.handle(context);
        }
    }

    private static void onHeal(KineticLivingEvents.Priority priority, LivingHealEvent event) {
        HealContextImpl context = new HealContextImpl(event);
        for (KineticLivingEvents.HealHandler handler : HEAL.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onAttack(KineticLivingEvents.Priority priority, LivingAttackEvent event) {
        AttackContextImpl context = new AttackContextImpl(event);
        for (KineticLivingEvents.AttackHandler handler : ATTACK.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onPotionApplicable(KineticLivingEvents.Priority priority, MobEffectEvent.Applicable event) {
        PotionApplicableContextImpl context = new PotionApplicableContextImpl(event);
        for (KineticLivingEvents.PotionApplicableHandler handler : POTION_APPLICABLE.get(priority)) {
            handler.handle(context);
        }
    }

    private static void onExperienceDrop(KineticLivingEvents.Priority priority, LivingExperienceDropEvent event) {
        ExperienceDropContextImpl context = new ExperienceDropContextImpl(event);
        for (KineticLivingEvents.ExperienceDropHandler handler : EXPERIENCE_DROP.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onDrops(KineticLivingEvents.Priority priority, LivingDropsEvent event) {
        DropsContextImpl context = new DropsContextImpl(event);
        for (KineticLivingEvents.DropsHandler handler : DROPS.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static EventPriority toForge(KineticLivingEvents.Priority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
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

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.UseItemFinishHandler>> useItemFinishHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.KnockbackHandler>> knockbackHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.LivingHandler>> livingHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.EquipmentChangeHandler>> equipmentHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.DeathHandler>> deathHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.HurtHandler>> hurtHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.DamageHandler>> damageHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.HealHandler>> healHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.AttackHandler>> attackHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.PotionApplicableHandler>> potionHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.ExperienceDropHandler>> experienceDropHandlers() {
        return buckets();
    }

    private static EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<KineticLivingEvents.DropsHandler>> dropsHandlers() {
        return buckets();
    }

    private static <T> EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<T>> buckets() {
        EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<T>> result = new EnumMap<>(KineticLivingEvents.Priority.class);
        for (KineticLivingEvents.Priority priority : KineticLivingEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }

    private static <T> HookRegistration add(EnumMap<KineticLivingEvents.Priority, CopyOnWriteArrayList<T>> listeners,
                                            KineticLivingEvents.Priority priority,
                                            T listener) {
        CopyOnWriteArrayList<T> bucket = listeners.get(priority);
        bucket.add(listener);
        return () -> bucket.remove(listener);
    }
}
