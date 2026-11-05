package dev.xyat.kineticcore.feature.spawnegg.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.itemcreator.AttributeSpec;
import dev.xyat.kineticcore.api.itemcreator.EffectSpec;
import dev.xyat.kineticcore.api.itemcreator.ItemCreatorStackData;
import dev.xyat.kineticcore.api.itemcreator.SpawnEggTemplate;
import dev.xyat.kineticcore.api.registry.KineticEntityAttributes;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.spawnegg.KineticSpawnEggs;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SpawnEggTemplateHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    /** Mobs that already got their egg's template: a thrown egg spawns through EntityType.spawn and then publishes. */
    private static final Set<Entity> APPLIED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    /** The name the item creator gives an egg whose record has no name. */
    private static final String UNNAMED_EGG = "gui.kineticcore.item_creator.special_egg";
    private SpawnEggTemplateHandler() { }
    public static void register() {
        REGISTRATION.run(() -> KineticSpawnEggs.onSpawned(KineticEventPriority.LOWEST,
                context -> applySpawned(context.entity(), context.stack(), context.player())));
    }

    /**
     * Applies the egg's template to the mob it spawned, once. Every egg spawn through EntityType.spawn calls this
     * (placing on a block or fluid, dispensers, thrown eggs), and so do integrations that publish
     * {@link KineticSpawnEggs#notifySpawned}.
     */
    public static void applySpawned(Entity entity, ItemStack stack, Player player) {
        if (!(entity instanceof LivingEntity living) || entity.isRemoved()) return;
        try {
            var template = ItemCreatorStackData.readTemplate(stack).orElse(null);
            if (template == null || !template.entityId().equals(KineticRegistries.entityTypes().id(living.getType()).toString())) return;
            if (!APPLIED.add(living)) return;
            applyTemplate(living, template, player);
            // Vanilla names the mob after the egg. An egg without a name in the editor is labelled "Special Spawn
            // Egg: Pig"; that label is not a mob name. A name given in an anvil stays.
            if (template.name().isEmpty() && living.getCustomName() != null
                    && living.getCustomName().getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals(UNNAMED_EGG)) living.setCustomName(null);
        } catch (RuntimeException exception) {
            KineticRuntime.logger().warn("Cannot apply spawn egg template to {}", entity, exception);
        }
    }

    /** Applies the same edits before native spawner data is serialized, without publishing a spawn callback. */
    public static void applyTemplate(LivingEntity living, SpawnEggTemplate template, Player player) {
        if (!template.name().isEmpty()) living.setCustomName(net.minecraft.network.chat.Component.literal(template.name()));
        applyEquipment(living, template.equipment());
        boolean healthChanged = false;
        for (AttributeSpec spec : template.attributes()) {
            try {
                var attribute = KineticRegistries.attributes().get(KineticResourceIds.tryParse(spec.id()));
                if (attribute == null) continue;
                var instance = KineticEntityAttributes.ensureInstance(living, attribute);
                if (spec.operation() == AttributeSpec.Operation.SET) instance.setBaseValue(spec.amount());
                else {
                    //? if >=1.21 {
                    /*var id = KineticResourceIds.of("kineticcore", "spawn_template/" + spec.id().replace(':', '/'));
                    var operation = spec.operation() == AttributeSpec.Operation.ADDITION
                            ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    instance.removeModifier(id);
                    instance.addPermanentModifier(new AttributeModifier(id, spec.amount(), operation));
                    *///?} else {
                    UUID id = UUID.nameUUIDFromBytes(("kineticcore:spawn_template:" + spec.id()).getBytes(StandardCharsets.UTF_8));
                    var operation = spec.operation() == AttributeSpec.Operation.ADDITION
                            ? AttributeModifier.Operation.ADDITION : AttributeModifier.Operation.MULTIPLY_TOTAL;
                    instance.removeModifier(id);
                    instance.addPermanentModifier(new AttributeModifier(id, "kineticcore:spawn_template", spec.amount(), operation));
                    //?}
                }
                if (spec.id().endsWith("max_health")) healthChanged = true;
            } catch (RuntimeException exception) { KineticRuntime.logger().warn("Cannot apply spawn attribute {}", spec.id(), exception); }
        }
        if (healthChanged) living.setHealth(living.getMaxHealth());
        for (EffectSpec spec : template.effects()) {
            try {
                var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(spec.id()));
                if (effect == null) continue;
                //? if >=1.21 {
                /*var holder = KineticRegistries.mobEffects().holder(effect);
                *///?} else {
                var holder = effect;
                //?}
                living.removeEffect(holder);
                if (effect.isInstantenous()) {
                    //? if >=26.1 {
                    /*effect.applyInstantenousEffect((net.minecraft.server.level.ServerLevel) living.level(), null, player, living, spec.amplifier(), 1D);
                    *///?} else {
                    effect.applyInstantenousEffect(null, player, living, spec.amplifier(), 1D);
                    //?}
                } else living.addEffect(new MobEffectInstance(holder, spec.durationTicks(), spec.amplifier(), false, spec.particles(), spec.icon()));
            } catch (RuntimeException exception) { KineticRuntime.logger().warn("Cannot apply spawn effect {}", spec.id(), exception); }
        }
        if (living.getHealth() > living.getMaxHealth()) living.setHealth(living.getMaxHealth());
    }
    /** Equipment uses native stack data and is applied after spawn initialization, including native spawners. */
    public static void applyEquipment(LivingEntity living, java.util.List<dev.xyat.kineticcore.api.itemcreator.EquipmentSpec> equipment) {
        var stacks = new java.util.EnumMap<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>(net.minecraft.world.entity.EquipmentSlot.class);
        for (var spec : equipment) {
            var stack = dev.xyat.kineticcore.api.inventory.KineticItemText.parse(spec.item());
            if (stack.isEmpty()) throw new IllegalArgumentException("Empty equipment item");
            stacks.put(net.minecraft.world.entity.EquipmentSlot.valueOf(spec.slot().name()), stack);
        }
        stacks.forEach((slot, stack) -> living.setItemSlot(slot, stack.copy()));
    }
}
