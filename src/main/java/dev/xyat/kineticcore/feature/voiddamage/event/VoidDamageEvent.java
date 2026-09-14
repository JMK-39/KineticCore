package dev.xyat.kineticcore.feature.voiddamage.event;

import net.minecraftforge.common.MinecraftForge;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class VoidDamageEvent {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, VoidDamageEvent::onVoidHurt);
    }


    public static void onVoidHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide() || entity instanceof Player) {
            return;
        }

        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            if (isWhiteListed(entity)) {
                return;
            }

            float maxHealth = entity.getMaxHealth();
            float percentage = KTServerConfigApi.getInt("kineticcore:general_mechanics", "void_damage_percentage", 10) / 100.0f;
            float calcDamage = maxHealth * percentage;
            float finalDamage = Math.max(calcDamage, 4.0f);

            event.setAmount(finalDamage);
        }
    }

    private static boolean isWhiteListed(LivingEntity entity) {
        if (KTServerConfigApi.getStringList("kineticcore:general_mechanics", "void_damage_whitelist", List.of()) == null || KTServerConfigApi.getStringList("kineticcore:general_mechanics", "void_damage_whitelist", List.of()).isEmpty()) {
            return false;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
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
                    ResourceLocation tagId = new ResourceLocation(rule.substring(1));
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
