package dev.xyat.kineticcore.feature.spawnegg.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.itemcreator.AttributeSpec;
import dev.xyat.kineticcore.api.itemcreator.EffectSpec;
import dev.xyat.kineticcore.api.itemcreator.ItemCreatorStackData;
import dev.xyat.kineticcore.api.registry.KineticEntityAttributes;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.spawnegg.KineticSpawnEggs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SpawnEggTemplateHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    private SpawnEggTemplateHandler() { }
    public static void register() {
        REGISTRATION.run(() -> KineticSpawnEggs.onSpawned(KineticEventPriority.LOWEST, SpawnEggTemplateHandler::apply));
    }
    private static void apply(KineticSpawnEggs.SpawnedContext context) {
        if (!(context.entity() instanceof LivingEntity living)) return;
        var template = ItemCreatorStackData.readTemplate(context.stack()).orElse(null);
        if (template == null || !template.entityId().equals(KineticRegistries.entityTypes().id(living.getType()).toString())) return;
        if (!template.name().isEmpty()) living.setCustomName(net.minecraft.network.chat.Component.literal(template.name()));
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
                    /*effect.applyInstantenousEffect((net.minecraft.server.level.ServerLevel) living.level(), null, context.player(), living, spec.amplifier(), 1D);
                    *///?} else {
                    effect.applyInstantenousEffect(null, context.player(), living, spec.amplifier(), 1D);
                    //?}
                } else living.addEffect(new MobEffectInstance(holder, spec.durationTicks(), spec.amplifier(), false, spec.particles(), spec.icon()));
            } catch (RuntimeException exception) { KineticRuntime.logger().warn("Cannot apply spawn effect {}", spec.id(), exception); }
        }
        if (living.getHealth() > living.getMaxHealth()) living.setHealth(living.getMaxHealth());
    }
}
