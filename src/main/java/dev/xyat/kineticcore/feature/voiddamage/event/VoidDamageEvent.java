package dev.xyat.kineticcore.feature.voiddamage.event;


import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.entity.event.KineticLivingEvents;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class VoidDamageEvent {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(() -> KineticLivingEvents.onHurt(KineticEventPriority.HIGH, VoidDamageEvent::onVoidHurt));
    }


    public static void onVoidHurt(KineticLivingEvents.HurtContext context) {
        LivingEntity entity = context.entity();

        if (entity.level().isClientSide() || entity instanceof Player) {
            return;
        }

        if (context.source().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            if (isWhiteListed(entity)) {
                return;
            }

            float maxHealth = entity.getMaxHealth();
            float percentage = KTServerConfigApi.getInt("kineticcore:general_mechanics", "void_damage_percentage", 10) / 100.0f;
            float calcDamage = maxHealth * percentage;
            float finalDamage = Math.max(calcDamage, 4.0f);

            context.amount(finalDamage);
        }
    }

    private static boolean isWhiteListed(LivingEntity entity) {
        if (KTServerConfigApi.getStringList("kineticcore:general_mechanics", "void_damage_whitelist", List.of()) == null || KTServerConfigApi.getStringList("kineticcore:general_mechanics", "void_damage_whitelist", List.of()).isEmpty()) {
            return false;
        }

        ResourceLocation entityId = KineticRegistries.entityTypes().id(entity.getType());
        if (entityId == null) {
            return false;
        }

        String idString = entityId.toString();
        String namespace = "@" + entityId.getNamespace();

        for (String rule : KTServerConfigApi.getStringList("kineticcore:general_mechanics", "void_damage_whitelist", List.of())) {
            if (rule == null || rule.isEmpty()) {
                continue;
            }

            if (rule.startsWith("@")) {
                if (rule.equals(namespace)) {
                    return true;
                }
            } else if (rule.startsWith("#")) {
                try {
                    ResourceLocation tagId = KineticResourceIds.parse(rule.substring(1));
                    TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagId);
                    if (entity.getType().is(tagKey)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            } else {
                if (rule.equals(idString)) {
                    return true;
                }
            }
        }
        return false;
    }
}
