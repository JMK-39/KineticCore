package dev.xyat.kineticcore.feature.voiddamage.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = KineticCore.MODID)
public class VoidDamageEvent {

    @SubscribeEvent(priority = EventPriority.HIGH)
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
            float percentage = GeneralMechanicsConfig.voidDamagePercentage / 100.0f;
            float calcDamage = maxHealth * percentage;
            float finalDamage = Math.max(calcDamage, 4.0f);

            event.setAmount(finalDamage);
        }
    }

    private static boolean isWhiteListed(LivingEntity entity) {
        if (GeneralMechanicsConfig.voidDamageWhiteList == null || GeneralMechanicsConfig.voidDamageWhiteList.isEmpty()) {
            return false;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) {
            return false;
        }

        String idString = entityId.toString();
        String namespace = "@" + entityId.getNamespace();

        for (String rule : GeneralMechanicsConfig.voidDamageWhiteList) {
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